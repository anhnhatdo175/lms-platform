package com.example.lms.commerce;

import java.time.LocalDate;

import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms.commerce.dto.CouponResponse;
import com.example.lms.commerce.dto.CreateCouponRequest;
import com.example.lms.commerce.dto.OrderSummaryResponse;
import com.example.lms.commerce.dto.UpdateCouponRequest;
import com.example.lms.contract.identity.CurrentUserProvider;
import com.example.lms.shared.pagination.PageRequests;
import com.example.lms.shared.pagination.PageResponse;
import com.example.lms.shared.web.ApiResponse;

import jakarta.validation.Valid;

/** {@code /admin/orders} và {@code /admin/coupons*}; chỉ Admin. */
@RestController
public class AdminCommerceController {

    private final OrderService orders;
    private final AdminCouponService coupons;
    private final CurrentUserProvider currentUser;

    public AdminCommerceController(OrderService orders, AdminCouponService coupons, CurrentUserProvider currentUser) {
        this.orders = orders;
        this.coupons = coupons;
        this.currentUser = currentUser;
    }

    /** {@code q} là mã đơn hoặc email người mua; {@code from}/{@code to} là ngày theo giờ Việt Nam. */
    @GetMapping("/admin/orders")
    public PageResponse<OrderSummaryResponse> orders(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return PageResponse.of(orders.searchForAdmin(currentUser.current(), status, q, from, to,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), dto -> dto);
    }

    @GetMapping("/admin/coupons")
    public PageResponse<CouponResponse> coupons(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) CouponStatus status,
            @RequestParam(required = false) String q) {
        return PageResponse.of(coupons.list(currentUser.current(), status, q,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), dto -> dto);
    }

    @PostMapping("/admin/coupons")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(coupons.create(currentUser.current(), request)));
    }

    @GetMapping("/admin/coupons/{couponId}")
    public ApiResponse<CouponResponse> coupon(@PathVariable long couponId) {
        return ApiResponse.of(coupons.get(currentUser.current(), couponId));
    }

    @PatchMapping("/admin/coupons/{couponId}")
    public ApiResponse<CouponResponse> updateCoupon(@PathVariable long couponId,
            @Valid @RequestBody UpdateCouponRequest request) {
        return ApiResponse.of(coupons.update(currentUser.current(), couponId, request));
    }
}
