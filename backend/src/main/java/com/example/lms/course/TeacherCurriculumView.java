package com.example.lms.course;

import java.util.List;

public record TeacherCurriculumView(List<SectionView> sections) {

    public record SectionView(SectionSummary section, List<LessonDetail> lessons) {
    }
}
