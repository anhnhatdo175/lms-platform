import type { AttemptQuestionView } from './exams.types'
import styles from './exams.module.css'

interface ChoiceQuestionProps {
  question: AttemptQuestionView
  selected: number[]
  isDisabled?: boolean
  onChange: (selectedOptionIds: number[]) => void
}

/** Một câu trắc nghiệm: radio cho một đáp án, checkbox cho nhiều đáp án. Nội dung hiển thị dạng văn bản thuần. */
export function ChoiceQuestion({ question, selected, isDisabled, onChange }: ChoiceQuestionProps) {
  const isMultiple = question.questionType === 'MULTIPLE_CHOICE'
  const name = `aq-${question.id}`

  const handleToggle = (optionId: number, isChecked: boolean) => {
    if (!isMultiple) {
      onChange(isChecked ? [optionId] : [])
      return
    }
    onChange(
      isChecked
        ? [...selected.filter((id) => id !== optionId), optionId]
        : selected.filter((id) => id !== optionId),
    )
  }

  return (
    <fieldset className={styles.question} disabled={isDisabled}>
      <legend className={styles.questionContent}>
        <span className={styles.questionNumber}>Câu {question.position}.</span> {question.content}
      </legend>
      <p className={styles.muted}>
        {isMultiple ? 'Chọn tất cả đáp án đúng' : 'Chọn một đáp án'} · {question.maximumScore} điểm
      </p>
      <div className={styles.options}>
        {question.options.map((option) => {
          const isChecked = selected.includes(option.id)
          return (
            <label
              key={option.id}
              className={`${styles.option} ${isChecked ? styles.optionChecked : ''}`}
            >
              <input
                type={isMultiple ? 'checkbox' : 'radio'}
                name={name}
                checked={isChecked}
                onChange={(e) => handleToggle(option.id, e.target.checked)}
              />
              <span>{option.content}</span>
            </label>
          )
        })}
      </div>
      {!isMultiple && selected.length > 0 && !isDisabled && (
        <button type="button" className={styles.clear} onClick={() => onChange([])}>
          Bỏ chọn
        </button>
      )}
    </fieldset>
  )
}
