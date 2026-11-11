import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { api, type DataResponse, type PageResponse } from '@/lib/api-client'
import type { PaymentStatus, ReviewReasonCode } from '@/features/commerce/commerce.types'

export type WebhookStatus =
  'RECEIVED' | 'VALIDATED' | 'MATCHED' | 'PROCESSED' | 'DUPLICATE' | 'UNMATCHED' | 'FAILED'
export type UnmatchedReason =
  'NO_REFERENCE' | 'UNKNOWN_REFERENCE' | 'ALREADY_PAID' | 'PAYMENT_NOT_PENDING'

export type ReconciliationItem = {
  id: number
  provider: string
  providerTransactionId: string
  amount: number
  status: WebhookStatus
  orderId: number | null
  receivedAt: string
  reviewNote: string | null
  paymentId: number | null
  paymentReference: string | null
  paymentStatus: PaymentStatus | null
  expectedAmount: number | null
  reviewReason: string | null
  lastAction: 'APPROVE' | 'REJECT' | 'MATCH' | null
  unmatchedReason: UnmatchedReason | null
  reviewReasonCode: ReviewReasonCode | null
}

export type ReconciliationView = 'queue' | 'all'

const listPath = (view: ReconciliationView) =>
  view === 'queue' ? '/admin/reconciliation' : '/admin/payment-webhooks'

export function useReconciliationList(view: ReconciliationView, page: number) {
  return useQuery({
    queryKey: ['admin', 'reconciliation', view, page],
    queryFn: () =>
      api.get<PageResponse<ReconciliationItem>>(listPath(view), { query: { page, size: 20 } }),
  })
}

export type ReconciliationAction =
  | { type: 'approve'; reason: string }
  | { type: 'reject'; reason: string }
  | { type: 'match'; reason: string; paymentId: number }

export function useReconciliationAction(webhookId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (action: ReconciliationAction) =>
      api
        .post<DataResponse<ReconciliationItem>>(
          `/admin/reconciliation/${webhookId}/${action.type}`,
          action.type === 'match'
            ? { paymentId: action.paymentId, reason: action.reason }
            : { reason: action.reason },
        )
        .then((r) => r.data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'reconciliation'] }),
  })
}

/** Thao tác hợp lệ theo trạng thái, khớp điều kiện ở `ReconciliationService` của backend. */
export function allowedActions(item: ReconciliationItem): ReconciliationAction['type'][] {
  if (item.status === 'MATCHED' && item.paymentStatus === 'REVIEW_REQUIRED')
    return ['approve', 'reject']
  if (item.status === 'UNMATCHED' && item.lastAction === null) return ['match', 'reject']
  return []
}
