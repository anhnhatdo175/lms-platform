package com.example.lms.shared.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/** {@code page} bắt đầu từ 0, {@code size} mặc định 20, tối đa 100 (Spec 19.3). */
public final class PageRequests {

    public static final int MAX_SIZE = 100;

    private PageRequests() {
    }

    public static PageRequest of(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page must be >= 0 and size 1-" + MAX_SIZE);
        }
        return PageRequest.of(page, size, sort);
    }
}
