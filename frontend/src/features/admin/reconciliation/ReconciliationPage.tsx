import { useState } from 'react'

import {
  Alert,
  Badge,
  Button,
  Input,
  Modal,
  Table,
  Tabs,
  type BadgeVariant,
  type Column,
} from '@/components/ui'
import { apiErrorMessage } from '@/utils/apiErrorMessage'
import { formatDateTime, formatVnd } from '@/utils/format'

import {
  allowedActions,
  useReconciliationAction,
  useReconciliationList,
  type ReconciliationAction,
  type ReconciliationItem,
  type ReconciliationView,
  type WebhookStatus,
} from './reconciliation.api'
import styles from './ReconciliationPage.module.css'

const STATUS: Record<WebhookStatus, { label: string; variant: BadgeVariant }> = {
  RECEIVED: { label: 'Mới nhận', variant: 'default' },
  VALIDATED: { label: 'Đã kiểm', variant: 'default' },
  MATCHED: { label: 'Cần duyệt', variant: 'warning' },
  PROCESSED: { label: 'Đã xử lý', variant: 'success' },
  DUPLICATE: { label: 'Trùng', variant: 'default' },
  UNMATCHED: { label: 'Không khớp', variant: 'destructive' },
  FAILED: { label: 'Lỗi', variant: 'destructive' },
}

const REASON: Record<string, string> = {
  AMOUNT_UNDERPAID: 'Chuyển thiếu',
  AMOUNT_OVERPAID: 'Chuyển thừa',
  ORDER_NOT_PAYABLE: 'Đơn đã hết hạn hoặc đã hủy',
  NO_REFERENCE: 'Không có mã thanh toán',
  UNKNOWN_REFERENCE: 'Mã thanh toán không tồn tại',
  ALREADY_PAID: 'Đơn đã thanh toán trước đó',
  PAYMENT_NOT_PENDING: 'Thanh toán không còn chờ',
}

const reasonOf = (item: ReconciliationItem) => {
  const code = item.reviewReasonCode ?? item.unmatchedReason
  return code ? (REASON[code] ?? code) : '—'
}

const COLUMNS: Column<ReconciliationItem>[] = [
  { key: 'receivedAt', header: 'Nhận lúc', render: (i) => formatDateTime(i.receivedAt) },
  { key: 'providerTransactionId', header: 'Mã GD ngân hàng' },
  { key: 'amount', header: 'Số tiền nhận', align: 'right', render: (i) => formatVnd(i.amount) },
  {
    key: 'expectedAmount',
    header: 'Cần thu',
    align: 'right',
    render: (i) => formatVnd(i.expectedAmount),
  },
  { key: 'paymentReference', header: 'Mã thanh toán', render: (i) => i.paymentReference ?? '—' },
  { key: 'reason', header: 'Lý do', render: reasonOf },
  {
    key: 'status',
    header: 'Trạng thái',
    render: (i) => <Badge variant={STATUS[i.status].variant}>{STATUS[i.status].label}</Badge>,
  },
]

/** Đối soát thanh toán (Spec 11.7): Admin duyệt, từ chối hoặc ghép giao dịch không khớp. Mọi thao tác cần lý do. */
export function ReconciliationPage() {
  const [view, setView] = useState<ReconciliationView>('queue')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<ReconciliationItem | null>(null)
  const list = useReconciliationList(view, page)

  return (
    <main className={styles.page}>
      <h1 className={styles.title}>Đối soát thanh toán</h1>
      <Tabs
        items={[
          { id: 'queue', label: 'Cần xử lý' },
          { id: 'all', label: 'Tất cả webhook' },
        ]}
        activeId={view}
        onChange={(id) => {
          setView(id as ReconciliationView)
          setPage(0)
        }}
      />
      {list.isError && <Alert variant="destructive">{apiErrorMessage(list.error)}</Alert>}
      <Table
        columns={COLUMNS}
        data={list.data?.data ?? []}
        isLoading={list.isPending}
        emptyMessage={
          view === 'queue' ? 'Không có giao dịch nào cần đối soát.' : 'Chưa có webhook nào.'
        }
        onRowClick={setSelected}
        pagination={list.data ? { meta: list.data.meta, onPageChange: setPage } : undefined}
      />
      {selected && (
        <ReviewDialog key={selected.id} item={selected} onClose={() => setSelected(null)} />
      )}
    </main>
  )
}

