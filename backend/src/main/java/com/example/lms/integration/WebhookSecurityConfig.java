package com.example.lms.integration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SePay gọi webhook không kèm JWT; xác thực bằng API key ngay trong {@code PaymentWebhookController}. Chuỗi bảo vệ
 * riêng này chỉ khớp đường dẫn webhook và đứng trước chuỗi của A, nên không phải sửa {@code SecurityConfig} của A.
 */
@Configuration
public class WebhookSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain paymentWebhookSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(BApiPathConfig.PREFIX + "/webhooks/payments/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}
