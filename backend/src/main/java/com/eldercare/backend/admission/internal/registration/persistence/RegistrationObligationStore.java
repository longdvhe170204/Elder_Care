package com.eldercare.backend.admission.internal.registration.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.admission.internal.registration.domain.ObligationContent;
import com.eldercare.backend.admission.internal.registration.domain.ObligationTrigger;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationObligation;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationStatus;
import com.eldercare.backend.admission.internal.registration.domain.ResidenceRegistration;
import com.eldercare.backend.admission.internal.registration.domain.SubmissionResult;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghĩa vụ khai báo ở {@code simple_record} loại {@code M02.REGISTRATION_OBLIGATION} (data-model 018 mục 0.1; research Z-03;
 * R-15): mỗi mốc phát sinh một dòng, hạn chốt lúc ghi. Không phải bản ghi nghiệp vụ, không tạo khai báo (Q-306). Đang mở
 * ({@code OPEN}) hay Đã hủy ({@code CANCELLED}, khóa dòng) ở cột {@code status}; khóa duy nhất theo (người, loại mốc,
 * nguồn). Thời điểm lưu UTC (DBR-25).
 */
@Repository
public class RegistrationObligationStore {

    static final String OPEN = "OPEN";
    static final String CANCELLED = "CANCELLED";
    private static final String SOURCE = "M01.RESIDENT";

    private final SimpleRecordApi records;
    private final RegistrationLockedReads lockedReads;
    private final ResidenceRegistrationStore registrations;
    private final RegistrationSubmissionStore submissions;

    RegistrationObligationStore(
            SimpleRecordApi records,
            RegistrationLockedReads lockedReads,
            ResidenceRegistrationStore registrations,
            RegistrationSubmissionStore submissions) {
        this.records = records;
        this.lockedReads = lockedReads;
        this.registrations = registrations;
        this.submissions = submissions;
    }

    private static String key(long residentId, ObligationTrigger trigger, long sourceId) {
        return "M02.REG_OBLIGATION:" + residentId + ":" + trigger.name() + ":" + sourceId;
    }

    /**
     * Ghi mốc; {@code false} nếu mốc {@code (người, loại mốc, nguồn)} đã có (sự kiện giao lại, NFR-05).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean record(long residentId, ObligationTrigger trigger, Instant triggerAt, Instant dueAt, long sourceId) {
        String key = key(residentId, trigger, sourceId);
        if (records.findByUniqueKey(ObligationContent.KIND, key).isPresent()) {
            return false;
        }
        try {
            records.create(
                    ObligationContent.KIND,
                    new NewSimpleRecord<>(
                            residentId,
                            new EntityRef(SOURCE, residentId),
                            OPEN,
                            new ObligationContent(trigger, triggerAt, dueAt, sourceId, null, false, null, null),
                            null,
                            null,
                            key,
                            0,
                            false));
            return true;
        } catch (ConditionNotMetException e) {
            return false;
        }
    }

    /** Nghĩa vụ chưa có khai báo đáp ứng, chưa hủy, hạn sớm nhất trước; đọc khóa dưới khóa của người cao tuổi. */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<RegistrationObligation> openOf(long residentId) {
        return lockedReads
                .ids(ObligationContent.KIND.code(), "resident_id = ? AND status = 'OPEN'", residentId)
                .stream()
                .map(id -> records.lockForUpdate(ObligationContent.KIND, id))
                .filter(r -> r.content().registrationId() == null)
                .map(r -> map(r, r.content().dueAt()))
                .sorted(Comparator.comparing(RegistrationObligation::dueAt)
                        .thenComparingLong(RegistrationObligation::id))
                .toList();
    }

