import { useEffect, useMemo, useState, type FormEvent } from 'react'

import { Alert, Button, Card, CardBody, Input } from '@/components/ui'
import {
  getTeacherCurriculum,
  listTeacherCourses,
  type LessonDetail,
  type ManagedCourse,
  type TeacherCurriculumSection,
} from '@/services/courseApi'
import { apiErrorMessage } from '@/utils/apiErrorMessage'
import { formatScore } from '@/utils/format'

import styles from './teacherAssessments.module.css'
import {
  teacherExamsApi,
  type Difficulty,
  type TeacherExamBuilder,
  type TeacherExamStatistics,
  type TeacherQuestion,
} from './teacherExams.api'

const difficulties: Array<{ value: Difficulty; label: string }> = [
  { value: 'EASY', label: 'Cơ bản' },
  { value: 'MEDIUM', label: 'Trung bình' },
  { value: 'HARD', label: 'Nâng cao' },
]

const resultModes = [
  { value: 'IMMEDIATE_SCORE_ONLY', label: 'Hiện điểm sau khi nộp' },
  { value: 'IMMEDIATE_WITH_CORRECT_ANSWERS', label: 'Hiện điểm và đáp án' },
  { value: 'AFTER_EXAM_CLOSED', label: 'Chỉ công bố khi đóng bài thi' },
] as const

interface QuestionOptionInput {
  id: string
  content: string
  correct: boolean
}

let nextOptionId = 0

function createOptionInput(content = '', correct = false): QuestionOptionInput {
  nextOptionId += 1
  return { id: `option-${nextOptionId}`, content, correct }
}

const blankOptions: QuestionOptionInput[] = [
  createOptionInput('', true),
  createOptionInput(),
]

