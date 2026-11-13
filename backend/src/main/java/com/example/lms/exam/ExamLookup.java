package com.example.lms.exam;

import org.springframework.stereotype.Component;

import com.example.lms.contract.course.CourseQueryService;
import com.example.lms.contract.course.CourseQueryService.LessonRef;
import com.example.lms.contract.identity.AccessPolicy;
import com.example.lms.contract.identity.CurrentUser;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/** Truy ngược Exam → Lesson → Course để kiểm tra quyền (Spec 7.2: API con phải truy về Course). */
@Component
public class ExamLookup {

    private final ExamRepository exams;
    private final CourseQueryService courses;
    private final AccessPolicy accessPolicy;

    public ExamLookup(ExamRepository exams, CourseQueryService courses, AccessPolicy accessPolicy) {
        this.exams = exams;
        this.courses = courses;
        this.accessPolicy = accessPolicy;
    }

    public record ExamContext(Exam exam, LessonRef lesson) {

        public long courseId() {
            return lesson.courseId();
        }
    }

    /** Teacher không được phân công Course: 403 dù có role TEACHER (AC-COURSE-01). */
    public ExamContext forManager(CurrentUser user, long examId) {
        ExamContext context = context(examId);
        requireManage(user, context.courseId());
        return context;
    }

    /** Student: Exam còn nháp coi như không tồn tại. */
    public ExamContext forLearner(long examId) {
        ExamContext context = context(examId);
        if (context.exam().getStatus() == ExamStatus.DRAFT) {
            throw BusinessException.notFound("Exam");
        }
        return context;
    }

    public LessonRef lesson(long lessonId) {
        return courses.findLesson(lessonId).orElseThrow(() -> BusinessException.notFound("Lesson"));
    }

    public void requireManage(CurrentUser user, long courseId) {
        if (!accessPolicy.canManageCourse(user.id(), courseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Not assigned to course " + courseId);
        }
    }

    private ExamContext context(long examId) {
        Exam exam = exams.findById(examId).orElseThrow(() -> BusinessException.notFound("Exam"));
        return new ExamContext(exam, lesson(exam.getLessonId()));
    }
}
