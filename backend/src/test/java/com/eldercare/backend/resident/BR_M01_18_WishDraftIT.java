package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.OpinionSourceType;
import com.eldercare.backend.resident.api.dto.PreferredHospital;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.Warning;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishOpinion;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import com.eldercare.backend.resident.api.events.UnverifiedWishDraftReminder;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeDraftContent;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWish;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWishDraft;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentDraftContent;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentWishDraft;
import com.eldercare.backend.resident.internal.wish.domain.WishStatus;
import com.eldercare.backend.resident.internal.wish.service.UnverifiedWishDraftReminderService;
import com.eldercare.backend.resident.internal.wish.service.WishDraftService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T109, T111, T113, T114 (Q-412, BR-M01-18; FR-023n → FR-023r; US7 #12 → #14): Hành chính lập bản Nháp tự khai của
 * phiếu nguyện vọng khi người cao tuổi còn Đang tiếp nhận; Nháp không có hiệu lực tới khi Bác sĩ, Điều dưỡng kiểm chứng
 * bằng lệnh mang đủ nội dung; Hủy nháp kèm lý do; còn Nháp thì không ghi thẳng phiên bản mới; Hoàn tất tiếp nhận cảnh báo,
 * sau đó nhắc theo CFG-M01-06. Cơ sở dữ liệu giữ tối đa một Nháp mỗi loại và bất biến của phiên bản đã có hiệu lực.
 */
class BR_M01_18_WishDraftIT extends WishTestBase {

    @Autowired
    WishDraftService drafts;

    @Autowired
    UnverifiedWishDraftReminderService draftReminders;

    @Autowired
    List<StatusTransitionParticipant> participants;

    @Autowired
    ApplicationEvents events;

    @Autowired
    TestClockControl clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private long admittingResident() {
        long id = newResident();
        setStatus(id, "ADMISSION_IN_PROGRESS");
        return id;
    }

    private EndOfLifeDraftContent endOfLifeDraft(long residentId) {
        long son = relatedFamilyMember(residentId);
        return new EndOfLifeDraftContent(
                WishChoice.STAY_PALLIATIVE,
                null,
                "Gia đình muốn ở lại viện",
                WishSignerType.PATIENT_REPRESENTATIVE,
                son,
                "Con trai",
                null,
                null,
                null,
                null,
                List.of(new WishOpinion(
                        new OpinionSource(OpinionSourceType.FAMILY_MEMBER, son), "Con gái muốn chuyển viện")),
                null,
                null);
    }

    private EndOfLifeWishDraft officeDraft(long residentId) {
        user.set(office.user());
        return drafts.saveEndOfLife(target(residentId), endOfLifeDraft(residentId))
                .draft();
    }

    @Test
    void US7_12_officeDraft_hasNoEffect_untilVerified() {
        long residentId = admittingResident();

        EndOfLifeWishDraft draft = officeDraft(residentId);

        assertThat(draft.recordedBy()).isEqualTo(office.accountId());
        assertThat(wishes.hasActive(residentId)).isFalse();
        assertThat(wishes.currentView(residentId)).isEmpty();
        assertThat(drafts.endOfLife(target(residentId)).id()).isEqualTo(draft.id());
        // Nháp thứ hai cùng loại: dịch vụ ghi đè Nháp đang có của chính người lập; cơ sở dữ liệu chặn dòng Nháp thứ hai
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO wish (kind, resident_id, respondent_type, respondent_family_member_id, relationship, status,"
                                + " recorded_by, recorded_at) SELECT kind, resident_id, respondent_type,"
                                + " respondent_family_member_id, relationship, status, recorded_by, recorded_at FROM wish"
                                + " WHERE id = ?",
                        draft.id()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uq_wish_draft");
        // Hành chính khác không thấy Nháp; Hành chính không đọc phiên bản Hiệu lực, không kiểm chứng, không hủy
        user.set(users.staff(com.eldercare.backend.common.Role.OFFICE_STAFF).user());
        assertThatThrownBy(() -> drafts.endOfLife(target(residentId))).isInstanceOf(PermissionDeniedException.class);
        user.set(office.user());
        assertThatThrownBy(() -> wishes.get(target(residentId))).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> wishes.verifyDraft(
                        target(residentId), command(residentId).build()))
                .isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> drafts.discardEndOfLife(target(residentId), new Reason("nhầm")))
                .isInstanceOf(PermissionDeniedException.class);
        // Còn Nháp thì Bác sĩ, Điều dưỡng không ghi thẳng phiên bản mới
        assertThat(failure(() -> record(command(residentId).build())).errorCode())
                .isEqualTo("DRAFT_ALREADY_EXISTS");
    }

    @Test
    void US7_13_doctorVerifiesDraft_withFullContent_sameRowBecomesActive() {
        long residentId = admittingResident();
        EndOfLifeWishDraft draft = officeDraft(residentId);

        user.set(doctor.user());
        EndOfLifeWish active = wishes.verifyDraft(
                target(residentId),
                command(residentId).choice(WishChoice.STAY_PALLIATIVE).build());

        assertThat(active.id()).isEqualTo(draft.id());
        assertThat(active.status()).isEqualTo(WishStatus.ACTIVE);
        assertThat(active.recordedBy()).isEqualTo(office.accountId());
        assertThat(active.verifiedBy()).isEqualTo(doctor.accountId());
        assertThat(active.verifiedAt()).isNotNull();
        assertThat(wishes.hasActive(residentId)).isTrue();
        assertThat(wishAuditActions(draft.id())).contains("WISH_DRAFT_CREATED", "WISH_DRAFT_VERIFIED");
        // Phiên bản đã có hiệu lực không sửa được nội dung, kể cả trực tiếp ở cơ sở dữ liệu
        assertThatThrownBy(() -> jdbc.update("UPDATE wish SET note = 'sửa' WHERE id = ?", draft.id()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("immutable");
        // Từ đó Hành chính quay về chỉ biết đã có phiếu
        user.set(office.user());
        assertThat(failure(() -> drafts.endOfLife(target(residentId))).errorCode())
                .isEqualTo("NO_DRAFT");
    }

    @Test
    void US7_14_treatmentDraft_warnsAtCompleteAdmission_remindsUntilDiscarded() {
        long residentId = admittingResident();
        user.set(office.user());
        TreatmentWishDraft draft = drafts.saveTreatment(
                        target(residentId),
                        new TreatmentDraftContent(
                                List.of(new PreferredHospital("Bệnh viện X", "12 Lê Lợi")),
                                null,
                                null,
                                List.of(),
                                null,
                                null,
                                new OpinionSource(OpinionSourceType.RESIDENT, null),
                                List.of(),
                                null))
                .draft();
        assertThat(treatments.hasActive(residentId)).isFalse();

        List<String> codes = m01()
                .warnings(new CommandContext(
                        residentId,
                        ResidentStatus.ADMISSION_IN_PROGRESS,
                        StatusCommand.COMPLETE_ADMISSION,
                        office.user(),
                        Instant.now(),
                        null))
                .stream()
                .map(Warning::code)
                .toList();
        assertThat(codes)
                .contains("UNVERIFIED_TREATMENT_WISH_DRAFT")
                .doesNotContain("UNVERIFIED_END_OF_LIFE_WISH_DRAFT");

        setStatus(residentId, "IN_RESIDENCE");
        draftReminders.startReminding(residentId);
        clock.advanceBy(Duration.ofDays(2));
        draftReminders.run();
        assertThat(events.stream(UnverifiedWishDraftReminder.class).filter(e -> e.residentId() == residentId))
                .hasSize(1);

        assignNurse(nurse, residentId);
        user.set(nurse.user());
        TreatmentWishDraft discarded = drafts.discardTreatment(target(residentId), new Reason("Gia đình khai nhầm"));
        assertThat(discarded.status()).isEqualTo("DISCARDED");
        assertThat(discarded.id()).isEqualTo(draft.id());
        clock.advanceBy(Duration.ofDays(2));
        draftReminders.run();
        assertThat(events.stream(UnverifiedWishDraftReminder.class).filter(e -> e.residentId() == residentId))
                .hasSize(1);
    }

    private StatusTransitionParticipant m01() {
        return participants.stream()
                .filter(p -> p.getClass().getSimpleName().startsWith("M01StatusParticipant"))
                .findFirst()
                .orElseThrow();
    }
}
