package com.example.lms.learning;

import com.example.lms.common.ErrorResponse;
import com.example.lms.course.LessonPrerequisiteRef;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice(basePackages = "com.example.lms.learning")
public class LearningExceptionHandler {

    @ExceptionHandler(LessonLockedException.class)
    public ResponseEntity<LessonLockedError> handleLessonLocked(LessonLockedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new LessonLockedError(new LessonLockedError.Detail(
                        exception.code().name(),
                        exception.getMessage(),
                        UUID.randomUUID().toString(),
                        new LessonLockedError.Lock(exception.releaseAt(), exception.prerequisite()))));
    }

    public record LessonLockedError(Detail error) {
        public record Detail(String code, String message, String traceId, Lock lock) {
        }

        public record Lock(Instant releaseAt, LessonPrerequisiteRef prerequisite) {
        }
    }
}
