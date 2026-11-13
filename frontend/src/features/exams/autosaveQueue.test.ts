import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { AutosaveQueue, type SaveState } from './autosaveQueue'

class Fatal extends Error {}

function setup(
  save: (aq: number, ids: number[], version: number) => Promise<{ attemptVersion: number }>,
) {
  const states: Array<[number, SaveState]> = []
  const fatal = vi.fn()
  const queue = new AutosaveQueue(5, {
    save,
    onStateChange: (aq, s) => states.push([aq, s]),
    isFatal: (e) => e instanceof Fatal,
    debounceMs: 100,
    retryMs: 1000,
  })
  queue.setFatalHandler(fatal)
  return { queue, states, fatal }
}

describe('AutosaveQueue', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  it('debounces per question and sends only the latest choice', async () => {
    const save = vi.fn((_aq: number, _ids: number[], version: number) =>
      Promise.resolve({ attemptVersion: version + 1 }),
    )
    const { queue } = setup(save)
    queue.change(1, [10])
    queue.change(1, [11])
    await vi.advanceTimersByTimeAsync(150)
    expect(save).toHaveBeenCalledTimes(1)
    expect(save).toHaveBeenCalledWith(1, [11], 5)
  })

  it('sends saves one after another, each with the version from the previous ack', async () => {
    const calls: Array<[number, number]> = []
    const save = vi.fn(async (aq: number, _ids: number[], version: number) => {
      calls.push([aq, version])
      await new Promise((r) => setTimeout(r, 50))
      return { attemptVersion: version + 1 }
    })
    const { queue } = setup(save)
    queue.change(1, [10])
    queue.change(2, [20])
    queue.change(3, [30])
    await vi.advanceTimersByTimeAsync(100) // hết debounce: cả ba vào hàng đợi
    expect(calls).toEqual([[1, 5]]) // câu 2 và 3 chờ câu 1 xong
    await vi.advanceTimersByTimeAsync(200)
    expect(queue.hasUnsaved).toBe(false)
    expect(calls).toEqual([
      [1, 5],
      [2, 6],
      [3, 7],
    ])
  })

  it('flush sends pending changes immediately', async () => {
    const save = vi.fn(() => Promise.resolve({ attemptVersion: 6 }))
    const { queue } = setup(save)
    queue.change(1, [10])
    expect(queue.hasUnsaved).toBe(true)
    await queue.flush()
    expect(save).toHaveBeenCalledWith(1, [10], 5)
    expect(queue.hasUnsaved).toBe(false)
  })

  it('keeps a change made while its save is in flight', async () => {
    let release: () => void = () => undefined
    const save = vi.fn(
      (_aq: number, _ids: number[], version: number) =>
        new Promise<{ attemptVersion: number }>((resolve) => {
          release = () => resolve({ attemptVersion: version + 1 })
        }),
    )
    const { queue, states } = setup(save)
    queue.change(1, [10])
    await vi.advanceTimersByTimeAsync(100)
    queue.change(1, [11]) // đổi tiếp trong lúc đang gửi [10]
    release()
    await vi.advanceTimersByTimeAsync(0)
    expect(states.at(-1)).toEqual([1, 'pending'])
    await vi.advanceTimersByTimeAsync(100)
    release()
    await queue.flush()
    expect(save).toHaveBeenLastCalledWith(1, [11], 6)
    expect(states.at(-1)).toEqual([1, 'saved'])
  })

  it('retries after a network error', async () => {
    const save = vi
      .fn<(aq: number, ids: number[], version: number) => Promise<{ attemptVersion: number }>>()
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce({ attemptVersion: 6 })
    const { queue, states } = setup(save)
    queue.change(1, [10])
    await vi.advanceTimersByTimeAsync(100)
    expect(states.at(-1)).toEqual([1, 'error'])
    expect(queue.hasUnsaved).toBe(true)
    await vi.advanceTimersByTimeAsync(1000)
    expect(save).toHaveBeenCalledTimes(2)
    expect(states.at(-1)).toEqual([1, 'saved'])
  })

  it('stops on a fatal error (time is up) and ignores later changes', async () => {
    const save = vi.fn(() => Promise.reject(new Fatal('ATTEMPT_EXPIRED')))
    const { queue, fatal } = setup(save)
    queue.change(1, [10])
    queue.change(2, [20])
    await vi.advanceTimersByTimeAsync(100)
    await queue.flush()
    expect(fatal).toHaveBeenCalledTimes(1)
    expect(save).toHaveBeenCalledTimes(1)
    queue.change(3, [30])
    await vi.advanceTimersByTimeAsync(500)
    expect(save).toHaveBeenCalledTimes(1)
  })
})
