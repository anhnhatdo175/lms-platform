import type { RouteObject } from 'react-router'

import { ReconciliationPage } from './admin/reconciliation/ReconciliationPage'
import { CartPage } from './commerce/cart/CartPage'
import { OrderDetailPage } from './commerce/orders/OrderDetailPage'
import { OrderHistoryPage } from './commerce/orders/OrderHistoryPage'
import { PaymentPage } from './commerce/payment/PaymentPage'
import { ExamAttemptPage } from './exams/ExamAttemptPage'
import { ExamResultPage } from './exams/ExamResultPage'
import { ExamStartPage } from './exams/ExamStartPage'
import { MyCoursesPage } from './learning/my-courses/MyCoursesPage'

/**
 * Các trang của Người B, để router của A gắn vào: `studentRoutesB` đặt dưới ProtectedRoute (đã đăng nhập),
 * `adminRoutesB` đặt dưới RoleRoute ADMIN. Đường dẫn trùng tên với API cho dễ dò.
 */
export const studentRoutesB: RouteObject[] = [
  { path: '/cart', element: <CartPage /> },
  { path: '/orders', element: <OrderHistoryPage /> },
  { path: '/orders/:orderId', element: <OrderDetailPage /> },
  { path: '/orders/:orderId/payment', element: <PaymentPage /> },
  { path: '/my-courses', element: <MyCoursesPage /> },
  { path: '/learning/exams/:examId', element: <ExamStartPage /> },
  { path: '/exam-attempts/:attemptId', element: <ExamAttemptPage /> },
  { path: '/exam-attempts/:attemptId/result', element: <ExamResultPage /> },
]

export const adminRoutesB: RouteObject[] = [
  { path: '/admin/reconciliation', element: <ReconciliationPage /> },
]
