package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.error.VersionMismatchException;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m01.api.HealthRecordApi;
import com.cambae.backend.m01.api.events.InitialHealthRecordChanged;
import com.cambae.backend.m01.internal.health.domain.InitialHealthRecordContent;
import com.cambae.backend.m01.internal.health.service.InitialHealthRecordService;
import com.cambae.backend.m01.internal.health.service.InitialHealthRecordService.RecordView;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CorrectionResult;
import com.cambae.backend.m15.correction.api.CorrectionType;
import com.cambae.backend.m15.correction.api.CreationMethod;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T026 (US2; FR-017, FR-049a; research H-08): hồ sơ sức khỏe ban đầu. Bác sĩ hoặc điều dưỡng lập và sửa bản nháp
 * (khóa lạc quan {@code If-Match}); chỉ Bác sĩ xác nhận; sau xác nhận trigger chặn sửa và chỉ đính chính qua 000, phát
 * {@code InitialHealthRecordChanged} cho 006; trên hồ sơ trạng thái cuối không còn cách đính chính A, B.
 */
class InitialHealthRecordIT extends HealthTestBase {

    @Autowired
    InitialHealthRecordService records;

    @Autowired
    HealthRecordApi api;

    @Autowired
    CorrectionApi corrections;

    @Autowired
    ApplicationEvents events;

    private static InitialHealthRecordContent content(String medications) {
        return new InitialHealthRecordContent(
                "O+",
                "Đi lại cần gậy",
                "Tự vệ sinh được",
                "Tỉnh táo",
                "Cần theo dõi huyết áp",
                null,
                medications,
                null);
    }

    private static String etag(RecordView view) {
        return VersionEtag.of(view.version());
    }

    private RecordView draftByNurse(long residentId, String medications) {
        assignNurse(nurse, residentId);
        user.set(nurse.user());
        return records.saveDraft(target(residentId), null, content(medications));
    }

    @Test
    void FR_017_nurseCreatesDraft_andDoctorCanEditItWithIfMatch() {
        long resident = newResident();
        RecordView draft = draftByNurse(resident, "Amlodipine 5mg");

        assertThat(draft.confirmed()).isFalse();
        assertThat(draft.content().bloodType()).isEqualTo("O+");
        assertThat(draft.content().currentMedications()).isEqualTo("Amlodipine 5mg");
        assertThat(records.get(target(resident)))
                .get()
                .extracting(RecordView::id)
                .isEqualTo(draft.id());

        user.set(doctor.user());
        RecordView edited = records.saveDraft(target(resident), etag(draft), content("Amlodipine 10mg"));

        assertThat(edited.id()).isEqualTo(draft.id());
        assertThat(edited.content().currentMedications()).isEqualTo("Amlodipine 10mg");
        assertThat(edited.version()).isGreaterThan(draft.version());
        assertThat(audit("INITIAL_HEALTH_RECORD_CREATED", draft.id())).isEqualTo(1);
        assertThat(audit("INITIAL_HEALTH_RECORD_DRAFT_EDITED", draft.id())).isEqualTo(1);
    }

    @Test
    void FR_017_editingDraftWithStaleOrMissingIfMatch_isRejected() {
        long resident = newResident();
        RecordView draft = draftByNurse(resident, "A");
        records.saveDraft(target(resident), etag(draft), content("B"));

        assertThatThrownBy(() -> records.saveDraft(target(resident), etag(draft), content("C")))
                .isInstanceOf(VersionMismatchException.class);
        assertThatThrownBy(() -> records.saveDraft(target(resident), null, content("C")))
                .isInstanceOf(VersionMismatchException.class);
    }

