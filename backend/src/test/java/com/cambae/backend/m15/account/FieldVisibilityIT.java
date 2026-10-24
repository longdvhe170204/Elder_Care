package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.PermissionMatrixIT.FLOOR_1;
import static com.cambae.backend.m15.account.PermissionMatrixIT.FLOOR_2;
import static com.cambae.backend.m15.account.PermissionMatrixIT.resident;
import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.FieldGroup;
import com.cambae.backend.m15.account.api.FieldVisibilityApi;
import com.cambae.backend.m15.account.api.SelfHiddenFields;
import com.cambae.backend.m15.account.api.VisibilityTarget;
import com.cambae.backend.m15.account.api.dto.AccessChannel;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.dto.Visibility;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.m15.account.api.spi.BedAllocationView;
import com.cambae.backend.m15.account.internal.auth.domain.ActiveSession;
import com.cambae.backend.m15.account.internal.auth.service.SessionAuthentication;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeBedAllocations;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 002 T019 (FR-022, FR-034, US1 #6, #11; research A-08): mức xem theo nhóm thông tin của từng vai trò (19.3, chú thích
 * ⁵, ¹⁴, ¹⁶, ³⁰) xét cùng phạm vi của chính vai trò đó rồi mới hợp; khi tuần tự hóa, trường không được xem đầy đủ bị
 * bỏ khỏi phản hồi và liệt kê trong {@code hiddenFields}.
 */
@Import(FakeM15Config.class)
class FieldVisibilityIT extends MySqlIntegrationTestBase {

    private static final AtomicLong IDS = new AtomicLong(950_000);

    @Autowired
    FieldVisibilityApi visibility;

    @Autowired
    JsonMapper json;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeBedAllocations bedAllocations;

    @Autowired
    BusinessTime time;

    Target onFloor1;
    Target onFloor2;

    /** Hồ sơ mẫu: dị ứng thuộc sức khỏe, nguyện vọng chỉ "có/không" với Hành chính. */
    record SampleProfile(
            long residentId,
            String fullName,
            @FieldGroup(InfoGroup.CARE_LEVEL) String careLevel,
            @FieldGroup(InfoGroup.HEALTH) List<String> allergies,
            @FieldGroup(InfoGroup.WISHES) String wishes,
            boolean hasWishes,
            List<String> hiddenFields)
            implements VisibilityTarget {

        @Override
        public Target visibilityTarget() {
            return resident(residentId);
        }
    }

