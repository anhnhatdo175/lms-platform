package com.example.lms.common;

import java.util.List;

public record PageResponse<T>(List<T> data, PageMeta meta) {
}
