import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useRef } from 'react'

import { cartApi, ordersApi } from './commerce.api'
import type { Cart, PaymentState } from './commerce.types'

export const commerceKeys = {
  cart: ['cart'] as const,
  orders: (page: number) => ['orders', 'list', page] as const,
  order: (orderId: number) => ['orders', 'detail', orderId] as const,
  paymentStatus: (orderId: number) => ['orders', 'payment-status', orderId] as const,
}

export function useCart() {
  return useQuery({ queryKey: commerceKeys.cart, queryFn: cartApi.get })
}

export function useAddToCart() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: cartApi.addItem,
    onSuccess: (cart: Cart) => queryClient.setQueryData(commerceKeys.cart, cart),
  })
}

export function useRemoveFromCart() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: cartApi.removeItem,
    onSuccess: (cart: Cart) => queryClient.setQueryData(commerceKeys.cart, cart),
  })
}

export function useCouponPreview() {
  return useMutation({ mutationFn: cartApi.previewCoupon })
}

/**
 * Giữ một Idempotency-Key cho tới khi đặt hàng thành công hoặc nội dung đặt hàng đổi (mã giảm giá, giỏ hàng):
 * bấm lại sau lỗi mạng sẽ không tạo Order thứ hai.
 */
export function useCheckout() {
  const queryClient = useQueryClient()
  const attempt = useRef<{ key: string; signature: string } | null>(null)

  const mutation = useMutation({
    mutationFn: ({ couponCode, signature }: { couponCode: string | null; signature: string }) => {
      if (!attempt.current || attempt.current.signature !== signature) {
        attempt.current = { key: crypto.randomUUID(), signature }
      }
      return ordersApi.checkout(attempt.current.key, couponCode)
    },
    onSuccess: () => {
      attempt.current = null
      void queryClient.invalidateQueries({ queryKey: commerceKeys.cart })
      void queryClient.invalidateQueries({ queryKey: ['orders', 'list'] })
    },
  })

  const checkout = useCallback(
    (cart: Cart, couponCode: string | null) =>
      mutation.mutateAsync({
        couponCode,
        signature: `${cart.items.map((i) => i.courseId).join(',')}|${couponCode ?? ''}`,
      }),
    [mutation],
  )

  return { ...mutation, checkout }
}

export function useOrders(page: number) {
  return useQuery({ queryKey: commerceKeys.orders(page), queryFn: () => ordersApi.list(page) })
}

export function useOrder(orderId: number) {
  return useQuery({ queryKey: commerceKeys.order(orderId), queryFn: () => ordersApi.get(orderId) })
}

export function useCancelOrder(orderId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (reason?: string) => ordersApi.cancel(orderId, reason),
    onSuccess: (order) => {
      queryClient.setQueryData(commerceKeys.order(orderId), order)
      void queryClient.invalidateQueries({ queryKey: ['orders', 'list'] })
      void queryClient.invalidateQueries({ queryKey: commerceKeys.paymentStatus(orderId) })
    },
  })
}

const POLL_MS = 3000

/** Hỏi trạng thái mỗi 3 giây khi còn chờ chuyển khoản; dừng khi đã có kết quả cuối. */
export function usePaymentStatus(orderId: number) {
  const queryClient = useQueryClient()
  return useQuery({
    queryKey: commerceKeys.paymentStatus(orderId),
    queryFn: async () => {
      const state = await ordersApi.paymentStatus(orderId)
      if (state.orderStatus !== 'PENDING_PAYMENT') {
        void queryClient.invalidateQueries({ queryKey: commerceKeys.order(orderId) })
        void queryClient.invalidateQueries({ queryKey: ['my-courses'] })
      }
      return state
    },
    refetchInterval: (query) => (isAwaitingTransfer(query.state.data) ? POLL_MS : false),
    refetchIntervalInBackground: false,
  })
}

export function isAwaitingTransfer(state: PaymentState | undefined): boolean {
  return !state || (state.orderStatus === 'PENDING_PAYMENT' && state.paymentStatus === 'PENDING')
}
