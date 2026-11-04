package com.eldercare.backend.family.internal.relationship.service;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import com.eldercare.backend.family.internal.relationship.domain.RelationshipAttribute;
import com.eldercare.backend.family.internal.relationship.persistence.FamilyRelationshipStore;
import com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Truy vấn quan hệ dùng chung cho các bản cài SPI của 012 (research F-04): hiện tại đọc cột của quan hệ Hiệu lực; tại một
 * thời điểm thì quan hệ phải Hiệu lực lúc đó và giá trị lấy từ lịch sử (FR-068). {@code NguoiDaiDienPort} và
 * {@code QuanHeNguoiThanApi.laNguoiDaiDienHieuLuc} dùng chung {@link #representativeAt}.
 */
@Service
@Transactional(readOnly = true)
public class RelationshipQueries {

    private final FamilyRelationshipStore store;
    private final RelationshipHistoryStore history;

    RelationshipQueries(FamilyRelationshipStore store, RelationshipHistoryStore history) {
        this.store = store;
        this.history = history;
    }

    public Optional<FamilyMember> familyMember(long familyMemberId) {
        return store.familyMember(familyMemberId);
    }

    public List<FamilyRelationship> activeOfFamilyMember(long familyMemberId) {
        return store.activeOfFamilyMember(familyMemberId);
    }

    /** Theo thứ tự liên hệ (FR-012). */
    public List<FamilyRelationship> activeOfResident(long residentId) {
        return store.activeOfResident(residentId);
    }

    public Optional<FamilyRelationship> active(long familyMemberId, long residentId) {
        return store.activeOfFamilyMember(familyMemberId).stream()
                .filter(r -> r.residentId() == residentId)
                .findFirst();
    }

    public boolean representativeAt(long familyMemberId, long residentId, Instant at) {
        return valueAt(familyMemberId, residentId, RelationshipAttribute.REPRESENTATIVE, at);
    }

    /** Quyền đang bật tại thời điểm (chưa xét bản đồng ý; quyền có tác dụng ở {@code EffectiveRights}). */
    public boolean rightEnabledAt(long familyMemberId, long residentId, FamilyRight right, Instant at) {
        return valueAt(familyMemberId, residentId, RelationshipAttribute.of(right), at);
    }

    private boolean valueAt(long familyMemberId, long residentId, RelationshipAttribute attribute, Instant at) {
        return store.ofPair(familyMemberId, residentId).stream()
                .filter(r -> r.activeAt(at))
                .anyMatch(r -> history.valueAt(r.id(), attribute, at));
    }
}
