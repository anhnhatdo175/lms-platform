package com.example.lms.commerce;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms.commerce.dto.AddCartItemRequest;
import com.example.lms.commerce.dto.CartResponse;
import com.example.lms.commerce.dto.CouponPreviewRequest;
import com.example.lms.commerce.dto.CouponPreviewResponse;
import com.example.lms.contract.identity.CurrentUserProvider;
import com.example.lms.shared.web.ApiResponse;

import jakarta.validation.Valid;

/** OpenAPI tag {@code cart}. */
@RestController
public class CartController {

    private final CartService carts;
    private final CurrentUserProvider currentUser;

    public CartController(CartService carts, CurrentUserProvider currentUser) {
        this.carts = carts;
        this.currentUser = currentUser;
    }

    @GetMapping("/cart")
    public ApiResponse<CartResponse> get() {
        return ApiResponse.of(carts.getCart(currentUser.current().id()));
    }

    @PostMapping("/cart/items")
    public ResponseEntity<ApiResponse<CartResponse>> add(@Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(carts.addItem(currentUser.current().id(), request.courseId())));
    }

    @DeleteMapping("/cart/items/{courseId}")
    public ResponseEntity<Void> remove(@PathVariable long courseId) {
        carts.removeItem(currentUser.current().id(), courseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cart/coupon-preview")
    public ApiResponse<CouponPreviewResponse> previewCoupon(@Valid @RequestBody CouponPreviewRequest request) {
        return ApiResponse.of(carts.previewCoupon(currentUser.current().id(), request.couponCode()));
    }
}