    @BeforeEach
    void residents() {
        long a = IDS.incrementAndGet();
        long b = IDS.incrementAndGet();
        bedAllocations.allocate(a, new BedAllocationView(IDS.incrementAndGet(), 11, 101, FLOOR_1));
        bedAllocations.allocate(b, new BedAllocationView(IDS.incrementAndGet(), 21, 201, FLOOR_2));
        onFloor1 = resident(a);
        onFloor2 = resident(b);
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void FR_022_officeAndAccountant_noHealthNoMedication_wishesPresenceOnly_careLevelFull() {
        for (Role role : List.of(Role.OFFICE_STAFF, Role.ACCOUNTANT)) {
            CurrentUser user = staff(EnumSet.of(role), null);

            assertThat(visibility.canView(user, InfoGroup.HEALTH, onFloor1)).isEqualTo(Visibility.NONE);
            assertThat(visibility.canView(user, InfoGroup.MEDICATION, onFloor1)).isEqualTo(Visibility.NONE);
            assertThat(visibility.canView(user, InfoGroup.WISHES, onFloor1)).isEqualTo(Visibility.PRESENCE_ONLY);
            assertThat(visibility.canView(user, InfoGroup.CARE_LEVEL, onFloor2)).isEqualTo(Visibility.FULL);
        }
    }

    @Test
    void US1_6_kitchen_noHealthMedicationWishes() {
        CurrentUser kitchen = staff(EnumSet.of(Role.KITCHEN_STAFF), null);

        assertThat(visibility.canView(kitchen, InfoGroup.HEALTH, onFloor1)).isEqualTo(Visibility.NONE);
        assertThat(visibility.canView(kitchen, InfoGroup.WISHES, onFloor1)).isEqualTo(Visibility.NONE);
        assertThat(visibility.canView(kitchen, InfoGroup.IDENTITY, onFloor1)).isEqualTo(Visibility.FULL);
    }

    @Test
    void US1_11_floorManagerOfResidentsFloor_seesHealth_otherFloorNot() {
        CurrentUser floorManager = staff(EnumSet.of(Role.FLOOR_MANAGER), FLOOR_1);

        assertThat(visibility.canView(floorManager, InfoGroup.HEALTH, onFloor1)).isEqualTo(Visibility.FULL);
        assertThat(visibility.canView(floorManager, InfoGroup.HEALTH, onFloor2)).isEqualTo(Visibility.NONE);
    }

    @Test
    void FR_034_officeAlsoNurse_seesHealthOnlyInNursingScope() {
        CurrentUser officeNurse = staff(EnumSet.of(Role.OFFICE_STAFF, Role.NURSING_STAFF), FLOOR_1);

        assertThat(visibility.canView(officeNurse, InfoGroup.HEALTH, onFloor1)).isEqualTo(Visibility.FULL);
        assertThat(visibility.canView(officeNurse, InfoGroup.HEALTH, onFloor2)).isEqualTo(Visibility.NONE);
        assertThat(visibility.canView(officeNurse, InfoGroup.WISHES, onFloor2)).isEqualTo(Visibility.PRESENCE_ONLY);
    }

    @Test
    void A_08_serialization_dropsHiddenFields_andListsThem() {
        signIn(staff(EnumSet.of(Role.OFFICE_STAFF), null));
        SampleProfile profile = new SampleProfile(
                Long.parseLong(onFloor1.id()), "Nguyễn Văn A", "INTENSIVE", List.of("Tôm"), "Về quê", true, null);

        JsonNode body = json.valueToTree(profile);

        assertThat(body.has("fullName")).isTrue();
        assertThat(body.get("careLevel").asString()).isEqualTo("INTENSIVE");
        assertThat(body.has("allergies")).isFalse();
        assertThat(body.has("wishes")).isFalse();
        assertThat(body.get("hasWishes").asBoolean()).isTrue();
        assertThat(body.get("hiddenFields").values())
                .extracting(JsonNode::asString)
                .containsExactlyInAnyOrder("allergies", "wishes");
    }

    @Test
    void A_08_serialization_fullVisibility_keepsFields_emptyHiddenList() {
        signIn(staff(EnumSet.of(Role.NURSING_HOME_MANAGER), null));
        SampleProfile profile = new SampleProfile(
                Long.parseLong(onFloor2.id()), "Nguyễn Văn B", "BASIC", List.of("Tôm"), "Về quê", true, null);

        JsonNode body = json.valueToTree(profile);

        assertThat(body.get("allergies").get(0).asString()).isEqualTo("Tôm");
        assertThat(body.get("wishes").asString()).isEqualTo("Về quê");
        assertThat(body.get("hiddenFields").isArray()).isTrue();
        assertThat(body.get("hiddenFields").isEmpty()).isTrue();
    }

    /** Cờ có/không mà module tự kiểm quyền, không gắn nhóm (001 {@code hasTreatmentWish}, FR-023m; "Đổi api" 2026-10-24). */
    record SampleFlags(
            long residentId,
            Boolean hasTreatmentWish,
            List<String> hiddenFields,
            @JsonIgnore Set<String> hide) implements SelfHiddenFields {

        @Override
        public Set<String> selfHiddenFields() {
            return hide;
        }
    }

    /** Vừa có trường theo nhóm vừa có trường module tự ẩn. */
    record SampleMixed(
            long residentId,
            @FieldGroup(InfoGroup.HEALTH) List<String> allergies,
            Boolean hasTreatmentWish,
            List<String> hiddenFields,
            @JsonIgnore Set<String> hide)
            implements VisibilityTarget, SelfHiddenFields {

        @Override
        public Target visibilityTarget() {
            return resident(residentId);
        }

        @Override
        public Set<String> selfHiddenFields() {
            return hide;
        }
    }

    @Test
    void A_08_selfHiddenField_omittedAndListedInHiddenFields() {
        signIn(staff(EnumSet.of(Role.ACCOUNTANT), null));

        JsonNode body = json.valueToTree(new SampleFlags(1, null, null, Set.of("hasTreatmentWish")));

        assertThat(body.has("hasTreatmentWish")).isFalse();
        assertThat(body.has("hide")).isFalse();
        assertThat(body.has("selfHiddenFields")).isFalse();
        assertThat(body.get("hiddenFields").values())
                .extracting(JsonNode::asString)
                .containsExactly("hasTreatmentWish");
    }

    @Test
    void A_08_selfHiddenEmpty_fieldKept() {
        signIn(staff(EnumSet.of(Role.OFFICE_STAFF), null));

        JsonNode body = json.valueToTree(new SampleFlags(1, true, null, Set.of()));

        assertThat(body.get("hasTreatmentWish").asBoolean()).isTrue();
        assertThat(body.get("hiddenFields").isEmpty()).isTrue();
    }

    @Test
    void A_08_groupedAndSelfHidden_bothListedInDeclarationOrder() {
        signIn(staff(EnumSet.of(Role.ACCOUNTANT), null));
        SampleMixed mixed =
                new SampleMixed(Long.parseLong(onFloor1.id()), List.of("Tôm"), null, null, Set.of("hasTreatmentWish"));

        JsonNode body = json.valueToTree(mixed);

        assertThat(body.has("allergies")).isFalse();
        assertThat(body.has("hasTreatmentWish")).isFalse();
        assertThat(body.get("hiddenFields").values())
                .extracting(JsonNode::asString)
                .containsExactly("allergies", "hasTreatmentWish");
    }

    private void signIn(ActiveSession user) {
        SecurityContextHolder.getContext().setAuthentication(new SessionAuthentication(user));
    }

    private ActiveSession staff(EnumSet<Role> roles, Long assignedFloor) {
        long staffId = IDS.incrementAndGet();
        if (assignedFloor != null) {
            assignments.assign(
                    staffId,
                    new AssignmentView(
                            AssignmentKind.SHIFT_MEMBER,
                            assignedFloor,
                            null,
                            null,
                            null,
                            time.now().minus(Duration.ofDays(1)),
                            null));
        }
        return new ActiveSession(
                staffId,
                AccountType.STAFF,
                Optional.of(staffId),
                Optional.empty(),
                java.util.Set.copyOf(roles),
                "visibility-it",
                AccessChannel.WEB);
    }
}
