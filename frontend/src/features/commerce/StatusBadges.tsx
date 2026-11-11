import { Badge, type BadgeVariant } from '@/components/ui'

import type { OrderStatus, PaymentStatus } from './commerce.types'

const ORDER: Record<OrderStatus, { label: string; variant: BadgeVariant }> = {
  PENDING_PAYMENT: { label: 'Chờ thanh toán', variant: 'warning' },
  PAID: { label: 'Đã thanh toán', variant: 'success' },
  CANCELLED: { label: 'Đã hủy', variant: 'default' },
  EXPIRED: { label: 'Hết hạn', variant: 'default' },
}

const PAYMENT: Record<PaymentStatus, { label: string; variant: BadgeVariant }> = {
  PENDING: { label: 'Chờ chuyển khoản', variant: 'warning' },
  PAID: { label: 'Đã nhận tiền', variant: 'success' },
  REVIEW_REQUIRED: { label: 'Đang đối soát', variant: 'info' },
  FAILED: { label: 'Thất bại', variant: 'destructive' },
  CANCELLED: { label: 'Đã hủy', variant: 'default' },
}

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return (
    <Badge variant={ORDER[status].variant} hasDot>
      {ORDER[status].label}
    </Badge>
  )
}

export function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  return (
    <Badge variant={PAYMENT[status].variant} hasDot>
      {PAYMENT[status].label}
    </Badge>
  )
}
