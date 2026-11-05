package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.spi.RepresentativePort;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.family.api.dto.FamilyMemberView;
import com.eldercare.backend.family.api.events.FamilyRightChanged;
import com.eldercare.backend.family.api.events.RelationshipChange;
import com.eldercare.backend.family.api.events.RelationshipChanged;
import com.eldercare.backend.family.internal.relationship.domain.RelationshipAttribute;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

/**
 * 012 T009 (US1; DBR-02, FR-003 → FR-006, FR-012; quickstart Q2 → Q4): đúng một liên hệ chính ({@code UNIQUE}), ít nhất
 * một người đại diện với người cao tuổi đang ở viện; Đang tiếp nhận lưu được khi chưa đủ nhưng Hoàn tất tiếp nhận bị chặn
 * điều kiện {@code DBR_02}; kết thúc quan hệ xét lại tài khoản trong lệnh và bỏ khỏi danh sách đón.
 */
class DBR_02_PrimaryContactIT extends FamilyCommandTestBase {

    @Autowired
    FamilyRightsApi rights;

    @Autowired
    RepresentativePort representatives;

    @Test
    void DBR_02_atMostOnePrimaryContact() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long r = relate(a, member("Trần R"), true, true);
        long m = member("Trần M");

        createRelationship(a, m, true, false)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DBR_02_VIOLATED"));

