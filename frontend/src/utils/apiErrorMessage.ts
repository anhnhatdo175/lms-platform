import { ApiError } from '@/lib/api-client'

/** Thông báo cho các mã lỗi phần B (thương mại và thi). Mã không có ở đây dùng message của server. */
const MESSAGES: Record<string, string> = {
  NETWORK_ERROR: 'Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.',
  UNAUTHENTICATED: 'Phiên đăng nhập đã hết. Vui lòng đăng nhập lại.',
  FORBIDDEN: 'Bạn không có quyền thực hiện thao tác này.',
  ACCOUNT_BLOCKED: 'Tài khoản đang bị khóa.',
  VALIDATION_ERROR: 'Dữ liệu chưa hợp lệ.',
  CONFLICT: 'Dữ liệu vừa thay đổi ở nơi khác. Tải lại rồi thử lại.',
  STATE_CONFLICT: 'Trạng thái hiện tại không cho phép thao tác này.',
  NOT_FOUND: 'Không tìm thấy dữ liệu.',
  CART_EMPTY: 'Giỏ hàng đang trống.',
  COURSE_ALREADY_ENROLLED: 'Bạn đã có quyền học khóa này.',
  COURSE_NOT_PUBLISHED: 'Khóa học chưa mở bán.',
  COUPON_INVALID: 'Mã giảm giá không tồn tại hoặc đã tắt.',
  COUPON_EXPIRED: 'Mã giảm giá đã hết hạn hoặc chưa tới ngày dùng.',
  COUPON_NOT_APPLICABLE: 'Mã giảm giá không áp dụng cho giỏ hàng này.',
  COUPON_USAGE_EXCEEDED: 'Mã giảm giá đã hết lượt dùng.',
  IDEMPOTENCY_KEY_REUSED: 'Giỏ hàng đã đổi so với lần đặt trước. Vui lòng đặt hàng lại.',
  ORDER_NOT_CANCELLABLE: 'Đơn hàng không còn hủy được.',
  ENROLLMENT_REQUIRED: 'Bạn cần mua khóa học để vào bài thi.',
  ENROLLMENT_EXPIRED: 'Quyền học của bạn đã hết hạn.',
  ENROLLMENT_REVOKED: 'Quyền học của bạn đã bị thu hồi.',
  LESSON_NOT_PUBLISHED: 'Bài học chưa được phát hành.',
  LESSON_LOCKED_UNTIL_RELEASE: 'Bài học chưa tới ngày mở.',
  LESSON_LOCKED_BY_PREREQUISITE: 'Bạn cần hoàn thành bài học trước đó.',
  EXAM_NOT_OPEN: 'Bài thi chưa mở.',
  EXAM_CLOSED: 'Bài thi đã đóng.',
  EXAM_ATTEMPT_LIMIT_REACHED: 'Bạn đã dùng hết số lần làm bài.',
  ATTEMPT_EXPIRED: 'Đã hết giờ. Bài làm được nộp với các câu trả lời đã lưu.',
  ATTEMPT_NOT_IN_PROGRESS: 'Bài làm đã được nộp.',
  QUESTION_INVALID: 'Lựa chọn không hợp lệ cho câu hỏi này.',
  PAYMENT_NOT_REVIEWABLE: 'Giao dịch này không còn ở trạng thái cần đối soát.',
  RECONCILIATION_INVALID: 'Không thể đối soát giao dịch với thanh toán đã chọn.',
}

export function apiErrorMessage(error: unknown): string {
  if (error instanceof ApiError) return MESSAGES[error.code] ?? error.message
  return 'Đã có lỗi xảy ra. Vui lòng thử lại.'
}

export function hasErrorCode(error: unknown, code: string): boolean {
  return error instanceof ApiError && error.code === code
}
