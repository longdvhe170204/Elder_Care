package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.file.FileService;
import com.cambae.backend.common.file.FileTypes;
import com.cambae.backend.common.file.FileView;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.support.fake.file.FakeFileConfig;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * 001 T081, T082 (FR-047c, BR-M05-32, quickstart T5; research H-18): giấy tờ bệnh viện đính kèm lệnh trạng thái. Chỉ
 * "Ghi nhận trở về" từ Điều trị tại bệnh viện (giấy ra viện, đơn thuốc) và "Chuyển viện" (giấy chuyển viện, kể cả khi
 * 007 gọi qua {@code ResidentStatusApi}) nhận {@code attachmentFileIds}, tối đa 10, không bắt buộc. Tệp gắn vào dòng lịch
 * sử trong transaction của lệnh (chủ sở hữu {@code M01.HOSPITAL_DOCUMENT}); tệp không có, đã gắn, hay lệnh không nhận tệp
 * thì lệnh bị từ chối và không đổi gì. Tải tệp theo quyền xem dị ứng, bệnh nền trong phạm vi (Hành chính không tải được).
 */
@Import(FakeFileConfig.class)
class HospitalDocumentsIT extends StatusTestBase {

    private static final String OWNER_TYPE = "M01.HOSPITAL_DOCUMENT";

    @Autowired
    FileApi files;

    @Autowired
    FileService fileService;

