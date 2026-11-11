import { describe, expect, it } from 'vitest'

import { allowedActions, type ReconciliationItem } from './reconciliation.api'

const base: ReconciliationItem = {
  id: 1,
  provider: 'SEPAY',
  providerTransactionId: '92704',
  amount: 500000,
  status: 'MATCHED',
  orderId: 1,
  receivedAt: '2026-11-10T08:00:00Z',
  reviewNote: null,
  paymentId: 1,
  paymentReference: 'LMS7K2M9QX4PZ',
  paymentStatus: 'REVIEW_REQUIRED',
  expectedAmount: 600000,
  reviewReason: null,
  lastAction: null,
  unmatchedReason: null,
  reviewReasonCode: 'AMOUNT_UNDERPAID',
}

describe('allowedActions', () => {
  it('review-required payment can be approved or rejected', () => {
    expect(allowedActions(base)).toEqual(['approve', 'reject'])
  })

  it('unmatched webhook can be matched or closed once', () => {
    const unmatched = {
      ...base,
      status: 'UNMATCHED' as const,
      paymentId: null,
      paymentStatus: null,
    }
    expect(allowedActions(unmatched)).toEqual(['match', 'reject'])
    expect(allowedActions({ ...unmatched, lastAction: 'REJECT' })).toEqual([])
  })

  it('settled webhooks have no actions', () => {
    expect(allowedActions({ ...base, status: 'PROCESSED', paymentStatus: 'PAID' })).toEqual([])
    expect(allowedActions({ ...base, status: 'DUPLICATE' })).toEqual([])
    expect(allowedActions({ ...base, paymentStatus: 'FAILED' })).toEqual([])
  })
})
