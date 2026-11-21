package com.eldercare.backend.admission.internal.change.service;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.admission.internal.change.domain.ChangeConfirmationRequest;
import com.eldercare.backend.admission.internal.change.domain.RepresentativeConfirmation;
import com.eldercare.backend.admission.internal.change.persistence.ConfirmationVoidStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Các lần gửi người đại diện xác nhận của một yêu cầu thay đổi lưu trú (FR-043c), ở {@code simple_record}. Lần mới nhất là
 * lần có hiệu lực; chỉ lần mới nhất ở Đồng ý mới thay cho xác nhận kèm bằng chứng. Lệnh ghi chạy trong transaction của lệnh
 * gọi; lớp này không tự kiểm quyền.
 */
@Component
class ChangeConfirmations {

    private static final Set<String> OPEN =
            Set.of(ChangeConfirmationRequest.AWAITING, ChangeConfirmationRequest.ACCEPTED);

    private final SimpleRecordApi records;
    private final ConfirmationVoidStore voids;

    ChangeConfirmations(SimpleRecordApi records, ConfirmationVoidStore voids) {
        this.records = records;
        this.voids = voids;
    }

    /**
     * Trả về nháp (Q-418): xác nhận kèm bằng chứng ghi trước thời điểm này hết tác dụng, lần gửi trên ứng dụng đang chờ
     * hoặc đã đồng ý thành Bị thay thế. Nội dung yêu cầu không sửa ở đây (người duyệt không có quyền sửa Nháp).
     */
    void voidAll(long requestId, Instant at) {
        supersede(requestId);
        voids.mark(requestId, at);
    }

    /** Lần bỏ xác nhận gần nhất của yêu cầu. */
    Optional<Instant> voidedAt(long requestId) {
        return voids.latest(requestId);
    }

    /**
     * Xác nhận kèm bằng chứng còn dùng được để gửi duyệt (Q-414, Q-418): có người đại diện đã xác nhận và ghi sau lần
     * Trả về nháp gần nhất.
     */
    Optional<RepresentativeConfirmation> validConfirmation(long requestId, JsonNode content) {
        Optional<Instant> voided = voidedAt(requestId);
        return RepresentativeConfirmation.of(content)
                .filter(RepresentativeConfirmation::names)
                .filter(c -> voided.isEmpty()
                        || (c.recordedAt() != null && c.recordedAt().isAfter(voided.get())));
    }

    private static EntityRef parent(long requestId) {
        return new EntityRef(ChangeConfirmationRequest.PARENT_TYPE, requestId);
    }

    Optional<SimpleRecord<ChangeConfirmationRequest>> latest(long requestId) {
        List<SimpleRecord<ChangeConfirmationRequest>> all =
                records.listByParent(ChangeConfirmationRequest.KIND, parent(requestId));
        return all.isEmpty() ? Optional.empty() : Optional.of(all.getLast());
    }

    boolean accepted(long requestId) {
        return latest(requestId)
                .filter(r -> ChangeConfirmationRequest.ACCEPTED.equals(r.status()))
                .isPresent();
    }

    SimpleRecord<ChangeConfirmationRequest> require(long id) {
        return records.require(ChangeConfirmationRequest.KIND, id);
    }

    SimpleRecord<ChangeConfirmationRequest> lock(long id) {
        return records.lockForUpdate(ChangeConfirmationRequest.KIND, id);
    }

    List<SimpleRecord<ChangeConfirmationRequest>> awaiting() {
        return records.listByStatus(ChangeConfirmationRequest.KIND, Set.of(ChangeConfirmationRequest.AWAITING));
    }

    /** Lần đang chờ hoặc đã đồng ý thành Bị thay thế (sửa nháp, gửi lại). */
    void supersede(long requestId) {
        for (SimpleRecord<ChangeConfirmationRequest> r :
                records.listByParent(ChangeConfirmationRequest.KIND, parent(requestId))) {
            if (OPEN.contains(r.status())) {
                change(r, ChangeConfirmationRequest.SUPERSEDED, r.content());
            }
        }
    }

    SimpleRecord<ChangeConfirmationRequest> send(long requestId, long residentId, long sentBy, Instant at) {
        supersede(requestId);
        return records.create(
                ChangeConfirmationRequest.KIND,
                new NewSimpleRecord<>(
                        residentId,
                        parent(requestId),
                        ChangeConfirmationRequest.AWAITING,
                        ChangeConfirmationRequest.sent(requestId, sentBy, at),
                        null,
                        null,
                        null,
                        sentBy,
                        false));
    }

    SimpleRecord<ChangeConfirmationRequest> change(
            SimpleRecord<ChangeConfirmationRequest> record, String status, ChangeConfirmationRequest content) {
        return records.change(
                        ChangeConfirmationRequest.KIND,
                        record.id(),
                        record.version(),
                        new RecordChange<>(status, content, record.effectiveTo(), record.uniqueKey(), false))
                .after();
    }
}
