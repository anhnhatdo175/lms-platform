import { Icons } from '@/components/ui'
import { formatDuration } from '@/utils/format'

import { urgency } from './examClock'
import styles from './exams.module.css'

export function ExamTimer({ remainingMs }: { remainingMs: number }) {
  const level = urgency(remainingMs)
  return (
    <div
      role="timer"
      aria-label={`Thời gian còn lại ${formatDuration(remainingMs)}`}
      className={`${styles.timer} ${level === 'warning' ? styles.timerWarning : ''} ${level === 'critical' ? styles.timerCritical : ''}`}
    >
      <Icons.Clock size={18} />
      <span>{formatDuration(remainingMs)}</span>
    </div>
  )
}
