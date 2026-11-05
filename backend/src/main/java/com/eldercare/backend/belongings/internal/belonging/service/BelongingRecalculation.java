package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CurrentValue;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.HolderType;
import com.eldercare.backend.belongings.api.events.BelongingStatusChanged;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingAmounts;
import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverRow;
import com.eldercare.backend.belongings.internal.belonging.domain.IntakeGroupMember;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingHandoverStore;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cột suy ra và số lượng của đồ theo bản ghi bàn giao hiện hành (FR-011, FR-011a, FR-012, SC-001; research K-04, K-05):
 * bỏ bản ghi đã Hủy ghi nhận, áp bản sửa mới nhất ({@code CorrectionApi.current}). Đồ không còn bản ghi hiện hành bị
 * loại; số lượng theo {@link BelongingAmounts}.
 */
@Component
class BelongingRecalculation {

    static final String RECORD_TYPE = "M12.BELONGING_HANDOVER";

    private final BelongingStore belongings;
    private final BelongingHandoverStore handovers;
    private final ObjectProvider<CorrectionApi> corrections;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    BelongingRecalculation(
            BelongingStore belongings,
            BelongingHandoverStore handovers,
            ObjectProvider<CorrectionApi> corrections,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.belongings = belongings;
        this.handovers = handovers;
        this.corrections = corrections;
        this.events = events;
        this.time = time;
    }

    /** Bản ghi hiện hành của các đồ theo thứ tự ghi. */
    List<HandoverRow> effective(Collection<Long> belongingIds) {
        List<HandoverRow> raw = handovers.ofBelongings(belongingIds);
        if (raw.isEmpty()) {
            return List.of();
        }
        Map<Long, CurrentValue> current = corrections
                .getObject()
                .current(RECORD_TYPE, raw.stream().map(HandoverRow::id).toList());
        List<HandoverRow> result = new ArrayList<>();
        for (HandoverRow row : raw) {
            CurrentValue value = current.get(row.id());
            if (value != null && value.voided()) {
                continue;
            }
            result.add(
                    value != null && value.corrected() && value.content() instanceof Map<?, ?> content
                            ? amended(row, content)
                            : row);
        }
        return result;
    }

    /** Kiểm cuối mỗi lệnh: tổng các đồ cùng lần tiếp nhận bằng số lượng Tiếp nhận hiện hành (SC-001). */
    @Transactional(propagation = Propagation.MANDATORY)
    void verify(long residentId, long intakeHandoverId) {
        List<IntakeGroupMember> members = belongings.lockIntakeGroup(residentId, intakeHandoverId);
        Map<Long, BigDecimal> amounts = amounts(members, byBelonging(members));
        for (IntakeGroupMember m : members) {
            BigDecimal expected = amounts.get(m.id());
            if (!m.excluded() && (expected == null || m.amount().compareTo(expected) != 0)) {
                throw new IllegalStateException("Số lượng đồ " + m.id() + " lệch bản ghi bàn giao (SC-001)");
            }
        }
    }

    /** Tính lại sau đính chính, trong transaction của đính chính; lệch thì ghi lại và tăng phiên bản. */
    @Transactional(propagation = Propagation.MANDATORY)
    void recalculate(long residentId, long intakeHandoverId) {
        List<IntakeGroupMember> members = belongings.lockIntakeGroup(residentId, intakeHandoverId);
        Map<Long, List<HandoverRow>> rows = byBelonging(members);
        Map<Long, BigDecimal> amounts = amounts(members, rows);
        for (IntakeGroupMember m : members) {
            if (m.excluded()) {
                continue;
            }
            List<HandoverRow> own = rows.get(m.id());
            if (own.isEmpty()) {
                belongings.exclude(m.id());
                continue;
            }
            BigDecimal amount = amounts.get(m.id());
            if (amount.signum() <= 0) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "INVALID_QUANTITY",
                        "Sau đính chính số lượng đồ " + m.id() + " không còn lớn hơn 0 (FR-012)");
            }
            if (m.amount().compareTo(amount) != 0) {
                boolean cash = m.cashAmount() != null;
                belongings.setAmount(m.id(), cash ? null : amount, cash ? amount : null);
            }
            HandoverRow latest = own.getLast();
            BelongingStatus status = latest.targetStatus();
            HolderType holderType = null;
            Long holderId = null;
            Long location = null;
            if (status == BelongingStatus.IN_USE) {
                holderType = HolderType.RESIDENT;
                holderId = residentId;
            } else if (status == BelongingStatus.HELD || status == BelongingStatus.DAMAGED) {
                location = latest.storageLocationId();
                if (latest.recipientType() == GiverType.STAFF) {
                    holderType = HolderType.STAFF;
                    holderId = latest.recipientId();
                }
            }
            long condition = latest.conditionLevelId() == null ? m.conditionLevelId() : latest.conditionLevelId();
            boolean changed = status != m.status()
                    || holderType != m.holderType()
                    || !Objects.equals(holderId, m.holderId())
                    || !Objects.equals(location, m.storageLocationId())
                    || condition != m.conditionLevelId()
                    || !Objects.equals(latest.id(), m.currentHandoverId());
            if (changed) {
                belongings.applyHandover(
                        m.id(), status, holderType, holderId, location, condition, latest.id(), latest.recordedAt());
            }
            if (status != m.status()) {
                events.publishEvent(
                        new BelongingStatusChanged(m.id(), residentId, m.status(), status, latest.id(), time.now()));
            }
        }
    }

    private Map<Long, List<HandoverRow>> byBelonging(List<IntakeGroupMember> members) {
        Map<Long, List<HandoverRow>> rows = new LinkedHashMap<>();
        for (IntakeGroupMember m : members) {
            rows.put(m.id(), new ArrayList<>());
        }
        for (HandoverRow row : effective(rows.keySet())) {
            rows.get(row.belongingId()).add(row);
        }
        return rows;
    }

    private static Map<Long, BigDecimal> amounts(List<IntakeGroupMember> members, Map<Long, List<HandoverRow>> rows) {
        List<BelongingAmounts.Node> nodes = new ArrayList<>();
        for (IntakeGroupMember m : members) {
            List<HandoverRow> own = rows.get(m.id());
            if (!own.isEmpty()) {
                nodes.add(new BelongingAmounts.Node(
                        m.id(), m.originalBelongingId(), own.getFirst().amount()));
            }
        }
        return BelongingAmounts.of(nodes);
    }

    private static HandoverRow amended(HandoverRow row, Map<?, ?> content) {
        BigDecimal quantity = row.quantity();
        BigDecimal cash = row.cashAmount();
        if (content.get("quantity") != null) {
            BigDecimal value = new BigDecimal(content.get("quantity").toString());
            if (cash != null) {
                cash = value;
            } else {
                quantity = value;
            }
        }
        return row.amended(
                quantity,
                cash,
                content.get("conditionLevelId") == null
                        ? row.conditionLevelId()
                        : Long.valueOf(content.get("conditionLevelId").toString()),
                content.get("storageLocationId") == null
                        ? row.storageLocationId()
                        : Long.valueOf(content.get("storageLocationId").toString()),
                content.get("recipientStaffId") == null
                        ? row.recipientId()
                        : Long.valueOf(content.get("recipientStaffId").toString()));
    }
}
