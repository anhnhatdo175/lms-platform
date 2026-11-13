/**
 * Hàng đợi autosave thuần (không phụ thuộc React) để kiểm thử được. Mỗi câu có debounce riêng; các lần gửi được
 * xếp nối tiếp vì mỗi lần lưu đổi version token của Attempt, gửi song song thì lần sau sẽ mang version cũ.
 */
export type SaveState = 'idle' | 'pending' | 'saving' | 'saved' | 'error'

export interface AutosaveDeps {
  save: (
    attemptQuestionId: number,
    selectedOptionIds: number[],
    version: number,
  ) => Promise<{ attemptVersion: number }>
  onStateChange: (attemptQuestionId: number, state: SaveState) => void
  isFatal: (error: unknown) => boolean
  debounceMs?: number
  retryMs?: number
}

export class AutosaveQueue {
  private version: number
  private readonly latest = new Map<number, number[]>()
  private readonly dirty = new Set<number>()
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>()
  private chain: Promise<void> = Promise.resolve()
  private stopped = false
  private readonly deps: AutosaveDeps
  private onFatal: (error: unknown) => void = () => undefined

  constructor(initialVersion: number, deps: AutosaveDeps) {
    this.version = initialVersion
    this.deps = deps
  }

  /** Lỗi không thử lại được (hết giờ, đã nộp, xung đột version): bên gọi quyết định làm gì tiếp. */
  setFatalHandler(handler: (error: unknown) => void): void {
    this.onFatal = handler
  }

  get hasUnsaved(): boolean {
    return this.dirty.size > 0
  }

  change(attemptQuestionId: number, selectedOptionIds: number[]): void {
    if (this.stopped) return
    this.latest.set(attemptQuestionId, selectedOptionIds)
    this.dirty.add(attemptQuestionId)
    this.deps.onStateChange(attemptQuestionId, 'pending')
    const previous = this.timers.get(attemptQuestionId)
    if (previous) clearTimeout(previous)
    this.timers.set(
      attemptQuestionId,
      setTimeout(() => {
        this.timers.delete(attemptQuestionId)
        void this.enqueue(attemptQuestionId)
      }, this.deps.debounceMs ?? 500),
    )
  }

  /** Gửi ngay mọi câu chưa lưu và chờ xong; dùng trước khi nộp bài. */
  async flush(): Promise<void> {
    this.timers.forEach((timer) => clearTimeout(timer))
    this.timers.clear()
    ;[...this.dirty].forEach((id) => void this.enqueue(id))
    await this.chain
  }

  /** Bỏ mọi thay đổi chưa gửi và dừng hẳn (đã hết giờ hoặc rời trang). */
  stop(): void {
    this.stopped = true
    this.timers.forEach((timer) => clearTimeout(timer))
    this.timers.clear()
  }

  /** Nạp lại sau xung đột: dùng version và đáp án mới nhất từ server. */
  reset(version: number): void {
    this.version = version
    this.dirty.clear()
    this.latest.clear()
    this.stopped = false
  }

  private enqueue(attemptQuestionId: number): Promise<void> {
    this.chain = this.chain.then(() => this.send(attemptQuestionId))
    return this.chain
  }

  private async send(attemptQuestionId: number): Promise<void> {
    if (this.stopped || !this.dirty.has(attemptQuestionId)) return
    const selected = this.latest.get(attemptQuestionId) ?? []
    this.dirty.delete(attemptQuestionId)
    this.deps.onStateChange(attemptQuestionId, 'saving')
    try {
      const ack = await this.deps.save(attemptQuestionId, selected, this.version)
      this.version = ack.attemptVersion
      // Student đổi tiếp trong lúc đang gửi thì câu đó vẫn còn "pending"
      if (!this.dirty.has(attemptQuestionId)) this.deps.onStateChange(attemptQuestionId, 'saved')
    } catch (error) {
      if (this.deps.isFatal(error)) {
        this.stop()
        this.onFatal(error)
        return
      }
      // Lỗi mạng: giữ lại và tự gửi lại sau ít giây (flush hoặc lần đổi kế tiếp cũng gửi lại)
      this.dirty.add(attemptQuestionId)
      this.deps.onStateChange(attemptQuestionId, 'error')
      if (!this.timers.has(attemptQuestionId)) {
        this.timers.set(
          attemptQuestionId,
          setTimeout(() => {
            this.timers.delete(attemptQuestionId)
            void this.enqueue(attemptQuestionId)
          }, this.deps.retryMs ?? 3000),
        )
      }
    }
  }
}
