package com.example.lms.exam.snapshot;

/** Một Option chụp lúc bắt đầu Attempt. {@code correct} chỉ dùng ở server, không trả cho Student trước khi chấm. */
public record OptionSnapshot(long id, String content, boolean correct) {
}
