package com.example.lms.commerce;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms.commerce.dto.CancelOrderRequest;
import com.example.lms.commerce.dto.CheckoutRequest;
import com.example.lms.commerce.dto.CheckoutResponse;
import com.example.lms.commerce.dto.OrderDetailResponse;
import com.example.lms.commerce.dto.OrderSummaryResponse;
import com.example.lms.commerce.dto.PaymentStatusResponse;
import com.example.lms.contract.identity.CurrentUserProvider;
import com.example.lms.shared.pagination.PageRequests;
import com.example.lms.shared.pagination.PageResponse;
import com.example.lms.shared.web.ApiResponse;

import jakarta.validation.Valid;

/** OpenAPI tag {@code orders}. Base path {@code /api/v1} do cấu hình chung của ứng dụng đặt. */
@RestController
@Validated
public class OrderController {

    private final CheckoutService checkout;
    private final OrderService orders;
    private final CurrentUserProvider currentUser;

    public OrderController(CheckoutService checkout, OrderService orders, CurrentUserProvider currentUser) {
        this.checkout = checkout;
        this.orders = orders;
        this.currentUser = currentUser;
    }

    /** 201 khi tạo mới; 200 khi gửi lại cùng Idempotency-Key và cùng nội dung (Spec 10.7). */
    @PostMapping("/orders/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody(required = false) CheckoutRequest request) {
        CheckoutService.CheckoutOutcome outcome = checkout.checkout(currentUser.current().id(), idempotencyKey,
                request);
        return ResponseEntity.status(outcome.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.of(CheckoutResponse.from(outcome.order(), outcome.payment())));
    }

    @GetMapping("/orders")
    public PageResponse<OrderSummaryResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(orders.listMine(currentUser.current().id(),
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), dto -> dto);
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderDetailResponse> get(@PathVariable long orderId) {
        return ApiResponse.of(orders.get(currentUser.current(), orderId));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<OrderDetailResponse> cancel(@PathVariable long orderId,
            @Valid @RequestBody(required = false) CancelOrderRequest request) {
        return ApiResponse.of(orders.cancel(currentUser.current().id(), orderId));
    }

    @GetMapping("/orders/{orderId}/payment")
    public ApiResponse<PaymentStatusResponse> payment(@PathVariable long orderId) {
        return ApiResponse.of(orders.paymentStatus(currentUser.current(), orderId));
    }

    @GetMapping("/orders/{orderId}/status")
    public ApiResponse<PaymentStatusResponse> status(@PathVariable long orderId) {
        return ApiResponse.of(orders.paymentStatus(currentUser.current(), orderId));
    }
}