const ACTION_LABEL: Record<ReconciliationAction['type'], string> = {
  approve: 'Duyệt và cấp quyền học',
  reject: 'Từ chối',
  match: 'Ghép với thanh toán',
}

function ReviewDialog({ item, onClose }: { item: ReconciliationItem; onClose: () => void }) {
  const action = useReconciliationAction(item.id)
  const actions = allowedActions(item)
  const [reason, setReason] = useState('')
  const [paymentId, setPaymentId] = useState('')
  const parsedPaymentId = Number(paymentId)
  const isPaymentIdValid = Number.isInteger(parsedPaymentId) && parsedPaymentId > 0

  const handleRun = async (type: ReconciliationAction['type']) => {
    const body: ReconciliationAction =
      type === 'match'
        ? { type, reason: reason.trim(), paymentId: parsedPaymentId }
        : { type, reason: reason.trim() }
    await action.mutateAsync(body)
    onClose()
  }

  return (
    <Modal
      isOpen
      onClose={onClose}
      size="lg"
      title={`Giao dịch ${item.providerTransactionId}`}
      description={`${item.provider} · nhận lúc ${formatDateTime(item.receivedAt)}`}
      footer={
        actions.length === 0 ? (
          <Button variant="ghost" onClick={onClose}>
            Đóng
          </Button>
        ) : (
          actions.map((type) => (
            <Button
              key={type}
              variant={type === 'reject' ? 'destructive' : 'primary'}
              disabled={!reason.trim() || (type === 'match' && !isPaymentIdValid)}
              isLoading={action.isPending && action.variables?.type === type}
              onClick={() => void handleRun(type).catch(() => undefined)}
            >
              {ACTION_LABEL[type]}
            </Button>
          ))
        )
      }
    >
      <dl className={styles.facts}>
        <dt>Số tiền nhận</dt>
        <dd>{formatVnd(item.amount)}</dd>
        <dt>Cần thu</dt>
        <dd>{formatVnd(item.expectedAmount)}</dd>
        <dt>Mã thanh toán</dt>
        <dd>{item.paymentReference ?? '—'}</dd>
        <dt>Đơn hàng</dt>
        <dd>{item.orderId ?? '—'}</dd>
        <dt>Lý do vào hàng đợi</dt>
        <dd>{reasonOf(item)}</dd>
        {item.reviewNote && (
          <>
            <dt>Ghi chú</dt>
            <dd>{item.reviewNote}</dd>
          </>
        )}
      </dl>

      {actions.includes('approve') && (
        <Alert variant="info">
          Duyệt sẽ ghi nhận {formatVnd(item.amount)} là số tiền thực nhận, chuyển đơn sang Đã thanh
          toán và cấp quyền học, kể cả khi đơn đã hết hạn.
        </Alert>
      )}
      {actions.includes('match') && (
        <Input
          label="Mã thanh toán (payment id) cần ghép"
          inputMode="numeric"
          value={paymentId}
          onChange={(e) => setPaymentId(e.target.value)}
          errorMessage={paymentId && !isPaymentIdValid ? 'Nhập số nguyên dương' : undefined}
        />
      )}
      {actions.length > 0 && (
        <Input
          label="Lý do (ghi vào nhật ký đối soát)"
          value={reason}
          maxLength={500}
          onChange={(e) => setReason(e.target.value)}
        />
      )}
      {actions.length === 0 && <p className={styles.muted}>Giao dịch này không cần thao tác.</p>}
      {action.isError && <Alert variant="destructive">{apiErrorMessage(action.error)}</Alert>}
    </Modal>
  )
}
