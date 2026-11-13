package com.example.lms.integration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Tiền tố {@code /api/v1} của OpenAPI cho controller của B. Controller của A tự ghi {@code /api/...} nên không
 * bị ảnh hưởng. Khi nhóm chốt một tiền tố chung, chỉ cần sửa {@link #PREFIX}.
 */
@Configuration
public class BApiPathConfig implements WebMvcConfigurer {

    public static final String PREFIX = "/api/v1";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(PREFIX, HandlerTypePredicate.forBasePackage("com.example.lms.commerce",
                "com.example.lms.payment", "com.example.lms.enrollment", "com.example.lms.exam"));
    }
}
