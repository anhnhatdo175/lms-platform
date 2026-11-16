package com.example.lms.ai;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật {@code @Scheduled} cho các worker AI (phiên luyện tập, ingestion, xử lý bù hồ sơ) mà không phụ thuộc cấu hình
 * của module khác. Khai báo lặp {@code @EnableScheduling} là vô hại.
 */
@Configuration
@EnableScheduling
public class AiSchedulingConfig {
}
