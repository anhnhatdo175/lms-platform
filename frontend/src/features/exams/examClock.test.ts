import { describe, expect, it } from 'vitest'

import { clockOffsetMs, remainingMs, urgency } from './examClock'

describe('exam clock', () => {
  it('counts down on server time even when the device clock is wrong', () => {
    // Máy chậm 2 phút: nhận response lúc 07:58 theo máy, server nói 08:00
    const received = Date.parse('2026-11-10T07:58:00Z')
    const offset = clockOffsetMs('2026-11-10T08:00:00Z', received)
    expect(offset).toBe(120_000)
    const expiresAt = '2026-11-10T08:30:00Z'
    expect(remainingMs(expiresAt, offset, received)).toBe(30 * 60_000)
    expect(remainingMs(expiresAt, offset, received + 10 * 60_000)).toBe(20 * 60_000)
  })

  it('never goes below zero', () => {
    expect(remainingMs('2026-11-10T08:00:00Z', 0, Date.parse('2026-11-10T09:00:00Z'))).toBe(0)
  })

  it('urgency thresholds', () => {
    expect(urgency(10 * 60_000)).toBe('normal')
    expect(urgency(5 * 60_000)).toBe('warning')
    expect(urgency(60_000)).toBe('critical')
    expect(urgency(0)).toBe('critical')
  })
})
