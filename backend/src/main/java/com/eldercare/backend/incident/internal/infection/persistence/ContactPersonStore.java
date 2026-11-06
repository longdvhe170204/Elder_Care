package com.eldercare.backend.incident.internal.infection.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.incident.internal.infection.domain.ContactPersonContent;
import com.eldercare.backend.incident.internal.infection.domain.ContactType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

/**
 * Người trong danh sách tiếp xúc trên hồ sơ chung (R-15, {@code M05.CONTACT_PERSON}). Trạng thái dòng: {@code DRAFT}
 * (danh sách Nháp), {@code REMOVED} (bỏ khỏi Nháp), {@code ACTIVE} (đã xác nhận), {@code EXCLUDED} (loại sau xác nhận);
 * hai trạng thái cuối khóa dòng và nhả khóa duy nhất {@code danhSach:loai:doiTuong}.
 */
@Repository
public class ContactPersonStore {
    public static final String DRAFT = "DRAFT";
    public static final String REMOVED = "REMOVED";
    public static final String ACTIVE = "ACTIVE";
    public static final String EXCLUDED = "EXCLUDED";
    private static final SharedKind<ContactPersonContent> KIND =
            SharedKind.of("M05.CONTACT_PERSON", ContactPersonContent.class);

    private final SimpleRecordApi records;

    public ContactPersonStore(SimpleRecordApi records) {
        this.records = records;
    }

    public SimpleRecord<ContactPersonContent> add(
            long listId, ContactPersonContent content, String status, long actorId) {
        var parent = new EntityRef("M05.CONTACT_LIST", listId);
        Long residentId = content.type() == ContactType.RESIDENT ? content.subjectId() : null;
        return records.create(
                KIND,
                new NewSimpleRecord<>(
                        residentId,
                        parent,
                        status,
                        content,
                        null,
                        null,
                        uniqueKey(listId, content.type(), content.subjectId()),
                        actorId,
                        false));
    }

    /** Dòng còn hiệu lực (Nháp hoặc đã xác nhận) của cùng đối tượng trong danh sách. */
    public Optional<SimpleRecord<ContactPersonContent>> current(long listId, ContactType type, Long subjectId) {
        String key = uniqueKey(listId, type, subjectId);
        return key == null ? Optional.empty() : records.findByUniqueKey(KIND, key);
    }

    public List<SimpleRecord<ContactPersonContent>> byList(long listId) {
        return records.listByParent(KIND, new EntityRef("M05.CONTACT_LIST", listId));
    }

    public SimpleRecord<ContactPersonContent> lock(long id) {
        return records.lockForUpdate(KIND, id);
    }

    /** Đổi trạng thái; {@code REMOVED}, {@code EXCLUDED} là cuối: khóa dòng, nhả khóa duy nhất để thêm lại được. */
    public SimpleRecord<ContactPersonContent> change(
            SimpleRecord<ContactPersonContent> row, String status, ContactPersonContent content) {
        boolean last = REMOVED.equals(status) || EXCLUDED.equals(status);
        return records.change(
                        KIND,
                        row.id(),
                        row.version(),
                        new RecordChange<>(status, content, null, last ? null : row.uniqueKey(), last))
                .after();
    }

    /** Dấu "tiếp xúc" (data-model mục 10): khoảng theo dõi chưa bị loại, các khoảng trùng tự gộp khi đọc. */
    public boolean listedAt(long residentId, Instant at) {
        return monitored(residentId).stream().anyMatch(row -> row.content().monitoredAt(at));
    }

    /** Dòng người cao tuổi đã xác nhận (kể cả đã loại, để đọc lịch sử dấu). */
    public List<SimpleRecord<ContactPersonContent>> monitored(long residentId) {
        return records.listByResident(KIND, residentId, Set.of(ACTIVE, EXCLUDED));
    }

    private static String uniqueKey(long listId, ContactType type, Long subjectId) {
        return subjectId == null ? null : listId + ":" + type + ":" + subjectId;
    }
}
