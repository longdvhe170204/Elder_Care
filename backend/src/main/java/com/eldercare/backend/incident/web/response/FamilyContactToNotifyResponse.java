package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.infection.domain.FamilyContactToNotify;
import io.swagger.v3.oas.annotations.media.Schema;

/** Schema {@code FamilyContactToNotify} của api-007 (FR-062, BR-M05-24). */
@Schema(name = "FamilyContactToNotify")
public record FamilyContactToNotifyResponse(String fullName, String relationship, String phoneNumber, String message) {

    public static FamilyContactToNotifyResponse from(FamilyContactToNotify contact) {
        return new FamilyContactToNotifyResponse(
                contact.fullName(), contact.relationship(), contact.phoneNumber(), contact.message());
    }
}