    /** Gắn khai báo vừa lập vào nghĩa vụ. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markMet(long id, long registrationId) {
        change(
                id,
                c -> c.registrationId() == null,
                c -> new ObligationContent(
                        c.trigger(),
                        c.triggerAt(),
                        c.dueAt(),
                        c.sourceId(),
                        registrationId,
                        c.submitted(),
                        c.managerNotifiedAt(),
                        c.cancelledAt()));
    }

    /** Nghĩa vụ {@code id}, kèm hạn đang có hiệu lực. */
    public Optional<RegistrationObligation> find(long id) {
        try {
            SimpleRecord<ObligationContent> row = records.require(ObligationContent.KIND, id);
            return Optional.of(map(row, effectiveDues().apply(row)));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Nghĩa vụ chưa hủy: chưa lập khai báo, hoặc đã lập mà chưa ghi nộp, hoặc khai báo bị từ chối chờ nộp lại (danh sách
     * cần làm, FR-001, FR-002).
     */
    public List<RegistrationObligation> pending() {
        var due = effectiveDues();
        return records.listByStatus(ObligationContent.KIND, Set.of(OPEN)).stream()
                .filter(r -> !r.content().submitted())
                .map(r -> map(r, due.apply(r)))
                .sorted(Comparator.comparing(RegistrationObligation::effectiveDueAt)
                        .thenComparingLong(RegistrationObligation::id))
                .toList();
    }

    /** Nghĩa vụ chưa hủy, chưa ghi nộp, hạn đang có hiệu lực trước {@code before} (quá hạn, FR-002). */
    public List<RegistrationObligation> overdue(Instant before) {
        return pending().stream()
                .filter(o -> o.effectiveDueAt().isBefore(before))
                .sorted(Comparator.comparingLong(RegistrationObligation::residentId)
                        .thenComparing(RegistrationObligation::effectiveDueAt)
                        .thenComparingLong(RegistrationObligation::id))
                .toList();
    }

    /**
     * Đặt cờ đã ghi nộp của mọi nghĩa vụ mà khai báo {@code registrationId} của người {@code residentId} đáp ứng: {@code true}
     * khi ghi đã nộp (dừng nhắc quá hạn), {@code false} khi lần nộp bị từ chối (không còn lần nộp đang chờ). Hạn gốc, người
     * đã báo Quản lý viện không đổi.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markSubmitted(long residentId, long registrationId, boolean submitted) {
        for (SimpleRecord<ObligationContent> row :
                records.listByResident(ObligationContent.KIND, residentId, Set.of(OPEN))) {
            if (Long.valueOf(registrationId).equals(row.content().registrationId())) {
                change(
                        row.id(),
                        c -> true,
                        c -> new ObligationContent(
                                c.trigger(),
                                c.triggerAt(),
                                c.dueAt(),
                                c.sourceId(),
                                c.registrationId(),
                                submitted,
                                c.managerNotifiedAt(),
                                c.cancelledAt()));
            }
        }
    }

    /**
     * Hủy nghĩa vụ đang mở khi người cao tuổi sang trạng thái cuối (research Z-05): nghĩa vụ chưa có khai báo đáp ứng hoặc
     * có khai báo đáp ứng đã Đã hủy; dòng giữ lại ({@code cancelledAt}), không xóa. Gọi sau khi hủy khai báo đang xử lý.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int cancelOpen(long residentId, Instant at) {
        int cancelled = 0;
        for (SimpleRecord<ObligationContent> row :
                records.listByResident(ObligationContent.KIND, residentId, Set.of(OPEN))) {
            Long registrationId = row.content().registrationId();
            boolean closeable = registrationId == null
                    || registrations
                            .find(registrationId)
                            .map(r -> r.status() == RegistrationStatus.CANCELLED)
                            .orElse(false);
            if (closeable) {
                SimpleRecord<ObligationContent> locked = records.lockForUpdate(ObligationContent.KIND, row.id());
                ObligationContent c = locked.content();
                records.change(
                        ObligationContent.KIND,
                        row.id(),
                        locked.version(),
                        new RecordChange<>(
                                CANCELLED,
                                new ObligationContent(
                                        c.trigger(),
                                        c.triggerAt(),
                                        c.dueAt(),
                                        c.sourceId(),
                                        c.registrationId(),
                                        c.submitted(),
                                        c.managerNotifiedAt(),
                                        at),
                                locked.effectiveTo(),
                                locked.uniqueKey(),
                                true));
                cancelled++;
            }
        }
        return cancelled;
    }

    /** Đánh dấu đã báo Quản lý viện; {@code false} nếu đã báo trước đó (một lần, FR-002). */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markManagerNotified(long id, Instant at) {
        return change(
                id,
                c -> c.managerNotifiedAt() == null,
                c -> new ObligationContent(
                        c.trigger(),
                        c.triggerAt(),
                        c.dueAt(),
                        c.sourceId(),
                        c.registrationId(),
                        c.submitted(),
                        at,
                        c.cancelledAt()));
    }

    /** Đổi nội dung dòng đang mở dưới khóa dòng; {@code false} khi dòng không có, không còn mở hoặc không thỏa điều kiện. */
    private boolean change(
            long id, java.util.function.Predicate<ObligationContent> when, UnaryOperator<ObligationContent> edit) {
        SimpleRecord<ObligationContent> row;
        try {
            row = records.lockForUpdate(ObligationContent.KIND, id);
        } catch (NotFoundException e) {
            return false;
        }
        if (!OPEN.equals(row.status()) || !when.test(row.content())) {
            return false;
        }
        records.change(
                ObligationContent.KIND,
                id,
                row.version(),
                new RecordChange<>(OPEN, edit.apply(row.content()), row.effectiveTo(), row.uniqueKey(), false));
        return true;
    }

    /**
     * Hạn đang có hiệu lực theo trạng thái (quyết định Phase 4, chủ dự án chốt 2026-10-27): khai báo của nghĩa vụ đã bị từ
     * chối và đang chờ nộp lại (Cần khai báo, lần nộp hiện hành {@code REJECTED}) thì hạn mới của khai báo (lúc ghi từ chối
     * + CFG-M02-12, FR-005); chưa lập khai báo, hoặc khai báo chưa từng bị từ chối, thì hạn của nghĩa vụ (FR-002). Không
     * lấy hạn khai báo khi chỉ vì có khai báo: hạn của khai báo là hạn nghĩa vụ sớm nhất lúc lập, khác hạn từng nghĩa vụ.
     */
    private java.util.function.Function<SimpleRecord<ObligationContent>, Instant> effectiveDues() {
        Map<Long, ResidenceRegistration> toSubmit = new HashMap<>();
        registrations.toSubmit().forEach(r -> toSubmit.put(r.id(), r));
        return row -> {
            Long registrationId = row.content().registrationId();
            ResidenceRegistration registration = registrationId == null ? null : toSubmit.get(registrationId);
            if (registration != null
                    && registration.currentSubmissionId() != null
                    && submissions
                            .find(registration.currentSubmissionId())
                            .map(s -> s.result() == SubmissionResult.REJECTED)
                            .orElse(false)) {
                return registration.dueAt();
            }
            return row.content().dueAt();
        };
    }

    private static RegistrationObligation map(SimpleRecord<ObligationContent> row, Instant effectiveDueAt) {
        ObligationContent c = row.content();
        return new RegistrationObligation(
                row.id(),
                row.residentId(),
                c.trigger(),
                c.triggerAt(),
                c.dueAt(),
                c.sourceId(),
                c.registrationId(),
                c.submitted(),
                c.managerNotifiedAt(),
                effectiveDueAt);
    }
}
