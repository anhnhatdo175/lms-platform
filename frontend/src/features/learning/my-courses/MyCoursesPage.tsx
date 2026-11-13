import { useState } from 'react'
import { useNavigate } from 'react-router'

import { Alert, Badge, Button, Card, CardBody, Progress, Skeleton } from '@/components/ui'
import { apiErrorMessage } from '@/utils/apiErrorMessage'
import { formatDateTime } from '@/utils/format'

import styles from './MyCoursesPage.module.css'
import { learningPath, useMyCourses, type MyCourse } from './myCourses.api'

export function MyCoursesPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const courses = useMyCourses(page)

  return (
    <main className={styles.page}>
      <h1 className={styles.title}>Khóa học của tôi</h1>
      {courses.isError && <Alert variant="destructive">{apiErrorMessage(courses.error)}</Alert>}
      {courses.isPending && (
        <div className={styles.grid}>
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} height={160} />
          ))}
        </div>
      )}
      {courses.data?.data.length === 0 && (
        <Card>
          <CardBody>
            <p>Bạn chưa đăng ký khóa học nào.</p>
            <Button variant="primary" onClick={() => void navigate('/courses')}>
              Khám phá khóa học
            </Button>
          </CardBody>
        </Card>
      )}
      <div className={styles.grid}>
        {courses.data?.data.map((course) => (
          <Card key={course.id}>
            <CardBody>
              <div className={styles.card}>
                <div className={styles.head}>
                  <h2 className={styles.courseTitle}>{course.courseTitle}</h2>
                  <AccessBadge course={course} />
                </div>
                <Progress
                  value={Number(course.progressPercent ?? 0)}
                  isLabelVisible
                  label="Tiến độ"
                />
                <p className={styles.muted}>
                  {course.expiresAt
                    ? `Hạn học tới ${formatDateTime(course.expiresAt)}`
                    : 'Học không giới hạn thời gian'}
                </p>
                <Button
                  variant={course.status === 'ACTIVE' ? 'primary' : 'outline'}
                  disabled={course.status === 'REVOKED'}
                  onClick={() =>
                    void navigate(
                      course.status === 'ACTIVE'
                        ? learningPath(course)
                        : `/courses/${course.courseSlug}`,
                    )
                  }
                >
                  {course.status === 'ACTIVE'
                    ? 'Tiếp tục học'
                    : course.status === 'EXPIRED'
                      ? 'Gia hạn'
                      : 'Đã thu hồi'}
                </Button>
              </div>
            </CardBody>
          </Card>
        ))}
      </div>
      {courses.data && courses.data.meta.totalPages > 1 && (
        <div className={styles.pager}>
          <Button variant="outline" disabled={page === 0} onClick={() => setPage(page - 1)}>
            Trước
          </Button>
          <span>
            Trang {page + 1}/{courses.data.meta.totalPages}
          </span>
          <Button
            variant="outline"
            disabled={page + 1 >= courses.data.meta.totalPages}
            onClick={() => setPage(page + 1)}
          >
            Sau
          </Button>
        </div>
      )}
    </main>
  )
}

function AccessBadge({ course }: { course: MyCourse }) {
  if (course.status === 'ACTIVE') return <Badge variant="success">Đang học</Badge>
  if (course.status === 'EXPIRED') return <Badge variant="warning">Hết hạn</Badge>
  return <Badge variant="destructive">Đã thu hồi</Badge>
}