        long second = relate(a, m, false, false);
        assertThatThrownBy(
                        () -> jdbc.update("UPDATE family_relationship SET primary_contact = TRUE WHERE id = ?", second))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(column(r, "primary_contact")).isTrue();
    }

    @Test
    void FR_004_changePrimaryContact_movesTheRoleInOneCommand() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long r = relate(a, member("Trần R"), true, true);
        long m = relate(a, member("Trần M"), false, false);

        command(m, "change-primary-contact", reason("R đi công tác dài ngày")).andExpect(status().isOk());

        assertThat(column(m, "primary_contact")).isTrue();
        assertThat(column(r, "primary_contact")).isFalse();
        // FR-009: liên hệ chính luôn nhận thông báo khẩn; người cũ giữ quyền tới khi có yêu cầu tắt
        assertThat(column(m, "receive_urgent")).isTrue();
        assertThat(column(r, "receive_urgent")).isTrue();
        assertThat(history.valueAt(m, RelationshipAttribute.PRIMARY_CONTACT, time.now()))
                .isTrue();
        assertThat(history.valueAt(r, RelationshipAttribute.PRIMARY_CONTACT, time.now()))
                .isFalse();
        assertThat(events.stream(RelationshipChanged.class))
                .anyMatch(e -> e.relationshipId() == m && e.change() == RelationshipChange.PRIMARY_CONTACT_CHANGED);
    }

    @Test
    void DBR_02_endingTheOnlyRepresentativeAndPrimaryContact_isRejected() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long r = relate(a, member("Trần R"), true, true);

        command(r, "end", reason("Không còn liên lạc"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DBR_02_VIOLATED"));

        assertThat(relationshipStatus(r)).isEqualTo("ACTIVE");
    }

    @Test
    void FR_006_end_rechecksTheAccount_andRemovesFromThePickupList() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        relate(a, member("Trần R"), true, true);
        long mMember = member("Trần M", "079000000001");
        long m = relate(a, mMember, false, false);
        jdbc.update("UPDATE family_relationship SET pickup_allowed = TRUE WHERE id = ?", m);
        fixtures.record(
                m, RelationshipAttribute.PICKUP_ALLOWED, true, time.now().minusSeconds(60));
        long account = users.family(mMember).accountId();

        command(m, "end", reason("Gia đình báo không còn liên quan")).andExpect(status().isOk());

        assertThat(relationshipStatus(m)).isEqualTo("ENDED");
        assertThat(column(m, "pickup_allowed")).isFalse();
        assertThat(rights.allowedPickersAt(a, time.now()))
                .extracting(FamilyMemberView::familyMemberId)
                .doesNotContain(mMember);
        // 002 FR-049: lệnh gọi AccountApi.recheckFamilyMember trong transaction của nó
        assertThat(jdbc.queryForObject("SELECT lock_cause FROM account WHERE id = ?", String.class, account))
                .isEqualTo("RELATIONSHIP_ENDED");
        assertThat(events.stream(RelationshipChanged.class))
                .anyMatch(e -> e.relationshipId() == m && e.change() == RelationshipChange.ENDED);
        assertThat(events.stream(FamilyRightChanged.class))
                .anyMatch(e -> e.relationshipId() == m && e.right() == FamilyRight.PICKUP_ALLOWED && !e.value());
        // Đã kết thúc thì không kích hoạt lại (FR-006)
        command(m, "change-contact-order", "{\"reason\": \"Đổi thứ tự\", \"contactOrder\": 1}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
    }

    @Test
    void FR_003_admittingResident_savesWithoutDbr02_butCompleteAdmissionIsBlocked() throws Exception {
        long b = resident(ResidentStatus.ADMISSION_IN_PROGRESS);
        long n = relate(b, member("Lê N"), false, false);
        assertThat(relationshipStatus(n)).isEqualTo("ACTIVE");

        assertThat(completeAdmissionConditions(b)).singleElement().satisfies(c -> {
            assertThat(c.code()).isEqualTo("DBR_02");
            assertThat(c.waivable()).isFalse();
        });

        relate(b, member("Lê R"), true, true);
        assertThat(completeAdmissionConditions(b)).isEmpty();
    }

    @Test
    void FR_005_firstRepresentative_isSetDirectly_onlyWhenThereIsNone() throws Exception {
        long b = resident(ResidentStatus.ADMISSION_IN_PROGRESS);
        long r = relate(b, member("Phạm R"), true, false);
        long s = relate(b, member("Phạm S"), false, false);
        assertThat(representatives.hasRepresentative(b)).isFalse();

        command(r, "set-first-representative", reason("Người ký hợp đồng"))
                .andExpect(status().isUnprocessableContent());
        command(
                        r,
                        "set-first-representative",
                        "{\"reason\": \"Người ký hợp đồng\", \"evidenceFileId\": " + file() + "}")
                .andExpect(status().isOk());

        assertThat(representatives.hasRepresentative(b)).isTrue();
        assertThat(column(r, "representative")).isTrue();
        // FR-010: được yêu cầu thay đổi dịch vụ tự bật khi là người đại diện
        assertThat(column(r, "request_services")).isTrue();
        assertThat(history.valueAt(r, RelationshipAttribute.REPRESENTATIVE, time.now()))
                .isTrue();
        // Đã có người đại diện: đặt thêm đi qua yêu cầu (FR-005, Q-127)
        command(s, "set-first-representative", "{\"reason\": \"Thêm\", \"evidenceFileId\": " + file() + "}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REPRESENTATIVE_ALREADY_EXISTS"));
    }

    @Test
    void FR_012_changeContactOrder() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long r = relate(a, member("Võ R"), true, true);
        long n = relate(a, member("Võ N"), false, false);
        assertThat(jdbc.queryForObject("SELECT contact_order FROM family_relationship WHERE id = ?", Integer.class, n))
                .isGreaterThan(jdbc.queryForObject(
                        "SELECT contact_order FROM family_relationship WHERE id = ?", Integer.class, r));

        command(n, "change-contact-order", "{\"reason\": \"N ở gần viện\", \"contactOrder\": 0}")
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT contact_order FROM family_relationship WHERE id = ?", Integer.class, n))
                .isZero();
    }

    private List<Condition> completeAdmissionConditions(long residentId) {
        return familyParticipant()
                .checkConditions(new CommandContext(
                        residentId,
                        ResidentStatus.ADMISSION_IN_PROGRESS,
                        StatusCommand.COMPLETE_ADMISSION,
                        null,
                        time.now(),
                        null));
    }
}
