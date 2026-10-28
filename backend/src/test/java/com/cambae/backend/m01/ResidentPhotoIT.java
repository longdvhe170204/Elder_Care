package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.file.FileService;
import com.cambae.backend.common.file.FileTypes;
import com.cambae.backend.common.file.UnattachedFileCleanup;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m01.internal.profile.domain.PersonalInfo;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService.ProfileView;
import com.cambae.backend.support.fake.file.FakeFileConfig;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

/**
 * 001 T018 (FR-001, FR-006; research H-13, NFR-08): ảnh người cao tuổi là tệp của {@code common.file}. Tạo hoặc thay
 * ảnh thì tệp được gắn vào hồ sơ (chủ sở hữu {@code M01.RESIDENT_PHOTO}, id hồ sơ) trong transaction của lệnh: tệp
 * không có thật hoặc đã gắn nơi khác thì lệnh bị từ chối và không đổi gì; tệp đã gắn không bị job dọn tệp chưa gắn xóa;
 * tải ảnh theo quyền xem nhóm nhận dạng của hồ sơ.
 */
@Import(FakeFileConfig.class)
class ResidentPhotoIT extends HealthTestBase {

    private static final String OWNER_TYPE = "M01.RESIDENT_PHOTO";

    @Autowired
    FileApi files;

    @Autowired
    FileService fileService;

    @Autowired
    UnattachedFileCleanup cleanup;

    @Autowired
    TestClockControl clock;

    @AfterEach
    void realClock() {
        clock.reset();
    }

    private long photo() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4};
        return files.upload("anh.jpg", FileTypes.JPEG, jpeg, office.accountId());
    }

    private PersonalInfo info(String name, Long photoFileId) {
        return new PersonalInfo(
                name, LocalDate.of(1944, 5, 5), Gender.FEMALE, null, null, photoFileId, null, null, null, null);
    }

    private String uniqueName() {
        return "Ảnh " + UUID.randomUUID();
    }

    private Map<String, Object> fileRow(long fileId) {
        return jdbc.queryForMap("SELECT owner_type, owner_id, attached_at FROM file WHERE id = ?", fileId);
    }

    private int residentsNamed(String name) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM resident WHERE full_name = ?", Integer.class, name);
    }

    private ProfileView createWithPhoto(Long photoFileId) {
        user.set(office.user());
        return profiles.create(info(uniqueName(), photoFileId), false).profile();
    }

    @Test
    void FR_001_create_attachesThePhotoToTheNewProfile() {
        long file = photo();

        ProfileView created = createWithPhoto(file);

        Map<String, Object> row = fileRow(file);
        assertThat(row.get("owner_type")).isEqualTo(OWNER_TYPE);
        assertThat(((Number) row.get("owner_id")).longValue()).isEqualTo(created.id());
        assertThat(created.personalInfo().photoFileId()).isEqualTo(file);
    }

    @Test
    void FR_001_create_withAPhotoThatDoesNotExist_isRefused_andCreatesNoProfile() {
        String name = uniqueName();
        user.set(office.user());

        assertThatThrownBy(() -> profiles.create(info(name, 987_654_321L), false))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(residentsNamed(name)).isZero();
    }

    @Test
    void FR_001_create_withAPhotoAlreadyAttached_isRefused_andCreatesNoProfile() {
        long file = photo();
        createWithPhoto(file);
        String name = uniqueName();

        assertThatThrownBy(() -> profiles.create(info(name, file), false))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.errorCode()).isEqualTo("FILE_ALREADY_ATTACHED");
                });
        assertThat(residentsNamed(name)).isZero();
    }

    @Test
    void FR_006_update_replacingThePhoto_attachesTheNewOne_andKeepsTheOldAsHistory() {
        long first = photo();
        ProfileView created = createWithPhoto(first);
        long second = photo();

        ProfileView updated = profiles.updatePersonalInfo(
                target(created.id()),
                "\"" + created.version() + "\"",
                info(created.personalInfo().fullName(), second),
                new Reason("Đổi ảnh mới"));

        assertThat(updated.personalInfo().photoFileId()).isEqualTo(second);
        assertThat(fileRow(second).get("owner_type")).isEqualTo(OWNER_TYPE);
        assertThat(((Number) fileRow(second).get("owner_id")).longValue()).isEqualTo(created.id());
        assertThat(fileRow(first).get("attached_at")).isNotNull();
    }

    @Test
    void FR_006_update_keepingTheSamePhoto_doesNotAttachAgain() {
        long file = photo();
        ProfileView created = createWithPhoto(file);

        ProfileView updated = profiles.updatePersonalInfo(
                target(created.id()),
                "\"" + created.version() + "\"",
                info(created.personalInfo().fullName() + " Sửa", file),
                new Reason("Sửa tên"));

        assertThat(updated.personalInfo().photoFileId()).isEqualTo(file);
    }

    @Test
    void FR_006_update_withAPhotoThatDoesNotExist_isRefused_andNothingChanges() {
        ProfileView created = createWithPhoto(null);

        assertThatThrownBy(() -> profiles.updatePersonalInfo(
                        target(created.id()),
                        "\"" + created.version() + "\"",
                        info(created.personalInfo().fullName(), 987_654_321L),
                        new Reason("Đổi ảnh")))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND));

        Long stored = jdbc.queryForObject("SELECT photo_file_id FROM resident WHERE id = ?", Long.class, created.id());
        assertThat(stored).isNull();
    }

    @Test
    void NFR_08_attachedPhoto_isNotRemovedByTheUnattachedFileCleanup() {
        long attached = photo();
        long loose = photo();
        createWithPhoto(attached);

        clock.advanceBy(Duration.ofHours(25));
        cleanup.run();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM file WHERE id = ?", Integer.class, attached))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM file WHERE id = ?", Integer.class, loose))
                .isZero();
    }

    @Test
    void NFR_08_photo_isDownloadedByWhoSeesTheIdentityGroup_notByAnUnassignedNurse() {
        long file = photo();
        ProfileView created = createWithPhoto(file);

        user.set(office.user());
        assertThat(fileService.download(file, office.accountId()).content()).isNotEmpty();
        user.set(doctor.user());
        assertThat(fileService.download(file, doctor.accountId()).content()).isNotEmpty();
        user.set(nurse.user());
        assertThatThrownBy(() -> fileService.download(file, nurse.accountId()))
                .isInstanceOf(PermissionDeniedException.class);

        assignNurse(nurse, created.id());
        assertThat(fileService.download(file, nurse.accountId()).content()).isNotEmpty();
    }
}
