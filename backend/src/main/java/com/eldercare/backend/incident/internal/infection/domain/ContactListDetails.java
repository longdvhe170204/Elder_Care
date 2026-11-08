package com.eldercare.backend.incident.internal.infection.domain;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import java.util.List;

/** Danh sách tiếp xúc kèm người còn trong danh sách (bỏ các dòng đã bỏ khỏi Nháp). */
public record ContactListDetails(
        SimpleRecord<ContactListContent> list, long incidentId, List<SimpleRecord<ContactPersonContent>> contacts) {
    public ContactListDetails {
        contacts = List.copyOf(contacts);
    }
}
