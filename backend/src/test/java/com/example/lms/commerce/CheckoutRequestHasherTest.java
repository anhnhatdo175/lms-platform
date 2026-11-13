package com.example.lms.commerce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CheckoutRequestHasherTest {

    @Test
    void sameLogicalRequest_sameHash() {
        assertEquals(CheckoutRequestHasher.hash(" sale10 ", null),
                CheckoutRequestHasher.hash("SALE10", "SEPAY_BANK_TRANSFER"));
        assertEquals(CheckoutRequestHasher.hash(null, null), CheckoutRequestHasher.hash("", "SEPAY_BANK_TRANSFER"));
    }

    @Test
    void differentCoupon_differentHash() {
        assertNotEquals(CheckoutRequestHasher.hash("SALE10", null), CheckoutRequestHasher.hash("SALE20", null));
        assertNotEquals(CheckoutRequestHasher.hash(null, null), CheckoutRequestHasher.hash("SALE10", null));
    }

    @Test
    void hashFitsChar64Column() {
        String hash = CheckoutRequestHasher.hash("SALE10", null);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }
}
