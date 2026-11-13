package com.example.lms.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mọi mốc thời gian nghiệp vụ (hạn Order, ân hạn webhook, hết giờ Attempt, hết hạn Enrollment)
 * đọc từ bean này, để test thay bằng đồng hồ cố định thay vì phải chờ thật.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