    private long scan() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4};
        return files.upload("giay-ra-vien.jpg", FileTypes.JPEG, jpeg, office.accountId());
    }

    private static ObjectNode withFiles(long... ids) {
        ObjectNode details = JsonNodeFactory.instance.objectNode();
        ArrayNode array = details.putArray("attachmentFileIds");
        for (long id : ids) {
            array.add(id);
        }
        return details;
    }

    private static JsonNode transferDetails(ObjectNode node) {
        node.putObject("basis").put("incidentId", 9L);
        return node;
    }

    private Map<String, Object> fileRow(long fileId) {
        return jdbc.queryForMap("SELECT owner_type, owner_id, attached_at FROM file WHERE id = ?", fileId);
    }

    private long lastHistoryId(long residentId) {
        return jdbc.queryForObject(
                "SELECT id FROM resident_status_history WHERE resident_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                residentId);
    }

    private void assertAttachedTo(long fileId, long historyId) {
        Map<String, Object> row = fileRow(fileId);
        assertThat(row.get("owner_type")).isEqualTo(OWNER_TYPE);
        assertThat(((Number) row.get("owner_id")).longValue()).isEqualTo(historyId);
    }

    @Test
    void FR_047c_recordReturnFromHospital_attachesBothFilesToTheHistoryRow() {
        long id = residentIn(ResidentStatus.HOSPITALIZED);
        long dischargeLetter = scan();
        long prescription = scan();
        user.set(office.user());

        statusService.recordReturn(target(id), reason(), withFiles(dischargeLetter, prescription));

        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertAttachedTo(dischargeLetter, lastHistoryId(id));
        assertAttachedTo(prescription, lastHistoryId(id));
    }

    @Test
    void FR_047c_attachmentsOfARow_areListedWithNameTypeAndSize_forTheOtherModules() {
        long id = residentIn(ResidentStatus.HOSPITALIZED);
        long dischargeLetter = scan();
        long prescription = scan();
        user.set(office.user());
        statusService.recordReturn(target(id), reason(), withFiles(dischargeLetter, prescription));

        var listed = statusApi.hospitalDocuments(lastHistoryId(id));

        assertThat(listed).extracting(FileView::id).containsExactly(dischargeLetter, prescription);
        assertThat(listed.get(0).originalName()).isEqualTo("giay-ra-vien.jpg");
        assertThat(listed.get(0).mimeType()).isEqualTo(FileTypes.JPEG);
        assertThat(listed.get(0).sizeBytes()).isEqualTo(8);
        assertThat(statusApi.hospitalDocuments(987_654_321L)).isEmpty();
    }

    @Test
    void FR_047c_recordReturnFromHospital_withoutFiles_stillSucceeds() {
        long id = residentIn(ResidentStatus.HOSPITALIZED);
        user.set(office.user());

        statusService.recordReturn(target(id), reason(), noDetails());

        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_047c_transferOut_attachesTheTransferLetter() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        long letter = scan();
        user.set(doctor.user());

        statusService.transferOut(target(id), reason(), transferDetails(withFiles(letter)));

        assertAttachedTo(letter, lastHistoryId(id));
    }

    @Test
    void BR_M05_32_transferOutCalledByTheIncidentModule_attachesTheFilesToo() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        long letter = scan();

        inTransaction(() -> statusApi.execute(
                id,
                StatusCommand.TRANSFER_OUT,
                doctor.user(),
                Optional.of(reason()),
                withFiles(letter),
                sourceRecord(5L)));

        assertAttachedTo(letter, lastHistoryId(id));
    }

    @Test
    void FR_047c_aCommandThatDoesNotTakeFiles_refuses_andChangesNothing() {
        for (ResidentStatus from :
                new ResidentStatus[] {ResidentStatus.IN_RESIDENCE, ResidentStatus.TEMPORARILY_ABSENT}) {
            long id = residentIn(from);
            long file = scan();
            int history = historyCount(id);
            user.set(office.user());

            BusinessException e = failure(() -> {
                if (from == ResidentStatus.IN_RESIDENCE) {
                    statusService.grantTemporaryAbsence(target(id), reason(), withFiles(file));
                } else {
                    // Về từ Tạm vắng không nhận giấy tờ bệnh viện (chỉ từ Điều trị tại bệnh viện)
                    statusService.recordReturn(target(id), reason(), withFiles(file));
                }
            });

            assertThat(e).as("%s", from).isNotNull();
            assertThat(e.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
            assertThat(e.errorCode()).isEqualTo("ATTACHMENTS_NOT_ACCEPTED");
            assertThat(currentStatus(id)).isEqualTo(from);
            assertThat(historyCount(id)).isEqualTo(history);
            assertThat(fileRow(file).get("attached_at")).isNull();
        }
    }

    @Test
    void FR_047c_moreThanTenFiles_isRefused_andNothingIsAttached() {
        long id = residentIn(ResidentStatus.HOSPITALIZED);
        long[] ids = new long[11];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = scan();
        }
        user.set(office.user());

        BusinessException e = failure(() -> statusService.recordReturn(target(id), reason(), withFiles(ids)));

        assertThat(e).isNotNull();
        assertThat(e.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(e.errorCode()).isEqualTo("TOO_MANY_ATTACHMENTS");
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.HOSPITALIZED);
        assertThat(fileRow(ids[0]).get("attached_at")).isNull();
    }

    @Test
    void FR_047c_aFileThatDoesNotExist_rollsTheWholeCommandBack_includingFilesAttachedBefore() {
        long id = residentIn(ResidentStatus.HOSPITALIZED);
        long good = scan();
        int history = historyCount(id);
        user.set(office.user());

        BusinessException e =
                failure(() -> statusService.recordReturn(target(id), reason(), withFiles(good, 987_654_321L)));

        assertThat(e).isNotNull();
        assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.HOSPITALIZED);
        assertThat(historyCount(id)).isEqualTo(history);
        assertThat(fileRow(good).get("attached_at")).isNull();
    }

    @Test
    void FR_047c_aFileAlreadyAttachedElsewhere_rollsTheCommandBack() {
        long first = residentIn(ResidentStatus.HOSPITALIZED);
        long second = residentIn(ResidentStatus.HOSPITALIZED);
        long file = scan();
        user.set(office.user());
        statusService.recordReturn(target(first), reason(), withFiles(file));
        int history = historyCount(second);

        BusinessException e = failure(() -> statusService.recordReturn(target(second), reason(), withFiles(file)));

        assertThat(e).isNotNull();
        assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(e.errorCode()).isEqualTo("FILE_ALREADY_ATTACHED");
        assertThat(currentStatus(second)).isEqualTo(ResidentStatus.HOSPITALIZED);
        assertThat(historyCount(second)).isEqualTo(history);
    }

    @Test
    void FR_047c_sameFileListedTwice_isAttachedOnce() {
        long id = residentIn(ResidentStatus.HOSPITALIZED);
        long file = scan();
        user.set(office.user());

        statusService.recordReturn(target(id), reason(), withFiles(file, file));

        assertAttachedTo(file, lastHistoryId(id));
    }

    @Test
    void NFR_08_hospitalDocuments_areDownloadedByWhoSeesAllergiesAndConditionsInScope_notByOfficeStaff() {
        long id = residentIn(ResidentStatus.HOSPITALIZED);
        long file = scan();
        user.set(office.user());
        statusService.recordReturn(target(id), reason(), withFiles(file));

        user.set(doctor.user());
        assertThat(fileService.download(file, doctor.accountId()).content()).isNotEmpty();
        user.set(office.user());
        assertThatThrownBy(() -> fileService.download(file, office.accountId()))
                .isInstanceOf(PermissionDeniedException.class);
        user.set(nurse.user());
        assertThatThrownBy(() -> fileService.download(file, nurse.accountId()))
                .isInstanceOf(PermissionDeniedException.class);
        assignNurse(nurse, id);
        assertThat(fileService.download(file, nurse.accountId()).content()).isNotEmpty();
    }
}
