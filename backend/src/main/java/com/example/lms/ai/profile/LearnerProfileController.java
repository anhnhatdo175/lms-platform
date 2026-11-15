package com.example.lms.ai.profile;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms.ai.profile.LearnerProfileViews.ProfileView;
import com.example.lms.ai.profile.LearnerProfileViews.SkillView;
import com.example.lms.contract.identity.CurrentUserProvider;
import com.example.lms.shared.web.ApiResponse;

/** Hồ sơ cá nhân hóa: Student xem của mình, Teacher xem học viên trong Course mình quản lý. */
@RestController
public class LearnerProfileController {

    private final LearnerProfileService service;
    private final CurrentUserProvider currentUser;

    public LearnerProfileController(LearnerProfileService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/student/ai/profile")
    public ApiResponse<ProfileView> myProfile() {
        return ApiResponse.of(service.myProfile(currentUser.current().id()));
    }

    @GetMapping("/student/ai/profile/skills")
    public ApiResponse<List<SkillView>> mySkills() {
        return ApiResponse.of(service.mySkills(currentUser.current().id()));
    }

    @GetMapping("/teacher/ai/students/{studentId}/profile")
    public ApiResponse<ProfileView> studentProfile(@PathVariable long studentId, @RequestParam long courseId) {
        return ApiResponse.of(service.studentProfileForTeacher(currentUser.current(), studentId, courseId));
    }
}
