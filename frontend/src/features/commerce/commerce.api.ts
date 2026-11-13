import { api, type DataResponse, type PageResponse } from '@/lib/api-client'

import type {
  Cart,
  CheckoutResult,
  CouponPreview,
  OrderDetail,
  OrderSummary,
  PaymentState,
} from './commerce.types'

export const cartApi = {
  get: () => api.get<DataResponse<Cart>>('/cart').then((r) => r.data),
  addItem: (courseId: number) =>
    api.post<DataResponse<Cart>>('/cart/items', { courseId }).then((r) => r.data),
  removeItem: (courseId: number) =>
    api.delete<DataResponse<Cart>>(`/cart/items/${courseId}`).then((r) => r.data),
  previewCoupon: (couponCode: string) =>
    api
      .post<DataResponse<CouponPreview>>('/cart/coupon-preview', { couponCode })
      .then((r) => r.data),
}

export const ordersApi = {
  /**
   * Cùng một `Idempotency-Key` cho mọi lần gửi lại của một lần bấm "Đặt hàng": server trả lại đúng Order đã tạo
   * thay vì tạo Order thứ hai (Spec 10.4).
   */
  checkout: (idempotencyKey: string, couponCode: string | null) =>
    api
      .post<DataResponse<CheckoutResult>>(
        '/orders/checkout',
        { couponCode: couponCode || null, paymentMethod: 'BANK_QR' },
        { headers: { 'Idempotency-Key': idempotencyKey } },
      )
      .then((r) => r.data),
  list: (page: number, size = 10) =>
    api.get<PageResponse<OrderSummary>>('/orders', { query: { page, size } }),
  get: (orderId: number) =>
    api.get<DataResponse<OrderDetail>>(`/orders/${orderId}`).then((r) => r.data),
  cancel: (orderId: number, reason?: string) =>
    api
      .post<DataResponse<OrderDetail>>(`/orders/${orderId}/cancel`, { reason })
      .then((r) => r.data),
  paymentStatus: (orderId: number) =>
    api.get<DataResponse<PaymentState>>(`/orders/${orderId}/status`).then((r) => r.data),
}
