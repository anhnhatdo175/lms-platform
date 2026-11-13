const vnd = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})
const dateTime = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'short',
  timeStyle: 'short',
  timeZone: 'Asia/Ho_Chi_Minh',
})

/** Tiền VND là số nguyên đồng (BIGINT ở backend). */
export function formatVnd(amount: number | null | undefined): string {
  return amount === null || amount === undefined ? '—' : vnd.format(amount)
}

export function formatDateTime(iso: string | null | undefined): string {
  return iso ? dateTime.format(new Date(iso)) : '—'
}

export function formatScore(score: number | string | null | undefined): string {
  if (score === null || score === undefined) return '—'
  const value = typeof score === 'string' ? Number(score) : score
  return Number.isFinite(value)
    ? `${value.toLocaleString('vi-VN', { maximumFractionDigits: 2 })}`
    : '—'
}

/** `mm:ss` hoặc `h:mm:ss`. */
export function formatDuration(totalMs: number): string {
  const total = Math.max(0, Math.ceil(totalMs / 1000))
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  const mmss = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return h > 0 ? `${h}:${mmss}` : mmss
}