export function TeacherAssessmentsPage({ accessToken }: { accessToken: string }) {
  const [courses, setCourses] = useState<ManagedCourse[]>([])
  const [courseId, setCourseId] = useState('')
  const [sections, setSections] = useState<TeacherCurriculumSection[]>([])
  const [questions, setQuestions] = useState<TeacherQuestion[]>([])
  const [selectedLessonId, setSelectedLessonId] = useState<number | null>(null)
  const [builder, setBuilder] = useState<TeacherExamBuilder | null>(null)
  const [statistics, setStatistics] = useState<TeacherExamStatistics | null>(null)
  const [isLoadingCourses, setIsLoadingCourses] = useState(true)
  const [isLoadingCurriculum, setIsLoadingCurriculum] = useState(false)
  const [isLoadingLesson, setIsLoadingLesson] = useState(false)
  const [isSavingQuestion, setIsSavingQuestion] = useState(false)
  const [isSavingExam, setIsSavingExam] = useState(false)
  const [busyAction, setBusyAction] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [optionInputs, setOptionInputs] = useState<QuestionOptionInput[]>(blankOptions)
  const [editingQuestionId, setEditingQuestionId] = useState<number | null>(null)
  const [questionType, setQuestionType] = useState<'SINGLE_CHOICE' | 'MULTIPLE_CHOICE'>('SINGLE_CHOICE')
  const [scoreInput, setScoreInput] = useState('1')

  useEffect(() => {
    let active = true
    listTeacherCourses(accessToken)
      .then((page) => {
        if (!active) return
        setCourses(page.data)
        if (page.data.length) {
          setIsLoadingCurriculum(true)
          setCourseId(String(page.data[0].id))
        }
      })
      .catch((cause: unknown) => {
        if (active) setError(apiErrorMessage(cause))
      })
      .finally(() => {
        if (active) setIsLoadingCourses(false)
      })
    return () => {
      active = false
    }
  }, [accessToken])

  useEffect(() => {
    if (!courseId) return undefined
    let active = true
    const selectedCourse = Number(courseId)
    Promise.all([
      getTeacherCurriculum(accessToken, selectedCourse),
      teacherExamsApi.listQuestions(selectedCourse),
    ])
      .then(([curriculum, questionPage]) => {
        if (!active) return
        setSections(curriculum.sections)
        setQuestions(questionPage.data)
      })
      .catch((cause: unknown) => {
        if (active) setError(apiErrorMessage(cause))
      })
      .finally(() => {
        if (active) setIsLoadingCurriculum(false)
      })
    return () => {
      active = false
    }
  }, [accessToken, courseId])

  const isLoading = isLoadingCourses || isLoadingCurriculum
  const lessons = useMemo(
    () => sections.flatMap((item) => item.lessons.map((lesson) => ({
      ...lesson,
      sectionTitle: item.section.title,
    }))),
    [sections],
  )
  const selectedLesson = lessons.find((lesson) => lesson.id === selectedLessonId) ?? null
  const examLessons = lessons.filter((lesson) => lesson.lessonType === 'EXAM')

  async function refreshExam(examId: number) {
    const updated = await teacherExamsApi.getExam(examId)
    setBuilder(updated)
    setSections((current) => current.map((item) => ({
      ...item,
      lessons: item.lessons.map((lesson) => lesson.id === updated.exam.lessonId
        ? { ...lesson, examId }
        : lesson),
    })))
  }

  async function openLesson(lesson: LessonDetail) {
    setSelectedLessonId(lesson.id)
    setBuilder(null)
    setStatistics(null)
    setError('')
    setNotice('')
    if (!lesson.examId) return
    setIsLoadingLesson(true)
    try {
      await refreshExam(lesson.examId)
    } catch (cause) {
      setError(apiErrorMessage(cause))
    } finally {
      setIsLoadingLesson(false)
    }
  }

  function editQuestion(question: TeacherQuestion) {
    setEditingQuestionId(question.id)
    setQuestionType(question.questionType)
    setOptionInputs(question.options.map((option) => createOptionInput(option.content, option.correct)))
    setError('')
    setNotice('')
  }

  function cancelQuestionEdit() {
    setEditingQuestionId(null)
    setQuestionType('SINGLE_CHOICE')
    setOptionInputs(blankOptions)
    setError('')
  }

  async function saveQuestion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!courseId || isSavingQuestion) return
    const form = event.currentTarget
    const values = new FormData(form)
    const content = String(values.get('questionContent') ?? '').trim()
    const options = optionInputs
      .map((option) => ({ content: option.content.trim(), correct: option.correct }))
      .filter((option) => option.content)
    const correctCount = options.filter((option) => option.correct).length
    if (!content || options.length < 2 || correctCount < 1
      || (questionType === 'SINGLE_CHOICE' && correctCount !== 1)) {
      setError(questionType === 'SINGLE_CHOICE'
        ? 'Câu hỏi cần ít nhất hai lựa chọn và đúng chính xác một đáp án.'
        : 'Câu hỏi cần ít nhất hai lựa chọn và một đáp án đúng.')
      return
    }

    setIsSavingQuestion(true)
    setError('')
    setNotice('')
    try {
      const input = {
        content,
        questionType,
        difficulty: String(values.get('difficulty')) as Difficulty,
        skillCode: String(values.get('skillCode') ?? '').trim() || null,
        explanation: String(values.get('explanation') ?? '').trim() || null,
        options: options.map((option, index) => ({ ...option, position: index + 1 })),
      }
      if (editingQuestionId !== null) {
        const updated = await teacherExamsApi.updateQuestion(editingQuestionId, input)
        setQuestions((current) => current.map((item) => item.id === updated.id ? updated : item))
        setBuilder((current) => current ? {
          ...current,
          questions: current.questions.map((item) => item.questionId === updated.id
            ? { ...item, question: updated }
            : item),
        } : current)
        setNotice('Đã cập nhật câu hỏi.')
      } else {
        const created = await teacherExamsApi.createQuestion(Number(courseId), input)
        setQuestions((current) => [created, ...current])
        setNotice('Đã thêm câu hỏi vào ngân hàng của khóa học.')
      }
      form.reset()
      setEditingQuestionId(null)
      setQuestionType('SINGLE_CHOICE')
      setOptionInputs(blankOptions)
    } catch (cause) {
      setError(apiErrorMessage(cause))
    } finally {
      setIsSavingQuestion(false)
    }
  }

  async function createExam(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedLesson || isSavingExam) return
    const form = event.currentTarget
    const values = new FormData(form)
    const durationMinutes = Number(values.get('durationMinutes'))
    const passingScore = Number(values.get('passingScore'))
    const maximumAttemptsValue = String(values.get('maximumAttempts') ?? '').trim()
    const maximumAttempts = maximumAttemptsValue ? Number(maximumAttemptsValue) : null
    if (!Number.isSafeInteger(durationMinutes) || durationMinutes < 1
      || !Number.isFinite(passingScore) || passingScore < 0 || passingScore > 100
      || (maximumAttempts !== null && (!Number.isSafeInteger(maximumAttempts) || maximumAttempts < 1))) {
      setError('Kiểm tra lại thời lượng, điểm đạt và số lần làm bài.')
      return
    }

    setIsSavingExam(true)
    setError('')
    try {
      const exam = await teacherExamsApi.createExam(selectedLesson.id, {
        title: String(values.get('examTitle') ?? '').trim(),
        description: String(values.get('examDescription') ?? '').trim() || null,
        durationMinutes,
        passingScore,
        maximumAttempts,
        shuffleQuestions: values.get('shuffleQuestions') === 'on',
        shuffleOptions: values.get('shuffleOptions') === 'on',
        resultDisplayMode: String(values.get('resultDisplayMode')) as TeacherExamBuilder['exam']['resultDisplayMode'],
        startAt: null,
        endAt: null,
      })
      await refreshExam(exam.id)
      setNotice('Đã tạo bài kiểm tra ở trạng thái nháp.')
    } catch (cause) {
      setError(apiErrorMessage(cause))
    } finally {
      setIsSavingExam(false)
    }
  }

  async function addQuestionToExam(question: TeacherQuestion) {
    if (!builder || busyAction) return
    const score = Number(scoreInput)
    if (!Number.isFinite(score) || score <= 0) {
      setError('Điểm cho mỗi câu phải lớn hơn 0.')
      return
    }
    setBusyAction('add-question')
    setError('')
    try {
      const updated = await teacherExamsApi.addQuestion(builder.exam.id, question.id, score)
      setBuilder(updated)
      setNotice('Đã thêm câu hỏi vào bài kiểm tra.')
    } catch (cause) {
      setError(apiErrorMessage(cause))
    } finally {
      setBusyAction('')
    }
  }

  async function removeQuestionFromExam(questionId: number) {
    if (!builder || busyAction) return
    setBusyAction(`remove-question-${questionId}`)
    setError('')
    try {
      await teacherExamsApi.removeQuestion(builder.exam.id, questionId)
      await refreshExam(builder.exam.id)
      setNotice('Đã bỏ câu hỏi khỏi bài kiểm tra.')
    } catch (cause) {
      setError(apiErrorMessage(cause))
    } finally {
      setBusyAction('')
    }
  }

  async function updateExam(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!builder || isSavingExam) return
    const values = new FormData(event.currentTarget)
    const durationMinutes = Number(values.get('durationMinutes'))
    const passingScore = Number(values.get('passingScore'))
    const maxAttemptsRaw = String(values.get('maximumAttempts') ?? '').trim()
    const maximumAttempts = maxAttemptsRaw ? Number(maxAttemptsRaw) : null
    if (!Number.isSafeInteger(durationMinutes) || durationMinutes < 1
      || !Number.isFinite(passingScore) || passingScore < 0 || passingScore > 100
      || (maximumAttempts !== null && (!Number.isSafeInteger(maximumAttempts) || maximumAttempts < 1))) {
      setError('Kiểm tra lại thời lượng, điểm đạt và số lần làm bài.')
      return
    }

    setIsSavingExam(true)
    setError('')
    try {
      const updated = await teacherExamsApi.updateExam(builder.exam.id, {
        title: String(values.get('examTitle') ?? '').trim(),
        description: String(values.get('examDescription') ?? '').trim() || null,
        durationMinutes,
        passingScore,
        maximumAttempts,
        shuffleQuestions: values.get('shuffleQuestions') === 'on',
        shuffleOptions: values.get('shuffleOptions') === 'on',
        resultDisplayMode: String(values.get('resultDisplayMode')) as TeacherExamBuilder['exam']['resultDisplayMode'],
      })
      setBuilder((current) => current ? { ...current, exam: updated } : current)
      setNotice('Đã lưu cài đặt bài kiểm tra.')
    } catch (cause) {
      setError(apiErrorMessage(cause))
    } finally {
      setIsSavingExam(false)
    }
  }

  async function transitionExam(action: 'publish' | 'close') {
    if (!builder || busyAction) return
    setBusyAction(action)
    setError('')
    try {
      const exam = action === 'publish'
        ? await teacherExamsApi.publish(builder.exam.id)
        : await teacherExamsApi.close(builder.exam.id)
      setBuilder((current) => current ? { ...current, exam } : current)
      setNotice(action === 'publish' ? 'Bài kiểm tra đã được phát hành.' : 'Bài kiểm tra đã được đóng.')
    } catch (cause) {
      setError(apiErrorMessage(cause))
    } finally {
      setBusyAction('')
    }
  }

  async function loadStatistics() {
    if (!builder) return
    setBusyAction('statistics')
    setError('')
    try {
      setStatistics(await teacherExamsApi.statistics(builder.exam.id))
    } catch (cause) {
      setError(apiErrorMessage(cause))
    } finally {
      setBusyAction('')
    }
  }

  async function deactivateQuestion(questionId: number) {
    if (busyAction) return
    setBusyAction(`deactivate-${questionId}`)
    setError('')
    try {
      await teacherExamsApi.deactivateQuestion(questionId)
      setQuestions((current) => current.filter((question) => question.id !== questionId))
      if (editingQuestionId === questionId) cancelQuestionEdit()
      setNotice('Đã ẩn câu hỏi khỏi ngân hàng.')
    } catch (cause) {
      setError(apiErrorMessage(cause))
    } finally {
      setBusyAction('')
    }
  }

  return (
    <main className={styles.page} aria-labelledby="teacher-assessments-title">
      <header className={styles.heading}>
        <div>
          <p className={styles.eyebrow}>CÔNG CỤ GIẢNG VIÊN</p>
          <h1 id="teacher-assessments-title">Ngân hàng câu hỏi & bài kiểm tra</h1>
          <p>Tạo câu hỏi theo khóa học, ghép vào bài kiểm tra và theo dõi kết quả đã chấm.</p>
        </div>
      </header>

      {error && <Alert variant="destructive" title="Chưa hoàn thành thao tác">{error}</Alert>}
      {notice && <Alert variant="success">{notice}</Alert>}

      <Card>
        <CardBody>
          <label className={styles.coursePicker}>
            <span>Khóa học</span>
            <select
              aria-label="Khóa học"
              onChange={(event) => {
                setCourseId(event.target.value)
                setIsLoadingCurriculum(Boolean(event.target.value))
                setSections([])
                setQuestions([])
                setEditingQuestionId(null)
                setSelectedLessonId(null)
                setBuilder(null)
                setStatistics(null)
                setError('')
                setNotice('')
              }}
              value={courseId}
            >
              {courses.length === 0 && <option value="">Chưa có khóa học</option>}
              {courses.map((course) => <option key={course.id} value={course.id}>{course.title}</option>)}
            </select>
          </label>
        </CardBody>
      </Card>

      {isLoading ? (
        <p className={styles.stateMessage}>Đang tải khóa học và nội dung...</p>
      ) : courses.length === 0 ? (
        <div className={styles.emptyState}>Tạo khóa học trước để quản lý câu hỏi và bài kiểm tra.</div>
      ) : (
        <div className={styles.columns}>
          <div className={styles.leftColumn}>
            <QuestionBank
              busyAction={busyAction}
              editingQuestion={questions.find((question) => question.id === editingQuestionId) ?? null}
              onCancelEdit={cancelQuestionEdit}
              isSaving={isSavingQuestion}
              onEdit={editQuestion}
              onSubmit={saveQuestion}
              onDeactivate={(id) => void deactivateQuestion(id)}
              onOptionChange={(index, field, value) => setOptionInputs((current) => current.map((option, optionIndex) => {
                if (field === 'correct' && questionType === 'SINGLE_CHOICE') {
                  return { ...option, correct: optionIndex === index && Boolean(value) }
                }
                if (optionIndex !== index) return option
                return field === 'correct'
                  ? { ...option, correct: Boolean(value) }
                  : { ...option, content: String(value) }
              }))}
              onOptionRemove={(index) => setOptionInputs((current) => current.filter((_, optionIndex) => optionIndex !== index))}
              onOptionAdd={() => setOptionInputs((current) => [...current, createOptionInput()])}
              onQuestionTypeChange={setQuestionType}
              options={optionInputs}
              questions={questions}
              questionType={questionType}
            />
          </div>

          <section className={styles.panel} aria-labelledby="exam-lessons-title">
            <div className={styles.panelHeading}>
              <div>
                <h2 id="exam-lessons-title">Bài kiểm tra trong khóa học</h2>
                <p>Chọn bài học loại Bài kiểm tra để tạo hoặc quản lý cấu hình.</p>
              </div>
            </div>
            {examLessons.length === 0 ? (
              <div className={styles.emptyState}>Khóa học chưa có bài học loại Bài kiểm tra. Hãy thêm một bài học EXAM trong Soạn khóa học.</div>
            ) : (
              <ul className={styles.lessonPicker}>
                {examLessons.map((lesson) => (
                  <li key={lesson.id}>
                    <button
                      aria-pressed={selectedLessonId === lesson.id}
                      className={selectedLessonId === lesson.id ? styles.selectedLesson : ''}
                      onClick={() => void openLesson(lesson)}
                      type="button"
                    >
                      <span>
                        <strong>{lesson.title}</strong>
                        <small>{lesson.sectionTitle}</small>
                      </span>
                      <span className={lesson.examId ? styles.examReady : styles.examMissing}>
                        {lesson.examId ? 'Đã cấu hình' : 'Chưa có bài thi'}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}

            {isLoadingLesson && <p className={styles.stateMessage}>Đang tải cấu hình bài thi...</p>}
            {selectedLesson && !builder && !isLoadingLesson && !selectedLesson.examId && (
              <ExamSettingsForm isSaving={isSavingExam} onSubmit={createExam} />
            )}
            {builder && <ExamBuilderPanel
              builder={builder}
              busyAction={busyAction}
              onAddQuestion={(question) => void addQuestionToExam(question)}
              onClose={() => void transitionExam('close')}
              onLoadStatistics={() => void loadStatistics()}
              onRemoveQuestion={(id) => void removeQuestionFromExam(id)}
              onScoreChange={setScoreInput}
              onSubmit={updateExam}
              onPublish={() => void transitionExam('publish')}
              score={scoreInput}
              isSaving={isSavingExam}
              questions={questions}
              statistics={statistics}
            />}
          </section>
        </div>
      )}
    </main>
  )
}

function QuestionBank({
  busyAction,
  editingQuestion,
  isSaving,
  onCancelEdit,
  onDeactivate,
  onEdit,
  onOptionChange,
  onOptionRemove,
  onOptionAdd,
  onQuestionTypeChange,
  onSubmit,
  options,
  questions,
  questionType,
}: {
  busyAction: string
  editingQuestion: TeacherQuestion | null
  isSaving: boolean
  onCancelEdit: () => void
  onDeactivate: (id: number) => void
  onEdit: (question: TeacherQuestion) => void
  onOptionChange: (index: number, field: keyof QuestionOptionInput, value: string | boolean) => void
  onOptionRemove: (index: number) => void
  onOptionAdd: () => void
  onQuestionTypeChange: (type: 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE') => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  options: QuestionOptionInput[]
  questions: TeacherQuestion[]
  questionType: 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE'
}) {
  return (
    <section className={styles.panel} aria-labelledby="question-bank-title">
      <div className={styles.panelHeading}>
        <div>
          <h2 id="question-bank-title">Ngân hàng câu hỏi</h2>
          <p>{questions.length} câu đang hoạt động trong khóa học.</p>
        </div>
      </div>
      <form className={styles.form} key={editingQuestion?.id ?? 'new-question'} onSubmit={onSubmit}>
        {editingQuestion && <h3>Chỉnh sửa câu hỏi</h3>}
        <label>
          <span>Nội dung câu hỏi</span>
          <textarea defaultValue={editingQuestion?.content ?? ''} maxLength={10000} name="questionContent" required rows={3} />
        </label>
        <div className={styles.formGrid}>
          <label>
            <span>Loại câu hỏi</span>
            <select onChange={(event) => onQuestionTypeChange(event.target.value as typeof questionType)} value={questionType}>
              <option value="SINGLE_CHOICE">Một đáp án đúng</option>
              <option value="MULTIPLE_CHOICE">Nhiều đáp án đúng</option>
            </select>
          </label>
          <label>
            <span>Độ khó</span>
            <select defaultValue={editingQuestion?.difficulty ?? 'MEDIUM'} name="difficulty">
              {difficulties.map((difficulty) => <option key={difficulty.value} value={difficulty.value}>{difficulty.label}</option>)}
            </select>
          </label>
          <label>
            <span>Mã kỹ năng</span>
            <input defaultValue={editingQuestion?.skillCode ?? ''} maxLength={150} name="skillCode" />
          </label>
        </div>
        <fieldset className={styles.optionsFieldset}>
          <legend>Lựa chọn trả lời</legend>
          {options.map((option, index) => (
            <div className={styles.optionRow} key={option.id}>
              <label className={styles.optionText}>
                <span className="sr-only">Lựa chọn {index + 1}</span>
                <input
                  onChange={(event) => onOptionChange(index, 'content', event.target.value)}
                  placeholder={`Lựa chọn ${index + 1}`}
                  value={option.content}
                />
              </label>
              <label className={styles.correctCheck}>
                <input
                  checked={option.correct}
                  onChange={(event) => onOptionChange(index, 'correct', event.target.checked)}
                  type={questionType === 'SINGLE_CHOICE' ? 'radio' : 'checkbox'}
                  name="correct-option"
                />
                <span>Đáp án đúng</span>
              </label>
              {options.length > 2 && (
                <button className={styles.iconButton} onClick={() => onOptionRemove(index)} type="button" aria-label={`Xóa lựa chọn ${index + 1}`}>
                  ×
                </button>
              )}
            </div>
          ))}
          <button className={styles.textButton} onClick={onOptionAdd} type="button">+ Thêm lựa chọn</button>
        </fieldset>
        <label>
          <span>Giải thích đáp án</span>
          <textarea defaultValue={editingQuestion?.explanation ?? ''} maxLength={5000} name="explanation" rows={2} />
        </label>
        <div className={styles.actions}>
          <Button type="submit" variant="primary" isLoading={isSaving}>
            {editingQuestion ? 'Lưu thay đổi' : 'Thêm vào ngân hàng'}
          </Button>
          {editingQuestion && (
            <Button disabled={isSaving} onClick={onCancelEdit} type="button" variant="outline">
              Hủy chỉnh sửa
            </Button>
          )}
        </div>
      </form>

      {questions.length > 0 && (
        <ul className={styles.questionList}>
          {questions.map((question) => (
            <li key={question.id}>
              <div>
                <strong>{question.content}</strong>
                <small>{question.questionType === 'SINGLE_CHOICE' ? 'Một đáp án' : 'Nhiều đáp án'} · {question.difficulty}{question.skillCode ? ` · ${question.skillCode}` : ''}</small>
              </div>
              <Button
                aria-label={`Sửa câu hỏi ${question.id}`}
                disabled={Boolean(busyAction) || isSaving}
                onClick={() => onEdit(question)}
                variant="outline"
                size="sm"
              >
                Sửa
              </Button>
              <Button
                aria-label={`Ẩn câu hỏi ${question.id}`}
                disabled={Boolean(busyAction) || isSaving}
                onClick={() => onDeactivate(question.id)}
                variant="ghost"
                size="sm"
              >
                Ẩn
              </Button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function ExamSettingsForm({
  isSaving,
  onSubmit,
}: {
  isSaving: boolean
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}) {
  return (
    <form className={styles.form} onSubmit={onSubmit}>
      <h3>Tạo bài kiểm tra</h3>
      <label>
        <span>Tên bài kiểm tra</span>
        <input maxLength={255} name="examTitle" required />
      </label>
      <label>
        <span>Mô tả</span>
        <textarea name="examDescription" rows={2} />
      </label>
      <ExamSettingsFields />
      <Button type="submit" variant="primary" isLoading={isSaving}>Tạo bản nháp</Button>
    </form>
  )
}

function ExamSettingsFields({ exam }: { exam?: TeacherExamBuilder['exam'] }) {
  return (
    <>
      <div className={styles.formGrid}>
        <label>
          <span>Thời lượng (phút)</span>
          <input defaultValue={exam?.durationMinutes ?? 30} min="1" name="durationMinutes" required type="number" />
        </label>
        <label>
          <span>Điểm đạt (0–100)</span>
          <input defaultValue={exam?.passingScore ?? 60} max="100" min="0" name="passingScore" required step="0.1" type="number" />
        </label>
        <label>
          <span>Số lần làm (để trống = không giới hạn)</span>
          <input defaultValue={exam?.maximumAttempts ?? ''} min="1" name="maximumAttempts" type="number" />
        </label>
        <label>
          <span>Cách công bố kết quả</span>
          <select defaultValue={exam?.resultDisplayMode ?? 'IMMEDIATE_SCORE_ONLY'} name="resultDisplayMode">
            {resultModes.map((mode) => <option key={mode.value} value={mode.value}>{mode.label}</option>)}
          </select>
        </label>
      </div>
      <label className={styles.checkboxField}>
        <input defaultChecked={exam?.shuffleQuestions ?? true} name="shuffleQuestions" type="checkbox" />
        <span>Trộn thứ tự câu hỏi</span>
      </label>
      <label className={styles.checkboxField}>
        <input defaultChecked={exam?.shuffleOptions ?? true} name="shuffleOptions" type="checkbox" />
        <span>Trộn thứ tự lựa chọn</span>
      </label>
    </>
  )
}

function ExamBuilderPanel({
  builder,
  busyAction,
  onAddQuestion,
  onClose,
  onLoadStatistics,
  onRemoveQuestion,
  onScoreChange,
  onSubmit,
  onPublish,
  score,
  isSaving,
  questions,
  statistics,
}: {
  builder: TeacherExamBuilder
  busyAction: string
  onAddQuestion: (question: TeacherQuestion) => void
  onClose: () => void
  onLoadStatistics: () => void
  onRemoveQuestion: (id: number) => void
  onScoreChange: (score: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onPublish: () => void
  score: string
  isSaving: boolean
  questions: TeacherQuestion[]
  statistics: TeacherExamStatistics | null
}) {
  const included = new Set(builder.questions.map((item) => item.questionId))
  const available = questions.filter((question) => !included.has(question.id))
  const exam = builder.exam

  return (
    <div className={styles.examWorkspace}>
      <div className={styles.examSummary}>
        <div>
          <span className={styles.statusBadge}>{exam.status === 'DRAFT' ? 'Bản nháp' : exam.status === 'PUBLISHED' ? 'Đã phát hành' : 'Đã đóng'}</span>
          <h3>{exam.title}</h3>
          <p>{exam.questionCount} câu · Tổng {exam.maximumScore} điểm · Đạt {formatScore(exam.passingScore)}/100</p>
        </div>
        <div className={styles.actions}>
          {exam.status === 'DRAFT' && (
            <Button disabled={Boolean(busyAction)} onClick={onPublish} variant="primary">
              {busyAction === 'publish' ? 'Đang phát hành...' : 'Phát hành bài thi'}
            </Button>
          )}
          {exam.status === 'PUBLISHED' && (
            <Button disabled={Boolean(busyAction)} onClick={onClose} variant="outline">
              {busyAction === 'close' ? 'Đang đóng...' : 'Đóng bài thi'}
            </Button>
          )}
          <Button disabled={Boolean(busyAction)} onClick={onLoadStatistics} variant="outline">
            {busyAction === 'statistics' ? 'Đang tải...' : 'Thống kê'}
          </Button>
        </div>
      </div>

      <form className={styles.form} onSubmit={onSubmit}>
        <h4>Cài đặt bài thi</h4>
        <label>
          <span>Tên bài kiểm tra</span>
          <input defaultValue={exam.title} maxLength={255} name="examTitle" required />
        </label>
        <label>
          <span>Mô tả</span>
          <textarea defaultValue={exam.description ?? ''} name="examDescription" rows={2} />
        </label>
        <ExamSettingsFields exam={exam} />
        <Button type="submit" variant="outline" isLoading={isSaving}>Lưu cài đặt</Button>
      </form>

      <section className={styles.subsection} aria-labelledby="included-questions-title">
        <h4 id="included-questions-title">Câu hỏi trong bài thi</h4>
        {builder.questions.length === 0 ? (
          <p className={styles.stateMessage}>Chưa có câu hỏi. Chọn câu từ ngân hàng bên dưới.</p>
        ) : (
          <ol className={styles.questionList}>
            {builder.questions.map((item) => (
              <li key={item.questionId}>
                <div>
                  <strong>{item.position}. {item.question.content}</strong>
                  <small>{item.score} điểm · {item.required ? 'Bắt buộc' : 'Không bắt buộc'}</small>
                </div>
                <Button
                  disabled={Boolean(busyAction) || exam.status === 'CLOSED'}
                  onClick={() => onRemoveQuestion(item.questionId)}
                  variant="ghost"
                  size="sm"
                >
                  Bỏ
                </Button>
              </li>
            ))}
          </ol>
        )}
      </section>

      <section className={styles.subsection} aria-labelledby="available-questions-title">
        <div className={styles.subsectionHeading}>
          <div>
            <h4 id="available-questions-title">Thêm từ ngân hàng câu hỏi</h4>
            <p>Chỉ câu hỏi có đáp án hợp lệ trong cùng khóa học mới được đưa vào đề.</p>
          </div>
          <div className={styles.scoreField}>
            <span>Điểm mỗi câu mới</span>
            <Input label="Điểm mỗi câu mới" min="0.01" onChange={(event) => onScoreChange(event.target.value)} step="0.01" type="number" value={score} />
          </div>
        </div>
        {available.length === 0 ? (
          <p className={styles.stateMessage}>Không còn câu hỏi khả dụng để thêm.</p>
        ) : (
          <ul className={styles.questionList}>
            {available.map((question) => (
              <li key={question.id}>
                <div>
                  <strong>{question.content}</strong>
                  <small>{question.difficulty} · {question.questionType === 'SINGLE_CHOICE' ? 'Một đáp án' : 'Nhiều đáp án'}</small>
                </div>
                <Button
                  disabled={Boolean(busyAction) || exam.status === 'CLOSED'}
                  onClick={() => onAddQuestion(question)}
                  variant="outline"
                  size="sm"
                >
                  {busyAction === 'add-question' ? 'Đang thêm...' : 'Thêm'}
                </Button>
              </li>
            ))}
          </ul>
        )}
      </section>

      {statistics && <StatisticsPanel statistics={statistics} />}
    </div>
  )
}

function StatisticsPanel({ statistics }: { statistics: TeacherExamStatistics }) {
  return (
    <section className={styles.subsection} aria-labelledby="exam-statistics-title">
      <h4 id="exam-statistics-title">Kết quả đã chấm</h4>
      {statistics.attemptCount === 0 ? (
        <p className={styles.stateMessage}>Chưa có lượt thi nào được chấm.</p>
      ) : (
        <>
          <dl className={styles.statistics}>
            <div><dt>Lượt làm</dt><dd>{statistics.attemptCount}</dd></div>
            <div><dt>Học viên</dt><dd>{statistics.studentCount}</dd></div>
            <div><dt>Điểm trung bình</dt><dd>{statistics.averageScore === null ? '—' : formatScore(statistics.averageScore)}</dd></div>
            <div><dt>Tỷ lệ đạt</dt><dd>{statistics.passRate === null ? '—' : `${formatScore(statistics.passRate)}%`}</dd></div>
          </dl>
          <ul className={styles.questionStats}>
            {statistics.questionStats.map((item) => (
              <li key={item.questionId}>
                <span>{item.content}</span>
                <strong>{formatScore(item.correctRate)}% đúng · {item.answeredCount} lượt</strong>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}
