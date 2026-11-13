import { useNavigate, useParams } from 'react-router'

import {
  Alert,
  Button,
  Card,
  CardBody,
  CardHeader,
  Skeleton,
  Table,
  type Column,
} from '@/components/ui'
import { apiErrorMessage } from '@/utils/apiErrorMessage'
import { formatDateTime, formatVnd } from '@/utils/format'

import styles from '../commerce.module.css'
import { useCancelOrder, useOrder } from '../commerce.hooks'
import type { OrderItem } from '../commerce.types'
import { OrderStatusBadge, PaymentStatusBadge } from '../StatusBadges'

const COLUMNS: Column<OrderItem>[] = [
  { key: 'courseTitle', header: 'Khóa học' },
  { key: 'unitPrice', header: 'Giá', align: 'right', render: (i) => formatVnd(i.unitPrice) },
  {
    key: 'discountAmount',
    header: 'Giảm',
    align: 'right',
    render: (i) => formatVnd(i.discountAmount),
  },
  {
    key: 'finalPrice',
    header: 'Thành tiền',
    align: 'right',
    render: (i) => formatVnd(i.finalPrice),
  },
]

export function OrderDetailPage() {
  const navigate = useNavigate()
  const orderId = Number(useParams().orderId)
  const order = useOrder(orderId)
  const cancel = useCancelOrder(orderId)

  if (order.isPending) {
    return (
      <main className={styles.page}>
        <Skeleton height={200} />
      </main>
    )
  }
  if (order.isError) {
    return (
      <main className={styles.page}>
        <Alert variant="destructive">{apiErrorMessage(order.error)}</Alert>
      </main>
    )
  }

  const o = order.data
  return (
    <main className={styles.page}>
      <h1 className={styles.title}>Đơn {o.orderNumber}</h1>
      <Card>
        <CardHeader
          title={<OrderStatusBadge status={o.status} />}
          subtitle={`Đặt lúc ${formatDateTime(o.createdAt)}`}
          action={o.payment ? <PaymentStatusBadge status={o.payment.paymentStatus} /> : undefined}
        />
        <CardBody>
          <div className={styles.summary}>
            <Table columns={COLUMNS} data={o.items} />
            <div className={styles.row}>
              <span>Tạm tính</span>
              <span>{formatVnd(o.subtotal)}</span>
            </div>
            {o.discountTotal > 0 && (
              <div className={`${styles.row} ${styles.discount}`}>
                <span>Giảm giá</span>
                <span>−{formatVnd(o.discountTotal)}</span>
              </div>
            )}
            <div className={styles.totalRow}>
              <span>Tổng cộng</span>
              <span>{formatVnd(o.total)}</span>
            </div>
            {/* Đang đối soát thì không còn chuyển khoản hay hủy được: Admin xử lý tiếp */}
            {o.status === 'PENDING_PAYMENT' && o.payment?.paymentStatus !== 'REVIEW_REQUIRED' && (
              <div className={styles.actions}>
                <Button variant="accent" onClick={() => void navigate(`/orders/${o.id}/payment`)}>
                  Thanh toán
                </Button>
                <Button
                  variant="ghost"
                  isLoading={cancel.isPending}
                  onClick={() => cancel.mutate(undefined)}
                >
                  Hủy đơn
                </Button>
              </div>
            )}
            {cancel.isError && <Alert variant="destructive">{apiErrorMessage(cancel.error)}</Alert>}
          </div>
        </CardBody>
      </Card>
    </main>
  )
}
