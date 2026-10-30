package com.eldercare.backend.notification.internal.governance.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.notification.internal.directcontact.domain.RecordedContactResult;
import com.eldercare.backend.notification.internal.directcontact.persistence.DirectContactStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Đính chính kết quả liên hệ trực tiếp ({@code M13.KET_QUA_LIEN_HE}; FR-021, FR-031, data-model mục 9): kết quả nhóm 3
 * không sửa, chỉ đính chính qua 000; người ghi là Hành chính; nội dung đúng có {@code result} và lý do khi Không liên hệ
 * được.
 */
@Component
class DirectContactResultCorrection implements CorrectableRecordProvider {

    static final String TYPE = "M13.DIRECT_CONTACT_RESULT";
    private static final Set<String> RESULTS = Set.of("CONTACTED", "UNREACHABLE");

    private final DirectContactStore store;

    DirectContactResultCorrection(DirectContactStore store) {
        this.store = store;
    }

    @Override
    public String recordType() {
        return TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return store.result(id).isPresent();
    }

    @Override
    public long originalRecorder(long id) {
        return store.result(id).orElseThrow().contactedBy();
    }

    /** Liên hệ trực tiếp do Hành chính làm cho toàn viện, không gắn tầng. */
    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.empty();
    }

    /** Người cao tuổi của yêu cầu thông báo mà dòng liên hệ trực tiếp thuộc về. */
    @Override
    public Optional<Long> residentId(long id) {
        return store.result(id).map(RecordedContactResult::residentId);
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().contains(Role.OFFICE_STAFF);
    }

    @Override
    public void validateContent(Object content) {
        if (!(content instanceof Map<?, ?> map)
                || !(map.get("result") instanceof String result)
                || !RESULTS.contains(result)) {
            throw new InvalidValueException("Nội dung đúng cần trường result (CONTACTED, UNREACHABLE)");
        }
        if (result.equals("UNREACHABLE") && !(map.get("reason") instanceof String reason && !reason.isBlank())) {
            throw new InvalidValueException("Không liên hệ được cần lý do");
        }
    }

    @Override
    public Object originalContent(long id) {
        RecordedContactResult r = store.result(id).orElseThrow();
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("result", r.content().result());
        content.put("reason", r.content().reason());
        content.put("note", r.content().note());
        content.put("occurredAt", r.content().occurredAt());
        return content;
    }
}
