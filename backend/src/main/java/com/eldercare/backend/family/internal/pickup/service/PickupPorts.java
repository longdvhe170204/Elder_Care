package com.eldercare.backend.family.internal.pickup.service;

import com.eldercare.backend.admission.api.dto.PickupRecordView;
import com.eldercare.backend.admission.api.spi.PickupRecordPort;
import com.eldercare.backend.care.api.spi.DayCarePickupPort;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.api.events.FamilyRightChanged;
import com.eldercare.backend.family.internal.pickup.domain.PickupOutcome;
import com.eldercare.backend.family.internal.pickup.domain.PickupRecord;
import com.eldercare.backend.family.internal.pickup.persistence.PickupRecordStore;
import com.eldercare.backend.family.internal.right.domain.SelfReturnTurnedOff;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản cài {@code BanGhiDonPort} (004: Cho tạm vắng, Đi chơi với gia đình, bàn giao khi Kết thúc lưu trú) và
 * {@code BanGhiDonBanTruPort} (005: Điểm danh về, gồm Tự về, Viện đưa về) (012 T020; contracts/module-interfaces.md mục
 * 1.1; research F-07): bản ghi Chưa dùng, đúng người cao tuổi, loại thuộc lệnh nguồn, chưa quá CFG-M10-09; dùng một lần
 * trong transaction của lệnh nguồn. Bỏ người đón, tắt dấu tự về thì bản ghi chưa dùng chuyển Không dùng ngay (FR-017).
 */
@Component
class PickupPorts implements PickupRecordPort, DayCarePickupPort {

    private final PickupRecordStore store;
    private final PickupOutcomes outcomes;

    PickupPorts(PickupRecordStore store, PickupOutcomes outcomes) {
        this.store = store;
        this.outcomes = outcomes;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PickupRecordView> validPickupRecord(long residentId, long pickerId, Instant at) {
        return store.ofResident(residentId).stream()
                .filter(r -> !r.type().dayCare())
                .filter(r -> r.familyMemberId() != null && r.familyMemberId() == pickerId)
                .filter(r -> r.usableAt(at, outcomes.status(r.id())))
                .findFirst()
                .map(r -> new PickupRecordView(r.id(), r.residentId(), r.type().name()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void markUsed(long pickupRecordId, String sourceCommand, long sourceId) {
        outcomes.markUsed(pickupRecordId, sourceCommand, sourceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PickupView> valid(long pickupRecordId, long residentId, Instant at) {
        return store.find(pickupRecordId)
                .filter(r -> r.residentId() == residentId && r.type().dayCare())
                .filter(r -> r.usableAt(at, outcomes.status(r.id())))
                .map(r -> new PickupView(r.id(), r.type().careCheckoutType()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void markUsed(long pickupRecordId, long attendanceRecordId) {
        outcomes.markUsed(pickupRecordId, "DAY_CARE_CHECKOUT", attendanceRecordId);
    }

    /** Tắt "được phép đón" (yêu cầu, kết thúc quan hệ) chạy trong lệnh đó, dưới khóa người cao tuổi. */
    @EventListener
    public void pickupRightChanged(FamilyRightChanged event) {
        if (event.right() == FamilyRight.PICKUP_ALLOWED && !event.value()) {
            outcomes.notUsed(
                    store.withoutOutcome(event.residentId(), event.familyMemberId()),
                    PickupOutcome.PICKER_REMOVED,
                    event.from());
        }
    }

    @EventListener
    public void selfReturnTurnedOff(SelfReturnTurnedOff event) {
        outcomes.notUsed(
                store.selfReturnsWithoutOutcome(event.residentId()), PickupOutcome.SELF_RETURN_OFF, event.at());
    }

    /** Việc theo phút (FR-027). */
    @Transactional
    public int expire(Instant at) {
        return outcomes.notUsed(store.expiredWithoutOutcome(at), PickupOutcome.EXPIRED, at);
    }

    PickupRecord.Status status(long id) {
        return outcomes.status(id);
    }
}
