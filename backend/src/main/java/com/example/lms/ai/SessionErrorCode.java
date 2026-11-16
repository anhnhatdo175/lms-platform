package com.example.lms.ai;

/** Giá trị khớp ENUM trong migration V20261110_1010 (ERD Mục 17). */
public enum SessionErrorCode {
    AI_NOT_CONFIGURED,
    NO_CANDIDATES,
    INSUFFICIENT_EVIDENCE,
    LLM_INVALID_OUTPUT,
    AI_UNAVAILABLE,
    INTERNAL_ERROR
}
