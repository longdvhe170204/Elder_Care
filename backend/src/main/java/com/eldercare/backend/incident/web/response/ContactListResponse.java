package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.infection.domain.ContactListDetails;
import com.eldercare.backend.incident.web.request.ContactBody;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Schema {@code ContactList} của api-007 (FR-061). */
@Schema(name = "ContactList")
public record ContactListResponse(
        Long id,
        Long incidentId,

        @Schema(allowableValues = {"DRAFT", "CONFIRMED", "ENDED"})
        String status,

        List<ContactBody> contacts,
        Boolean noContacts) {

    public static ContactListResponse from(ContactListDetails details) {
        var list = details.list();
        return new ContactListResponse(
                list.id(),
                details.incidentId(),
                list.status(),
                details.contacts().stream().map(ContactBody::from).toList(),
                list.content().noContactsReason() != null);
    }
}
