import { useState } from 'react'
import { useNavigate } from 'react-router'

import { Alert, Table, type Column } from '@/components/ui'
import { apiErrorMessage } from '@/utils/apiErrorMessage'
import { formatDateTime, formatVnd } from '@/utils/format'

import styles from '../commerce.module.css'
import { useOrders } from '../commerce.hooks'
import type { OrderSummary } from '../commerce.types'
import { OrderStatusBadge } from '../StatusBadges'

const COLUMNS: Column<OrderSummary>[] = [
  { key: 'orderNumber', header: 'Mã đơn' },
  { key: 'createdAt', header: 'Ngày đặt', render: (o) => formatDateTime(o.createdAt) },
  { key: 'itemCount', header: 'Số khóa', align: 'center' },
  { key: 'total', header: 'Tổng tiền', align: 'right', render: (o) => formatVnd(o.total) },
  { key: 'status', header: 'Trạng thái', render: (o) => <OrderStatusBadge status={o.status} /> },
]

export function OrderHistoryPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const orders = useOrders(page)

  return (
    <main className={styles.page}>
      <h1 className={styles.title}>Đơn hàng của tôi</h1>
      {orders.isError && <Alert variant="destructive">{apiErrorMessage(orders.error)}</Alert>}
      <Table
        columns={COLUMNS}
        data={orders.data?.data ?? []}
        isLoading={orders.isPending}
        emptyMessage="Bạn chưa có đơn hàng nào."
        onRowClick={(order) =>
          void navigate(
            order.status === 'PENDING_PAYMENT'
              ? `/orders/${order.id}/payment`
              : `/orders/${order.id}`,
          )
        }
        pagination={orders.data ? { meta: orders.data.meta, onPageChange: setPage } : undefined}
      />
    </main>
  )
}
