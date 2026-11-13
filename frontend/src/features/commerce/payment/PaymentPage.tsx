import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'

import { Alert, Button, Card, CardBody, CardHeader, Icons, Skeleton } from '@/components/ui'
import { apiErrorMessage } from '@/utils/apiErrorMessage'
import { formatDateTime, formatDuration, formatVnd } from '@/utils/format'

import styles from '../commerce.module.css'
import { isAwaitingTransfer, useCancelOrder, useOrder, usePaymentStatus } from '../commerce.hooks'
import { OrderStatusBadge, PaymentStatusBadge } from '../StatusBadges'

/**
 * Trang QR chuyển khoản. Trang chỉ hỏi trạng thái; quyền học được cấp bởi webhook SePay ở server, nên đóng tab
 * hay mất mạng giữa chừng cũng không ảnh hưởng tới việc ghi nhận thanh toán.
 */
export function PaymentPage() {
  const orderId = Number(useParams().orderId)
  const order = useOrder(orderId)
  const status = usePaymentStatus(orderId)
  const cancel = useCancelOrder(orderId)
  const remaining = useRemaining(order.data?.expiresAt)
  const [copied, setCopied] = useState(false)

  if (order.isPending || status.isPending) {
    return (
      <main className={styles.page}>
        <Skeleton height={40} width="50%" />
        <Skeleton height={280} />
      </main>
    )
  }
  if (order.isError || status.isError) {
    return (
      <main className={styles.page}>
        <Alert variant="destructive" title="Không tải được đơn hàng">
          {apiErrorMessage(order.error ?? status.error)}
        </Alert>
      </main>
    )
  }

  const state = status.data
  const awaiting = isAwaitingTransfer(state)

  const handleCopy = async () => {
    await navigator.clipboard.writeText(state.paymentReference)
    setCopied(true)
  }

  return (
    <main className={styles.page}>
      <h1 className={styles.title}>Thanh toán đơn {state.orderNumber}</h1>

      <Outcome state={state} />

      {awaiting && (
        <Card>
          <CardHeader
            title="Quét mã QR bằng app ngân hàng"
            subtitle={
              remaining !== null ? `Đơn hết hạn sau ${formatDuration(remaining)}` : undefined
            }
            action={<PaymentStatusBadge status={state.paymentStatus} />}
          />
          <CardBody>
            <div className={styles.qrLayout}>
              {state.checkoutUrl ? (
                <img
                  className={styles.qr}
                  src={state.checkoutUrl}
                  alt={`Mã QR chuyển ${formatVnd(state.amount)}`}
                />
              ) : (
                <Skeleton height={280} />
              )}
              <div className={styles.summary}>
                <dl className={styles.facts}>
                  <dt>Số tiền</dt>
                  <dd>{formatVnd(state.amount)}</dd>
                  <dt>Nội dung</dt>
                  <dd className={styles.reference}>{state.paymentReference}</dd>
                  <dt>Hết hạn lúc</dt>
                  <dd>{formatDateTime(order.data.expiresAt)}</dd>
                </dl>
                <Alert variant="info">
                  Chuyển đúng số tiền và giữ nguyên nội dung {state.paymentReference}. Hệ thống tự
                  xác nhận trong vài giây sau khi ngân hàng báo có.
                </Alert>
                <div className={styles.actions}>
                  <Button
                    variant="outline"
                    leftIcon={<Icons.Check size={16} />}
                    onClick={() => void handleCopy()}
                  >
                    {copied ? 'Đã sao chép' : 'Sao chép nội dung'}
                  </Button>
                  <Button
                    variant="ghost"
                    isLoading={cancel.isPending}
                    onClick={() => cancel.mutate(undefined)}
                  >
                    Hủy đơn
                  </Button>
                </div>
                {cancel.isError && (
                  <Alert variant="destructive">{apiErrorMessage(cancel.error)}</Alert>
                )}
                <p className={styles.muted} aria-live="polite">
                  <Icons.Clock size={14} /> Đang chờ xác nhận chuyển khoản…
                </p>
              </div>
            </div>
          </CardBody>
        </Card>
      )}

      <p>
        <Link to={`/orders/${orderId}`}>Xem chi tiết đơn hàng</Link>
      </p>
    </main>
  )
}

function Outcome({ state }: { state: NonNullable<ReturnType<typeof usePaymentStatus>['data']> }) {
  if (state.orderStatus === 'PAID') {
    return (
      <Alert
        variant="success"
        title="Thanh toán thành công"
        action={<Link to="/my-courses">Vào học ngay</Link>}
      >
        Đã nhận {formatVnd(state.receivedAmount ?? state.amount)} lúc {formatDateTime(state.paidAt)}
        . Khóa học đã có trong mục Khóa học của tôi.
      </Alert>
    )
  }
  if (state.paymentStatus === 'REVIEW_REQUIRED') {
    return (
      <Alert variant="warning" title="Giao dịch cần đối soát">
        Hệ thống đã nhận {formatVnd(state.receivedAmount)} nhưng chưa khớp hoàn toàn với đơn hàng
        {state.reviewReasonCode === 'AMOUNT_UNDERPAID' && ' (chuyển thiếu)'}
        {state.reviewReasonCode === 'AMOUNT_OVERPAID' && ' (chuyển thừa)'}
        {state.reviewReasonCode === 'ORDER_NOT_PAYABLE' && ' (đơn đã hết hạn hoặc đã hủy)'}. Quản
        trị viên sẽ kiểm tra và phản hồi.
      </Alert>
    )
  }
  if (state.orderStatus === 'EXPIRED' || state.orderStatus === 'CANCELLED') {
    return (
      <Alert
        variant="info"
        title={state.orderStatus === 'EXPIRED' ? 'Đơn hàng đã hết hạn' : 'Đơn hàng đã hủy'}
        action={<Link to="/cart">Về giỏ hàng</Link>}
      >
        <OrderStatusBadge status={state.orderStatus} /> Nếu bạn đã lỡ chuyển khoản, giao dịch sẽ
        được đưa vào đối soát.
      </Alert>
    )
  }
  if (state.paymentStatus === 'FAILED') {
    return (
      <Alert variant="destructive" title="Thanh toán không thành công">
        Vui lòng liên hệ hỗ trợ.
      </Alert>
    )
  }
  return null
}

/** Thời gian còn lại theo đồng hồ máy; chỉ để hiển thị, server mới quyết định hết hạn. */
function useRemaining(expiresAt: string | undefined): number | null {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [])
  return expiresAt ? Math.max(0, new Date(expiresAt).getTime() - now) : null
}
