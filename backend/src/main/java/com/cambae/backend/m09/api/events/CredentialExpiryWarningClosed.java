package com.cambae.backend.m09.api.events;

import java.time.Instant;

/** Sự kiện 008 mục 1.6; dịch vụ nguồn phát trong transaction, Modulith chuyển sau commit. */
public record CredentialExpiryWarningClosed(long credentialId, Instant occurredAt) {

    public String eventKey() {
        return "CREDENTIAL_EXPIRY_WARNING_CLOSED:" + credentialId + ":" + occurredAt;
    }
}
