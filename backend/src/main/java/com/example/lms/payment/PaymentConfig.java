package com.example.lms.payment;

import java.security.SecureRandom;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.lms.commerce.OrderCodeGenerator;

import com.example.lms.payment.provider.SepayApiKeyVerifier;

/** Cấu hình của phần B: thuộc tính thanh toán, bộ sinh mã, xác thực webhook và các job định kỳ. */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentConfig {

    @Bean
    public PaymentReferenceGenerator paymentReferenceGenerator(PaymentProperties properties) {
        return new PaymentReferenceGenerator(properties.referencePrefix(), new SecureRandom());
    }

    @Bean
    public OrderCodeGenerator orderCodeGenerator() {
        return new OrderCodeGenerator(new SecureRandom());
    }

    @Bean
    public SepayApiKeyVerifier sepayApiKeyVerifier(PaymentProperties properties) {
        return new SepayApiKeyVerifier(properties.sepay().apiKey());
    }
}
