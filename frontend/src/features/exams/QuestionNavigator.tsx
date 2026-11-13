import type { SaveState } from './autosaveQueue'
import type { AttemptQuestionView } from './exams.types'
import styles from './exams.module.css'

interface QuestionNavigatorProps {
  questions: AttemptQuestionView[]
  currentIndex: number
  selections: Record<number, number[]>
  saveStates: Record<number, SaveState>
  onSelect: (index: number) => void
}

export function QuestionNavigator({
  questions,
  currentIndex,
  selections,
  saveStates,
  onSelect,
}: QuestionNavigatorProps) {
  return (
    <nav aria-label="Danh sách câu hỏi" className={styles.navigator}>
      {questions.map((q, index) => {
        const isAnswered = (selections[q.id] ?? []).length > 0
        const isFailed = saveStates[q.id] === 'error'
        const classes = [
          styles.navItem,
          isAnswered ? styles.navAnswered : '',
          index === currentIndex ? styles.navCurrent : '',
          isFailed ? styles.navFailed : '',
        ].join(' ')
        return (
          <button
            key={q.id}
            type="button"
            className={classes}
            aria-current={index === currentIndex ? 'step' : undefined}
            aria-label={`Câu ${q.position}${isAnswered ? ', đã trả lời' : ', chưa trả lời'}${isFailed ? ', chưa lưu được' : ''}`}
            onClick={() => onSelect(index)}
          >
            {q.position}
          </button>
        )
      })}
    </nav>
  )
}
