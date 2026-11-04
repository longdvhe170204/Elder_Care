package com.eldercare.backend.incident.internal.infection.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.incident.internal.infection.domain.ContactFollowUpContent;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Repository;

/** Dòng theo dõi người cao tuổi trong danh sách tiếp xúc đã xác nhận. */
@Repository
public class ContactFollowUpStore {
    private static final String ACTIVE = "ACTIVE";
    private static final String REMOVED = "REMOVED";
    private static final SharedKind<ContactFollowUpContent> KIND =
            SharedKind.of("M05.CONTACT_PERSON", ContactFollowUpContent.class);

    private final SimpleRecordApi records;

    public ContactFollowUpStore(SimpleRecordApi records) {
        this.records = records;
    }

    public SimpleRecord<ContactFollowUpContent> add(
            long listId, long residentId, Instant from, Instant to, long actorId) {
        var parent = new EntityRef("M05.CONTACT_LIST", listId);
        var content = new ContactFollowUpContent(from, to, null, null);
        return records.create(
                KIND,
                new NewSimpleRecord<>(
                        residentId, parent, ACTIVE, content, null, null, listId + ":" + residentId, actorId, false));
    }

    public void remove(long id, int version, Instant at, String reason) {
        var row = records.lockForUpdate(KIND, id);
        var content =
                new ContactFollowUpContent(row.content().from(), row.content().to(), at, reason);
        records.change(KIND, id, version, new RecordChange<>(REMOVED, content, null, row.uniqueKey(), true));
    }

    public boolean listedAt(long residentId, Instant at) {
        return records.listByResident(KIND, residentId, Set.of(ACTIVE, REMOVED)).stream()
                .map(SimpleRecord::content)
                .anyMatch(content -> !at.isBefore(content.from())
                        && at.isBefore(content.to())
                        && (content.removedAt() == null || at.isBefore(content.removedAt())));
    }
}