    @Test
    void FR_017_nurseOutOfAssignment_cannotWrite() {
        long resident = newResident();
        user.set(nurse.user());

        assertThatThrownBy(() -> records.saveDraft(target(resident), null, content("A")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void FR_017_onlyDoctorConfirms_afterwardNoDraftEdit() {
        long resident = newResident();
        RecordView draft = draftByNurse(resident, "Metformin 500mg");

        assertThatThrownBy(() -> records.confirm(target(resident), etag(draft)))
                .isInstanceOf(PermissionDeniedException.class);

        user.set(doctor.user());
        RecordView confirmed = records.confirm(target(resident), etag(draft));

        assertThat(confirmed.confirmed()).isTrue();
        assertThat(confirmed.confirmedBy()).isEqualTo("#" + doctor.accountId());
        assertThat(audit("INITIAL_HEALTH_RECORD_CONFIRMED", draft.id())).isEqualTo(1);
        assertThatThrownBy(() -> records.saveDraft(target(resident), etag(confirmed), content("Khác")))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("ALREADY_CONFIRMED"));
        assertThatThrownBy(() -> records.confirm(target(resident), etag(confirmed)))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("ALREADY_CONFIRMED"));
    }

    @Test
    void FR_017_confirmedRecord_isBlockedByTriggerEvenAgainstDirectUpdate() {
        long resident = newResident();
        RecordView draft = draftByNurse(resident, "Metformin 500mg");
        user.set(doctor.user());
        records.confirm(target(resident), etag(draft));

        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE initial_health_record SET current_medications = 'Đổi lén' WHERE id = ?", draft.id()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("confirmed initial_health_record");
    }

    @Test
    void FR_017_confirmWithoutRecord_isNotFound() {
        long resident = newResident();
        user.set(doctor.user());

        assertThatThrownBy(() -> records.confirm(target(resident), "\"0\"")).isInstanceOf(NotFoundException.class);
        assertThat(records.get(target(resident))).isEmpty();
    }

    @Test
    void FR_017_healthRecordApi_givesMedicationsAtAdmission() {
        long resident = newResident();
        RecordView draft = draftByNurse(resident, "Amlodipine 5mg");

        assertThat(api.initial(resident)).get().satisfies(v -> {
            assertThat(v.currentMedications()).isEqualTo("Amlodipine 5mg");
            assertThat(v.confirmed()).isFalse();
            assertThat(v.id()).isEqualTo(draft.id());
        });
        assertThat(api.initial(newResident())).isEmpty();
    }

    @Test
    void FR_017_correctionByOriginalAuthor_changesCurrentValueAndNotifies() {
        long resident = newResident();
        RecordView draft = draftByNurse(resident, "Amlodipine 5mg");
        user.set(doctor.user());
        records.confirm(target(resident), etag(draft));

        user.set(nurse.user());
        CorrectionResult result = corrections.create(
                InitialHealthRecordService.RECORD_TYPE,
                draft.id(),
                CorrectionType.AMEND_CONTENT,
                Map.of("currentMedications", "Amlodipine 10mg"),
                new Reason("Ghi sai liều"));

        assertThat(result)
                .isInstanceOfSatisfying(
                        CorrectionResult.Saved.class,
                        saved -> assertThat(saved.correction().creationMethod()).isEqualTo(CreationMethod.A));
        RecordView current = records.get(target(resident)).orElseThrow();
        assertThat(current.corrected()).isTrue();
        assertThat(current.content().currentMedications()).isEqualTo("Amlodipine 10mg");
        assertThat(api.initial(resident).orElseThrow().currentMedications()).isEqualTo("Amlodipine 10mg");
        assertThat(events.stream(InitialHealthRecordChanged.class)).anyMatch(e -> e.residentId() == resident);
    }

    @Test
    void FR_017_correctionOfDraftOrWithUnknownField_isRejected() {
        long resident = newResident();
        RecordView draft = draftByNurse(resident, "A");

        assertThatThrownBy(() -> corrections.create(
                        InitialHealthRecordService.RECORD_TYPE,
                        draft.id(),
                        CorrectionType.AMEND_CONTENT,
                        Map.of("currentMedications", "B"),
                        new Reason("Thử")))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("RECORD_NOT_CONFIRMED"));

        user.set(doctor.user());
        records.confirm(target(resident), etag(draft));
        user.set(nurse.user());
        assertThatThrownBy(() -> corrections.create(
                        InitialHealthRecordService.RECORD_TYPE,
                        draft.id(),
                        CorrectionType.AMEND_CONTENT,
                        Map.of("tenTruongLa", "B"),
                        new Reason("Thử")))
                .isInstanceOf(com.cambae.backend.common.error.InvalidValueException.class);
    }

    @Test
    void FR_049a_onClosedProfile_authorCorrectionIsNotAllowed() {
        long resident = newResident();
        RecordView draft = draftByNurse(resident, "A");
        user.set(doctor.user());
        records.confirm(target(resident), etag(draft));
        setStatus(resident, "RESIDENCY_ENDED");

        user.set(nurse.user());
        assertThatThrownBy(() -> corrections.create(
                        InitialHealthRecordService.RECORD_TYPE,
                        draft.id(),
                        CorrectionType.AMEND_CONTENT,
                        Map.of("currentMedications", "B"),
                        new Reason("Thử")))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("CORRECTION_NOT_ALLOWED"));
    }

    @Test
    void BR_M01_05_closedProfile_blocksNewDraft() {
        long resident = newResident();
        setStatus(resident, "DECEASED");
        user.set(doctor.user());

        assertThatThrownBy(() -> records.saveDraft(target(resident), null, content("A")))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("PROFILE_CLOSED"));
    }

    private int audit(String action, long recordId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M01.INITIAL_HEALTH_RECORD' AND entity_id = ?"
                        + " AND action = ?",
                Integer.class,
                String.valueOf(recordId),
                action);
    }
}
