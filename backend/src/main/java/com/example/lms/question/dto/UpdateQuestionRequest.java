package com.example.lms.question.dto;

import java.util.List;

import com.example.lms.contract.question.Difficulty;
import com.example.lms.contract.question.QuestionType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PATCH: trường vắng mặt = giữ nguyên; {@code skillCode} và {@code explanation} gửi {@code null} nghĩa là xóa. Cần phân
 * biệt "không gửi" với "gửi null" nên theo dõi qua cờ do setter đặt.
 */
public class UpdateQuestionRequest {

    private String content;
    private QuestionType questionType;
    private Difficulty difficulty;
    private String skillCode;
    private boolean skillCodeProvided;
    private String explanation;
    private boolean explanationProvided;
    private List<@Valid @NotNull QuestionOptionRequest> options;
    private Boolean active;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    @Size(max = 150)
    public String getSkillCode() {
        return skillCode;
    }

    public void setSkillCode(String skillCode) {
        this.skillCode = skillCode;
        this.skillCodeProvided = true;
    }

    @JsonIgnore
    public boolean isSkillCodeProvided() {
        return skillCodeProvided;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
        this.explanationProvided = true;
    }

    @JsonIgnore
    public boolean isExplanationProvided() {
        return explanationProvided;
    }

    public List<@Valid @NotNull QuestionOptionRequest> getOptions() {
        return options;
    }

    public void setOptions(List<@Valid @NotNull QuestionOptionRequest> options) {
        this.options = options;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
