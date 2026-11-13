/**
 * Kiểu dữ liệu theo OpenAPI v1.5 (tag cart, orders). Tiền là số nguyên VND. Dùng `type` thay vì `interface`
 * để truyền được vào `Table<T extends Record<string, unknown>>` dùng chung.
 */
export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED' | 'EXPIRED'
export type PaymentStatus = 'PENDING' | 'PAID' | 'REVIEW_REQUIRED' | 'FAILED' | 'CANCELLED'
export type ReviewReasonCode = 'ORDER_NOT_PAYABLE' | 'AMOUNT_UNDERPAID' | 'AMOUNT_OVERPAID'

export type CartItem = {
  courseId: number
  courseSlug: string
  courseTitle: string
  price: number
  thumbnailUrl: string | null
}

export type Cart = {
  items: CartItem[]
  subtotal: number
  itemCount: number
  updatedAt: string | null
}

export type CouponPreview = {
  valid: boolean
  couponCode: string
  subtotal: number | null
  discountTotal: number | null
  total: number | null
  reasonCode: string | null
}

export type OrderItem = {
  id: number
  courseId: number
  courseTitle: string
  unitPrice: number
  discountAmount: number
  finalPrice: number
}

export type PaymentState = {
  orderId: number
  orderNumber: string
  orderStatus: OrderStatus
  paymentStatus: PaymentStatus
  provider: string
  paymentReference: string
  amount: number
  checkoutUrl: string | null
  paidAt: string | null
  updatedAt: string
  receivedAmount: number | null
  reviewReason: string | null
  reviewReasonCode: ReviewReasonCode | null
}

export type OrderDetail = {
  id: number
  orderNumber: string
  status: OrderStatus
  subtotal: number
  discountTotal: number
  total: number
  items: OrderItem[]
  payment: PaymentState | null
  createdAt: string
  expiresAt: string
}

export type OrderSummary = {
  id: number
  orderNumber: string
  status: OrderStatus
  total: number
  itemCount: number
  createdAt: string
  expiresAt: string
}

export type CheckoutPayment = {
  id: number
  provider: string
  status: PaymentStatus
  paymentReference: string
  amount: number
  instructions: string
  qrCodeUrl: string
  expiresAt: string
}

export type CheckoutResult = {
  order: OrderDetail
  payment: CheckoutPayment
}
