const REASONS: Record<string, string> = {
  COUPON_INVALID: 'Mã không tồn tại hoặc đã tắt.',
  COUPON_EXPIRED: 'Mã đã hết hạn hoặc chưa tới ngày dùng.',
  COUPON_NOT_APPLICABLE: 'Mã không áp dụng cho các khóa trong giỏ hoặc chưa đủ giá trị tối thiểu.',
  COUPON_USAGE_EXCEEDED: 'Mã đã hết lượt dùng.',
}

export function couponReasonMessage(reasonCode: string | null): string {
  return (reasonCode && REASONS[reasonCode]) || 'Không áp dụng được mã này.'
}
