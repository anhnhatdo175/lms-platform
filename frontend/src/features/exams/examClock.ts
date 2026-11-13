/**
 * Đồng hồ bài thi theo giờ server (Spec 14.5): máy Student có thể lệch giờ, nên chỉ dùng máy để đếm khoảng trôi
 * qua kể từ lúc nhận `serverTime`. Hết giờ thật do server quyết định; đồng hồ này chỉ để hiển thị và tự nộp.
 */
export function clockOffsetMs(serverTime: string, receivedAt: number): number {
  return new Date(serverTime).getTime() - receivedAt
}

export function remainingMs(expiresAt: string, offsetMs: number, now: number): number {
  return Math.max(0, new Date(expiresAt).getTime() - (now + offsetMs))
}

/** Mức cảnh báo cho đồng hồ: dưới 5 phút và dưới 1 phút. */
export function urgency(remaining: number): 'normal' | 'warning' | 'critical' {
  if (remaining <= 60_000) return 'critical'
  if (remaining <= 5 * 60_000) return 'warning'
  return 'normal'
}
