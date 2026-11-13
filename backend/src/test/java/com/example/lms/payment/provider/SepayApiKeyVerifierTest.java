package com.example.lms.payment.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SepayApiKeyVerifierTest {

    private final SepayApiKeyVerifier verifier = new SepayApiKeyVerifier("secret-key");

    @Test
    void acceptsMatchingKey_schemeCaseInsensitive() {
        assertTrue(verifier.verify("Apikey secret-key"));
        assertTrue(verifier.verify("APIKEY secret-key"));
        assertTrue(verifier.verify("  Apikey   secret-key  "));
    }

    @Test
    void rejectsWrongKeyOrScheme() {
        assertFalse(verifier.verify("Apikey wrong-key"));
        assertFalse(verifier.verify("Apikey secret-key2"));
        assertFalse(verifier.verify("Bearer secret-key"));
        assertFalse(verifier.verify("secret-key"));
        assertFalse(verifier.verify(""));
        assertFalse(verifier.verify(null));
    }

    @Test
    void blankConfiguredKey_rejectsEverything() {
        SepayApiKeyVerifier unconfigured = new SepayApiKeyVerifier("  ");
        assertFalse(unconfigured.verify("Apikey "));
        assertFalse(unconfigured.verify("Apikey anything"));
    }
}
