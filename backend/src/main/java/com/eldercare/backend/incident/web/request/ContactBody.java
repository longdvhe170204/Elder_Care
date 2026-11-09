package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.incident.internal.infection.domain.ContactGroup;
import com.eldercare.backend.incident.internal.infection.domain.ContactPersonContent;
import com.eldercare.backend.incident.internal.infection.domain.ContactType;
import com.eldercare.backend.incident.internal.infection.domain.NewContact;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Schema {@code Contact} của api-007 (tên component sinh ra {@code InfectionContact} vì {@code Contact} đã có ở hợp đồng khác): thân lệnh thêm người và phần tử của tra cứu, danh sách tiếp xúc (FR-060). */
@Schema(name = "InfectionContact")
public record ContactBody(
        Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ContactType type,
        Long subjectId,
        String fullName,
        String relationship,
        String phoneNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ContactGroup group,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String source,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant contactFrom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant contactTo,

        @Schema(description = "Bắt buộc với nhóm OTHER và khi bổ sung sau xác nhận")
        String reason,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Instant monitoredFrom,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Instant monitoredTo,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Instant excludedAt) {

    public NewContact toCommand() {
        return new NewContact(
                type, subjectId, fullName, relationship, phoneNumber, group, source, contactFrom, contactTo, reason);
    }

    public static ContactBody from(ContactPersonContent c) {
        return from(null, c);
    }

    public static ContactBody from(SimpleRecord<ContactPersonContent> row) {
        return from(row.id(), row.content());
    }

    private static ContactBody from(Long id, ContactPersonContent c) {
        return new ContactBody(
                id,
                c.type(),
                c.subjectId(),
                c.fullName(),
                c.relationship(),
                c.phoneNumber(),
                c.group(),
                c.source(),
                c.contactFrom(),
                c.contactTo(),
                c.addReason(),
                c.monitoredFrom(),
                c.monitoredTo(),
                c.excludedAt());
    }
}
