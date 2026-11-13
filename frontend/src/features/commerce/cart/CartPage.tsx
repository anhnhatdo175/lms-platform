import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'

import { Alert, Button, Card, CardBody, CardHeader, Icons, Input, Skeleton } from '@/components/ui'
import { apiErrorMessage } from '@/utils/apiErrorMessage'
import { formatVnd } from '@/utils/format'

import styles from '../commerce.module.css'
import { useCart, useCheckout, useCouponPreview, useRemoveFromCart } from '../commerce.hooks'
import type { CouponPreview } from '../commerce.types'
import { couponReasonMessage } from './couponReason'

/** Giỏ hàng, xem trước mã giảm giá và đặt hàng (Spec 10.1–10.4). Giá cuối luôn do server tính. */
export function CartPage() {
  const navigate = useNavigate()
  const cart = useCart()
  const remove = useRemoveFromCart()
  const preview = useCouponPreview()
  const checkout = useCheckout()
  const [couponInput, setCouponInput] = useState('')
  const [applied, setApplied] = useState<CouponPreview | null>(null)

  const items = cart.data?.items ?? []
  const appliedCode = applied?.valid ? applied.couponCode : null

  const handleApplyCoupon = async (event: FormEvent) => {
    event.preventDefault()
    const code = couponInput.trim()
    if (!code) return
    setApplied(await preview.mutateAsync(code).catch(() => null))
  }

  const handleRemove = async (courseId: number) => {
    await remove.mutateAsync(courseId)
    // Tổng tiền đổi nên phải xem trước lại; bỏ kết quả cũ cho tới khi Student áp lại
    setApplied(null)
  }

  const handleCheckout = async () => {
    if (!cart.data) return
    const result = await checkout.checkout(cart.data, appliedCode)
    await navigate(`/orders/${result.order.id}/payment`)
  }

  if (cart.isPending) {
    return (
      <main className={styles.page}>
        <Skeleton height={40} width="40%" />
        <Skeleton height={160} />
      </main>
    )
  }

  if (cart.isError) {
    return (
      <main className={styles.page}>
        <Alert variant="destructive" title="Không tải được giỏ hàng">
          {apiErrorMessage(cart.error)}
        </Alert>
      </main>
    )
  }

  return (
    <main className={styles.page}>
      <h1 className={styles.title}>Giỏ hàng</h1>

      {items.length === 0 ? (
        <Card>
          <CardBody>
            <p>Giỏ hàng đang trống.</p>
            <Link to="/courses">Xem khóa học</Link>
          </CardBody>
        </Card>
      ) : (
        <div className={styles.layout}>
          <ul className={styles.list} aria-label="Khóa học trong giỏ">
            {items.map((item) => (
              <li key={item.courseId} className={styles.item}>
                {item.thumbnailUrl ? (
                  <img className={styles.thumb} src={item.thumbnailUrl} alt="" />
                ) : (
                  <div className={styles.thumb} aria-hidden />
                )}
                <div className={styles.grow}>
                  <p className={styles.itemTitle}>
                    <Link to={`/courses/${item.courseSlug}`}>{item.courseTitle}</Link>
                  </p>
                </div>
                <span className={styles.price}>{formatVnd(item.price)}</span>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Bỏ ${item.courseTitle} khỏi giỏ`}
                  isLoading={remove.isPending && remove.variables === item.courseId}
                  onClick={() => void handleRemove(item.courseId)}
                >
                  <Icons.Trash size={18} />
                </Button>
              </li>
            ))}
          </ul>

          <Card>
            <CardHeader title="Tóm tắt đơn hàng" />
            <CardBody>
              <div className={styles.summary}>
                <form className={styles.couponRow} onSubmit={(e) => void handleApplyCoupon(e)}>
                  <div className={styles.grow}>
                    <Input
                      label="Mã giảm giá"
                      value={couponInput}
                      onChange={(e) => {
                        setCouponInput(e.target.value)
                        setApplied(null)
                      }}
                      autoComplete="off"
                      maxLength={64}
                      errorMessage={
                        applied && !applied.valid
                          ? couponReasonMessage(applied.reasonCode)
                          : preview.isError
                            ? apiErrorMessage(preview.error)
                            : undefined
                      }
                      helperText={appliedCode ? `Đã áp dụng ${appliedCode}` : undefined}
                    />
                  </div>
                  <Button
                    type="submit"
                    variant="outline"
                    isLoading={preview.isPending}
                    disabled={!couponInput.trim()}
                  >
                    Áp dụng
                  </Button>
                </form>

                <div className={styles.row}>
                  <span>Tạm tính ({cart.data.itemCount} khóa)</span>
                  <span>{formatVnd(cart.data.subtotal)}</span>
                </div>
                {applied?.valid && (
                  <div className={`${styles.row} ${styles.discount}`}>
                    <span>Giảm giá</span>
                    <span>−{formatVnd(applied.discountTotal)}</span>
                  </div>
                )}
                <div className={styles.totalRow}>
                  <span>Tổng cộng</span>
                  <span>{formatVnd(applied?.valid ? applied.total : cart.data.subtotal)}</span>
                </div>
                <p className={styles.muted}>Số tiền cuối cùng được xác nhận khi tạo đơn hàng.</p>

                {checkout.isError && (
                  <Alert variant="destructive" title="Chưa đặt được hàng">
                    {apiErrorMessage(checkout.error)}
                  </Alert>
                )}
                <Button
                  variant="accent"
                  size="lg"
                  fullWidth
                  isLoading={checkout.isPending}
                  leftIcon={<Icons.ShoppingCart size={18} />}
                  onClick={() => void handleCheckout().catch(() => undefined)}
                >
                  Đặt hàng và thanh toán
                </Button>
              </div>
            </CardBody>
          </Card>
        </div>
      )}
    </main>
  )
}
