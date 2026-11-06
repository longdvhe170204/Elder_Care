package com.eldercare.backend.admission.internal.registration.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationContent;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationStatus;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationType;
import com.eldercare.backend.admission.internal.registration.domain.ResidenceRegistration;
import com.eldercare.backend.common.error.NotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc, ghi khai báo ở {@code simple_record} loại {@code M02.RESIDENCE_REGISTRATION} (data-model 018 mục 0.1; R-15). Quy ước
 * tên (plan 018, glossary 24.5): mọi lệnh ghi dòng khai báo mới đặt tên {@code insert…} để {@code ResidenceRegistrationArchTest}
 * bắt được đường đi từ Bộ lập lịch. DBR-33 bằng {@code unique_key}: khai báo Cần khai báo, Đã nộp giữ khóa xử lý, khai báo Đã
 * xác nhận giữ khóa xác nhận, trạng thái khác nhả khóa; bảng chuyển trạng thái kiểm ở {@link #transition}. Trạng thái cuối
 * (Đã xóa, Được thay thế, Đã hủy) khóa dòng.
 */
@Repository
public class ResidenceRegistrationStore {

    private static final Set<RegistrationStatus> IN_PROGRESS =
            EnumSet.of(RegistrationStatus.TO_SUBMIT, RegistrationStatus.SUBMITTED);
    private static final Set<RegistrationStatus> CONFIRMED_OR_EXPIRED =
            EnumSet.of(RegistrationStatus.CONFIRMED, RegistrationStatus.EXPIRED);
    private static final Set<RegistrationStatus> FINAL =
            EnumSet.of(RegistrationStatus.DEREGISTERED, RegistrationStatus.SUPERSEDED, RegistrationStatus.CANCELLED);

    private final SimpleRecordApi records;
    private final RegistrationLockedReads lockedReads;

    ResidenceRegistrationStore(SimpleRecordApi records, RegistrationLockedReads lockedReads) {
        this.records = records;
        this.lockedReads = lockedReads;
    }

    static String processingKey(long residentId) {
        return "M02.REG_PROCESSING:" + residentId;
    }

    static String confirmedKey(long residentId) {
        return "M02.REG_CONFIRMED:" + residentId;
    }

    private static String uniqueKey(RegistrationStatus status, long residentId) {
        if (IN_PROGRESS.contains(status)) {
            return processingKey(residentId);
        }
        return status == RegistrationStatus.CONFIRMED ? confirmedKey(residentId) : null;
    }

    private static Set<String> names(Set<RegistrationStatus> statuses) {
        Set<String> out = new HashSet<>();
        statuses.forEach(s -> out.add(s.name()));
        return out;
    }

    /** Người cao tuổi của khai báo, rỗng nếu không có khai báo {@code id}. */
    public Optional<Long> residentOf(long id) {
        return find(id).map(ResidenceRegistration::residentId);
    }

    /**
     * Ghi khai báo mới ở Cần khai báo (lệnh lập khai báo của Hành chính, Q-306). Khóa xử lý chặn khai báo đang xử lý thứ hai
     * (DBR-33): trùng thì {@code ConditionNotMetException} {@code UNIQUE_KEY} của {@code SimpleRecordApi}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public long insertToSubmit(
            long residentId,
            RegistrationType type,
            boolean renewal,
            Long obligationId,
            Instant dueAt,
            boolean localPermanentResident,
            String basisNote,
            Long replacedRegistrationId,
            long createdBy) {
        RegistrationContent content = new RegistrationContent(
                type,
                renewal,
                obligationId,
                dueAt,
                localPermanentResident,
                basisNote,
                null,
                null,
                null,
                replacedRegistrationId,
                null,
                null,
                null);
        return records.create(
                        RegistrationContent.KIND,
                        new NewSimpleRecord<>(
                                residentId,
                                null,
                                RegistrationStatus.TO_SUBMIT.name(),
                                content,
                                null,
                                null,
                                processingKey(residentId),
                                createdBy,
                                false))
                .id();
    }

    public Optional<ResidenceRegistration> find(long id) {
        try {
            return Optional.of(map(records.require(RegistrationContent.KIND, id)));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    /** Đọc khóa khai báo (bản mới nhất đã commit), dùng sau khi đã khóa người cao tuổi. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ResidenceRegistration> findForUpdate(long id) {
        try {
            return Optional.of(map(records.lockForUpdate(RegistrationContent.KIND, id)));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    /** Khai báo Đã xác nhận của người cao tuổi (tối đa một, DBR-33), đọc khóa. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Long> confirmedOf(long residentId) {
        return lockedReads.ids(RegistrationContent.KIND.code(), "unique_key = ?", confirmedKey(residentId)).stream()
                .findFirst();
    }

    /** Đăng ký tạm trú Đã xác nhận (nhắc gia hạn, chuyển Hết hạn; FR-006). */
    public List<ResidenceRegistration> confirmedTemporaryResidence() {
        return byStatus(Set.of(RegistrationStatus.CONFIRMED)).stream()
                .filter(r -> r.type() == RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION)
                .toList();
    }

    /** Người cao tuổi đang có khai báo Cần khai báo hoặc Đã nộp (FR-004). */
    public Set<Long> residentsWithInProgress() {
        Set<Long> out = new HashSet<>();
        byStatus(IN_PROGRESS).forEach(r -> out.add(r.residentId()));
        return out;
    }

    /** Khai báo của người cao tuổi, mới nhất trước. */
    public List<ResidenceRegistration> ofResident(long residentId) {
        return records.listByResident(RegistrationContent.KIND, residentId, Set.of()).stream()
                .map(ResidenceRegistrationStore::map)
                .sorted(Comparator.comparingLong(ResidenceRegistration::id).reversed())
                .toList();
    }

    /**
     * Có khai báo Cần khai báo hoặc Đã nộp (FR-004). Đọc khóa (bản mới nhất đã commit, không theo ảnh chụp của transaction)
     * vì bên gọi vừa chờ khóa người cao tuổi của lệnh khác.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean hasInProgress(long residentId) {
        return !lockedReads
                .ids(RegistrationContent.KIND.code(), "unique_key = ?", processingKey(residentId))
                .isEmpty();
    }

    /** Xác nhận thường trú cùng xã/phường (FR-001): loại thành Thông báo lưu trú, hạn giữ nguyên; chỉ khi Cần khai báo. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean confirmLocalPermanentResident(long id, String basisNote) {
        return transition(
                id,
                EnumSet.of(RegistrationStatus.TO_SUBMIT),
                RegistrationStatus.TO_SUBMIT,
                c -> new RegistrationContent(
                        RegistrationType.STAY_NOTIFICATION,
                        c.renewal(),
                        c.obligationId(),
                        c.dueAt(),
                        true,
                        basisNote,
                        c.expiryDate(),
                        c.currentSubmissionId(),
                        c.evidenceFileId(),
                        c.replacedRegistrationId(),
                        c.deregisteredAt(),
                        c.deregisteredById(),
                        c.deregistrationEvidenceFileId()));
    }

    /**
     * Gỡ xác nhận thường trú cùng xã/phường bằng đính chính (FR-001): loại tính lại, ghi chú căn cứ bỏ; chỉ khi Cần khai
     * báo.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean removeLocalPermanentResident(long id, RegistrationType type) {
        return transition(
                id,
                EnumSet.of(RegistrationStatus.TO_SUBMIT),
                RegistrationStatus.TO_SUBMIT,
                c -> new RegistrationContent(
                        type,
                        c.renewal(),
                        c.obligationId(),
                        c.dueAt(),
                        false,
                        null,
                        c.expiryDate(),
                        c.currentSubmissionId(),
                        c.evidenceFileId(),
                        c.replacedRegistrationId(),
                        c.deregisteredAt(),
                        c.deregisteredById(),
                        c.deregistrationEvidenceFileId()));
    }

    /** Ghi đã nộp (FR-005): Cần khai báo → Đã nộp, lần nộp hiện hành. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markSubmitted(long id, long submissionId) {
        return transition(
                id,
                EnumSet.of(RegistrationStatus.TO_SUBMIT),
                RegistrationStatus.SUBMITTED,
                c -> new RegistrationContent(
                        c.type(),
                        c.renewal(),
                        c.obligationId(),
                        c.dueAt(),
                        c.localPermanentResident(),
                        c.basisNote(),
                        c.expiryDate(),
                        submissionId,
                        c.evidenceFileId(),
                        c.replacedRegistrationId(),
                        c.deregisteredAt(),
                        c.deregisteredById(),
                        c.deregistrationEvidenceFileId()));
    }

    /** Ghi được chấp nhận (FR-005): Đã nộp → Đã xác nhận, ngày hết hạn, bằng chứng. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markConfirmed(long id, LocalDate expiryDate, long evidenceFileId) {
        return transition(
                id,
                EnumSet.of(RegistrationStatus.SUBMITTED),
                RegistrationStatus.CONFIRMED,
                c -> new RegistrationContent(
                        c.type(),
                        c.renewal(),
                        c.obligationId(),
                        c.dueAt(),
                        c.localPermanentResident(),
                        c.basisNote(),
                        expiryDate,
                        c.currentSubmissionId(),
                        evidenceFileId,
                        c.replacedRegistrationId(),
                        c.deregisteredAt(),
                        c.deregisteredById(),
                        c.deregistrationEvidenceFileId()));
    }

    /** Ghi bị từ chối (FR-005): Đã nộp → Cần khai báo, hạn mới của khai báo; lần nộp hiện hành giữ (đã từ chối). */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markRejected(long id, Instant newDueAt) {
        return transition(
                id,
                EnumSet.of(RegistrationStatus.SUBMITTED),
                RegistrationStatus.TO_SUBMIT,
                c -> new RegistrationContent(
                        c.type(),
                        c.renewal(),
                        c.obligationId(),
                        newDueAt,
                        c.localPermanentResident(),
                        c.basisNote(),
                        c.expiryDate(),
                        c.currentSubmissionId(),
                        c.evidenceFileId(),
                        c.replacedRegistrationId(),
                        c.deregisteredAt(),
                        c.deregisteredById(),
                        c.deregistrationEvidenceFileId()));
    }

    /** Qua ngày hết hạn (FR-006): Đã xác nhận → Hết hạn; {@code false} nếu đã chuyển. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markExpired(long id) {
        return transition(
                id, EnumSet.of(RegistrationStatus.CONFIRMED), RegistrationStatus.EXPIRED, UnaryOperator.identity());
    }

    /** Khai báo Đã xác nhận cũ → Được thay thế (FR-005), trước khi khai báo mới Đã xác nhận, cùng transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markSuperseded(long id) {
        return transition(
                id, EnumSet.of(RegistrationStatus.CONFIRMED), RegistrationStatus.SUPERSEDED, UnaryOperator.identity());
    }

    /** Khai báo Cần khai báo hoặc Đã nộp của người cao tuổi, đọc khóa (DBR-33: tối đa một). */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<ResidenceRegistration> inProgressForUpdate(long residentId) {
        return lockedReads.ids(RegistrationContent.KIND.code(), "unique_key = ?", processingKey(residentId)).stream()
                .map(id -> map(records.lockForUpdate(RegistrationContent.KIND, id)))
                .toList();
    }

    /** Khai báo Đã xác nhận hoặc Hết hạn của người cao tuổi (cần khai báo xóa khi ở trạng thái cuối, FR-008), đọc khóa. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean hasConfirmedOrExpired(long residentId) {
        return !lockedReads
                .ids(
                        RegistrationContent.KIND.code(),
                        "resident_id = ? AND status IN ('CONFIRMED', 'EXPIRED')",
                        residentId)
                .isEmpty();
    }

    /** Khai báo Đã xác nhận hoặc Hết hạn của một người, đọc khóa (một thủ tục khai báo xóa theo người, BL-97 B). */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<ResidenceRegistration> confirmedOrExpiredForUpdate(long residentId) {
        return lockedReads
                .ids(
                        RegistrationContent.KIND.code(),
                        "resident_id = ? AND status IN ('CONFIRMED', 'EXPIRED')",
                        residentId)
                .stream()
                .map(id -> map(records.lockForUpdate(RegistrationContent.KIND, id)))
                .toList();
    }

    /** Mọi khai báo Đã xác nhận hoặc Hết hạn (lọc người ở trạng thái cuối ở service, FR-008). */
    public List<ResidenceRegistration> confirmedOrExpired() {
        return byStatus(CONFIRMED_OR_EXPIRED).stream()
                .sorted(Comparator.comparingLong(ResidenceRegistration::id))
                .toList();
    }

    /** Người cao tuổi chuyển trạng thái cuối (FR-005): Cần khai báo, Đã nộp → Đã hủy; {@code false} nếu đã chuyển. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markCancelled(long id) {
        return transition(id, IN_PROGRESS, RegistrationStatus.CANCELLED, UnaryOperator.identity());
    }

    /** Ghi đã khai báo xóa (FR-008): Đã xác nhận, Hết hạn → Đã xóa; thời điểm, người ghi, bằng chứng đặt một lần. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markDeregistered(long id, Instant at, long byId, long evidenceFileId) {
        return transition(
                id,
                CONFIRMED_OR_EXPIRED,
                RegistrationStatus.DEREGISTERED,
                c -> new RegistrationContent(
                        c.type(),
                        c.renewal(),
                        c.obligationId(),
                        c.dueAt(),
                        c.localPermanentResident(),
                        c.basisNote(),
                        c.expiryDate(),
                        c.currentSubmissionId(),
                        c.evidenceFileId(),
                        c.replacedRegistrationId(),
                        at,
                        byId,
                        evidenceFileId));
    }

    /** Khai báo Cần khai báo mà lần nộp hiện hành đã bị từ chối, theo mã người (cho hạn hiệu lực của nghĩa vụ). */
    List<ResidenceRegistration> toSubmit() {
        return byStatus(Set.of(RegistrationStatus.TO_SUBMIT));
    }

    private List<ResidenceRegistration> byStatus(Set<RegistrationStatus> statuses) {
        return records.listByStatus(RegistrationContent.KIND, names(statuses)).stream()
                .map(ResidenceRegistrationStore::map)
                .toList();
    }

    /** Đổi dòng dưới khóa dòng; {@code false} khi không còn ở một trong các trạng thái {@code from} (bảng chuyển, FR-005). */
    private boolean transition(
            long id, Set<RegistrationStatus> from, RegistrationStatus to, UnaryOperator<RegistrationContent> edit) {
        SimpleRecord<RegistrationContent> row;
        try {
            row = records.lockForUpdate(RegistrationContent.KIND, id);
        } catch (NotFoundException e) {
            return false;
        }
        if (!from.contains(RegistrationStatus.valueOf(row.status()))) {
            return false;
        }
        records.change(
                RegistrationContent.KIND,
                id,
                row.version(),
                new RecordChange<>(
                        to.name(),
                        edit.apply(row.content()),
                        row.effectiveTo(),
                        uniqueKey(to, row.residentId()),
                        FINAL.contains(to)));
        return true;
    }

    private static ResidenceRegistration map(SimpleRecord<RegistrationContent> row) {
        RegistrationContent c = row.content();
        return new ResidenceRegistration(
                row.id(),
                row.residentId(),
                c.type(),
                c.renewal(),
                c.obligationId(),
                c.dueAt(),
                c.localPermanentResident(),
                c.basisNote(),
                RegistrationStatus.valueOf(row.status()),
                c.expiryDate(),
                c.currentSubmissionId(),
                c.evidenceFileId());
    }
}
