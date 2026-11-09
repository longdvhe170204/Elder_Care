package com.eldercare.backend.incident.internal.infection.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.incident.internal.infection.domain.ContactListContent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Danh sách tiếp xúc của sự cố và khoảng truy vết được chốt khi lập. */
@Repository
public class ContactListStore {
    private static final SharedKind<ContactListContent> KIND =
            SharedKind.of("M05.CONTACT_LIST", ContactListContent.class);

    private final SimpleRecordApi records;

    public ContactListStore(SimpleRecordApi records) {
        this.records = records;
    }

    public SimpleRecord<ContactListContent> create(long incidentId, Instant traceFrom, Instant traceTo, long actorId) {
        var parent = new EntityRef("M05.INCIDENT", incidentId);
        var content = new ContactListContent(traceFrom, traceTo, null, null, null);
        return records.create(
                KIND,
                new NewSimpleRecord<>(
                        null, parent, "DRAFT", content, null, null, Long.toString(incidentId), actorId, false));
    }

    public Optional<SimpleRecord<ContactListContent>> findByIncident(long incidentId) {
        return records.findByUniqueKey(KIND, Long.toString(incidentId));
    }

    public SimpleRecord<ContactListContent> require(long id) {
        return records.require(KIND, id);
    }

    public SimpleRecord<ContactListContent> lock(long id) {
        return records.lockForUpdate(KIND, id);
    }

    public SimpleRecord<ContactListContent> confirm(
            long id, int version, Instant at, long actorId, String noContactsReason) {
        var row = records.lockForUpdate(KIND, id);
        if (!"DRAFT".equals(row.status())) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("FR-060", "Chỉ xác nhận danh sách tiếp xúc đang ở trạng thái Nháp")));
        }
        var draft = row.content();
        var content = new ContactListContent(draft.traceFrom(), draft.traceTo(), at, actorId, noContactsReason);
        return change(row, version, "CONFIRMED", content, false);
    }

    public SimpleRecord<ContactListContent> end(long id, int version) {
        var current = records.lockForUpdate(KIND, id);
        return change(current, version, "ENDED", current.content(), true);
    }

    private SimpleRecord<ContactListContent> change(
            SimpleRecord<ContactListContent> row,
            int version,
            String status,
            ContactListContent content,
            boolean lock) {
        return records.change(KIND, row.id(), version, new RecordChange<>(status, content, null, row.uniqueKey(), lock))
                .after();
    }
}
