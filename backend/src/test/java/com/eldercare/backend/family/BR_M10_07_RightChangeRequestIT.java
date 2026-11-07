package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.AccessChannel;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.internal.auth.domain.ActiveSession;
import com.eldercare.backend.administration.account.internal.auth.service.SessionAuthentication;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.family.api.events.FamilyRightChanged;
import com.eldercare.backend.family.internal.relationship.domain.RelationshipAttribute;
import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.JsonNode;

/**
 * 012 T014 (BR-M10-07; FR-005, FR-006, FR-013 → FR-020, FR-069, FR-075; quickstart Q8 → Q12; US3 kịch bản 1 → 8): vòng
 * đời yêu cầu thay đổi quyền qua REST với phiên thật của Hành chính, Quản lý viện và người thân.
 */
class BR_M10_07_RightChangeRequestIT extends FamilyCommandTestBase {

    @Autowired
    List<M10TimedTask> tasks;

    long a;
    long rMember;
    long r;
    long sMember;
    long s;
    long nMember;
    long n;
    String manager;

    /** A có R (người đại diện, liên hệ chính), S (người đại diện), N (người thân thường, được phép đón). */
    @BeforeEach
    void family() throws Exception {
        a = resident(ResidentStatus.IN_RESIDENCE);
        rMember = member("Trần R", "079000000001");
        r = relate(a, rMember, true, true);
        sMember = fixtures.familyMember("Trần S", "0902000001");
        s = fixtures.relate(sMember, a, 5, true, false);
        nMember = fixtures.familyMember("Trần N", "0902000002", "CCCD", "079000000002");
        n = fixtures.relate(nMember, a, 6, false, false, FamilyRight.PICKUP_ALLOWED);
        manager = staffSession(Role.NURSING_HOME_MANAGER);
    }

    @Test
    void US3_1_2_officeAddsNewPicker_pending_thenRepresentativeConfirmsOnPortal() throws Exception {
        JsonNode created = json(createRequest(
                        office,
                        a,
                        "\"type\": \"RIGHT\", \"right\": \"PICKUP_ALLOWED\", \"newValue\": true, \"newPicker\":"
                                + " {\"fullName\": \"Lê X\", \"idDocumentType\": \"CCCD\", \"idDocumentNumber\":"
                                + " \"079000000099\", \"phoneNumber\": \"0903000099\", \"relationship\": \"OTHER\"}")
                .andExpect(status().isCreated()));
        long request = created.get("id").asLong();
        long x = created.get("relationshipId").asLong();

        assertThat(created.get("status").asString()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(created.get("source").asString()).isEqualTo("COUNTER");
        assertThat(column(x, "pickup_allowed")).isFalse();
        assertThat(pickupNames()).doesNotContain("Lê X");
        assertThat(notices("RIGHT_CHANGE_NEEDS_CONFIRMATION")).singleElement().satisfies(notice -> {
            assertThat(notice.level()).contains(NotificationLevel.MEDIUM);
            assertThat(notice.recipientGroups())
                    .contains(
                            new RecipientGroup.FamilyMember(rMember, "REPRESENTATIVE"),
                            new RecipientGroup.FamilyMember(sMember, "REPRESENTATIVE"),
                            new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        });

        requestCommand(familySession(rMember), request, "confirm", "{}").andExpect(status().isOk());

        assertThat(requestStatus(request)).isEqualTo("EFFECTIVE");
        assertThat(column(x, "pickup_allowed")).isTrue();
        assertThat(pickupNames()).contains("Lê X");
        assertThat(requestJson(request).get("confirmationMethod").asString()).isEqualTo("PORTAL");
    }

    @Test
    void US3_2_officeRecordsSignedForm_onPendingRequest() throws Exception {
        long request = id(requestRight(office, a, n, "VIEW_COSTS", true).andExpect(status().isCreated()));

        requestCommand(
                        office,
                        request,
                        "confirm",
                        "{\"evidenceFileId\": " + file() + ", \"signedByFamilyMemberId\": " + rMember + "}")
                .andExpect(status().isOk());

        assertThat(requestStatus(request)).isEqualTo("EFFECTIVE");
        assertThat(requestJson(request).get("confirmationMethod").asString()).isEqualTo("SIGNED_FORM");
        assertThat(column(n, "view_costs")).isTrue();
    }

    @Test
    void US3_3_removingPicker_appliesAtOnce_rejectRestoresFromDecision() throws Exception {
        Instant before = time.now();
        JsonNode created =
                json(requestRight(office, a, n, "PICKUP_ALLOWED", false).andExpect(status().isCreated()));
        long request = created.get("id").asLong();

        assertThat(created.get("status").asString()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(created.get("appliedOnCreation").asBoolean()).isTrue();
        assertThat(column(n, "pickup_allowed")).isFalse();
        assertThat(events.stream(FamilyRightChanged.class))
                .anyMatch(e -> e.relationshipId() == n && e.right() == FamilyRight.PICKUP_ALLOWED && !e.value());
        Instant removedAt = time.now();

        requestCommand(familySession(rMember), request, "reject", reason("N vẫn đón được"))
                .andExpect(status().isOk());

        assertThat(requestStatus(request)).isEqualTo("REJECTED");
        assertThat(column(n, "pickup_allowed")).isTrue();
        assertThat(history.valueAt(n, RelationshipAttribute.PICKUP_ALLOWED, before))
                .isTrue();
        assertThat(history.valueAt(n, RelationshipAttribute.PICKUP_ALLOWED, removedAt))
                .isFalse();
        assertThat(history.valueAt(n, RelationshipAttribute.PICKUP_ALLOWED, time.now()))
                .isTrue();
    }

    @Test
    void US3_3_6_Q8_nonRepresentative_mayOnlyRemoveThemself() throws Exception {
        long mMember = fixtures.familyMember("Trần M", "0902000003", "CCCD", "079000000003");
        long m = fixtures.relate(mMember, a, 7, false, false, FamilyRight.PICKUP_ALLOWED);
        String mSession = familySession(mMember);

        requestRight(mSession, a, n, "PICKUP_ALLOWED", false)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("REQUEST_NOT_ALLOWED"));
        requestRight(mSession, a, m, "VIEW_COSTS", true)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("REQUEST_NOT_ALLOWED"));

        JsonNode own =
                json(requestRight(mSession, a, m, "PICKUP_ALLOWED", false).andExpect(status().isCreated()));

        assertThat(own.get("source").asString()).isEqualTo("PORTAL");
        assertThat(own.get("appliedOnCreation").asBoolean()).isTrue();
        assertThat(column(m, "pickup_allowed")).isFalse();
        requestCommand(mSession, own.get("id").asLong(), "confirm", "{}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("REQUEST_NOT_ALLOWED"));
    }

    @Test
    void US3_4_pendingRightKeepsOldValue_untilManagerApproves() throws Exception {
        long request = id(requestRight(office, a, n, "VIEW_COSTS", true).andExpect(status().isCreated()));

        assertThat(column(n, "view_costs")).isFalse();
        requestCommand(manager, request, "approve", "{}").andExpect(status().isUnprocessableContent());

        requestCommand(manager, request, "approve", reason("Đã gọi điện cho R")).andExpect(status().isOk());

        assertThat(requestStatus(request)).isEqualTo("EFFECTIVE");
        assertThat(requestJson(request).get("confirmationMethod").asString()).isEqualTo("MANAGER_APPROVAL");
        assertThat(column(n, "view_costs")).isTrue();
        assertThat(notices("RIGHT_CHANGE_DECIDED")).singleElement().satisfies(notice -> {
            assertThat(notice.level()).contains(NotificationLevel.LOW);
            assertThat(notice.recipientGroups())
                    .contains(
                            new RecipientGroup.FamilyMember(nMember, "AFFECTED"),
                            new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
        });
    }

    @Test
    void US3_5_creatorCancels_removalIsRestored() throws Exception {
        long request = id(requestRight(office, a, n, "PICKUP_ALLOWED", false).andExpect(status().isCreated()));

        requestCommand(office, request, "cancel", reason("Gia đình đổi ý")).andExpect(status().isOk());

        assertThat(requestStatus(request)).isEqualTo("CANCELLED");
        assertThat(column(n, "pickup_allowed")).isTrue();
        requestCommand(manager, request, "approve", reason("Muộn")).andExpect(status().isConflict());
    }

    @Test
    void US3_7_representativeOnPortal_isAlreadyConfirmed() throws Exception {
        JsonNode created = json(
                requestRight(familySession(rMember), a, n, "VIEW_COSTS", true).andExpect(status().isCreated()));

        assertThat(created.get("status").asString()).isEqualTo("EFFECTIVE");
        assertThat(created.get("confirmationMethod").asString()).isEqualTo("PORTAL");
        assertThat(column(n, "view_costs")).isTrue();
        assertThat(notices("RIGHT_CHANGE_DECIDED"))
                .singleElement()
                .satisfies(notice -> assertThat(notice.recipientGroups())
                        .contains(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)));
        assertThat(notices("RIGHT_CHANGE_EFFECTIVE_OTHER_REPRESENTATIVES"))
                .singleElement()
                .satisfies(notice -> assertThat(notice.recipientGroups())
                        .containsExactly(new RecipientGroup.FamilyMember(sMember, "REPRESENTATIVE")));
    }

    @Test
    void Q9_Q249_onlyOtherRepresentatives_areNotified_notTheConfirmer() throws Exception {
        long request = id(requestRight(office, a, n, "VIEW_COSTS", true).andExpect(status().isCreated()));

        requestCommand(familySession(rMember), request, "confirm", "{}").andExpect(status().isOk());

        assertThat(notices("RIGHT_CHANGE_EFFECTIVE_OTHER_REPRESENTATIVES"))
                .singleElement()
                .satisfies(notice -> assertThat(notice.recipientGroups())
                        .containsExactly(new RecipientGroup.FamilyMember(sMember, "REPRESENTATIVE")));
    }

    @Test
    void Q10_FR_018_secondPendingRequest_forSameRight_isRejected() throws Exception {
        requestRight(office, a, n, "VIEW_COSTS", true).andExpect(status().isCreated());

        requestRight(office, a, n, "VIEW_COSTS", true)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_PENDING_REQUEST"));
        requestRight(office, a, n, "REGISTER_VISITS", true).andExpect(status().isCreated());
    }

    @Test
    void Q11_FR_019_overdueRequests_areRemindedThenReported_neverRejected() {
        long request;
        try {
            request = id(requestRight(office, a, n, "VIEW_COSTS", true).andExpect(status().isCreated()));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        Instant created = time.now();
        M10TimedTask reminders = tasks.stream()
                .filter(t -> t.name().equals("RIGHT_CHANGE_REMINDER"))
                .findFirst()
                .orElseThrow();
        assertThat(reminders.cadence()).isEqualTo(M10TimedTask.Cadence.HOURLY);

        tx.executeWithoutResult(t -> reminders.run(created, created.plus(Duration.ofHours(47))));
        assertThat(notices("RIGHT_CHANGE_REMINDER")).isEmpty();

        tx.executeWithoutResult(t -> reminders.run(created, created.plus(Duration.ofHours(49))));
        tx.executeWithoutResult(t -> reminders.run(created, created.plus(Duration.ofHours(50))));
        assertThat(notices("RIGHT_CHANGE_REMINDER"))
                .singleElement()
                .satisfies(notice -> assertThat(notice.recipientGroups())
                        .contains(
                                new RecipientGroup.FamilyMember(rMember, "REPRESENTATIVE"),
                                new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)));
        assertThat(notices("RIGHT_CHANGE_MANAGER_NOTICE")).isEmpty();

        tx.executeWithoutResult(t -> reminders.run(created, created.plus(Duration.ofHours(97))));
        assertThat(notices("RIGHT_CHANGE_MANAGER_NOTICE"))
                .singleElement()
                .satisfies(notice -> assertThat(notice.recipientGroups())
                        .containsExactly(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)));
        assertThat(requestStatus(request)).isEqualTo("PENDING_CONFIRMATION");
    }

    @Test
    void Q12_FR_018_removal_replacesPendingGrant_forSamePerson() throws Exception {
        long mMember = fixtures.familyMember("Trần M", "0902000003", "CCCD", "079000000003");
        long m = fixtures.relate(mMember, a, 7, false, false);
        long grant = id(requestRight(office, a, m, "PICKUP_ALLOWED", true).andExpect(status().isCreated()));

        long removal = id(requestRight(office, a, m, "PICKUP_ALLOWED", false).andExpect(status().isCreated()));

        assertThat(requestStatus(grant)).isEqualTo("CANCELLED");
        assertThat(requestStatus(removal)).isEqualTo("PENDING_CONFIRMATION");
        assertThat(column(m, "pickup_allowed")).isFalse();
    }

    @Test
    void FR_016_twoRepresentativesConfirmAtOnce_exactlyOneSucceeds() throws Exception {
        long request = id(requestRight(office, a, n, "VIEW_COSTS", true).andExpect(status().isCreated()));
        String rSession = familySession(rMember);
        String sSession = familySession(sMember);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (String session : List.of(rSession, sSession)) {
                Callable<Integer> confirm = () -> {
                    start.await();
                    return requestCommand(session, request, "confirm", "{}")
                            .andReturn()
                            .getResponse()
                            .getStatus();
                };
                results.add(pool.submit(confirm));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(requestStatus(request)).isEqualTo("EFFECTIVE");
        assertThat(events.stream(FamilyRightChanged.class)
                        .filter(e -> e.relationshipId() == n && e.right() == FamilyRight.VIEW_COSTS))
                .hasSize(1);
    }

    @Test
    void FR_005_settingRepresentative_isConfirmedByAnotherRepresentative_notByTheNominee() throws Exception {
        long request = id(createRequest(
                        office,
                        a,
                        "\"type\": \"SET_REPRESENTATIVE\", \"relationshipId\": " + n + ", \"evidenceFileId\": "
                                + file())
                .andExpect(status().isCreated()));

        requestCommand(familySession(nMember), request, "confirm", "{}").andExpect(status().isForbidden());
        assertThat(column(n, "representative")).isFalse();

        requestCommand(familySession(sMember), request, "confirm", "{}").andExpect(status().isOk());

        assertThat(column(n, "representative")).isTrue();
        assertThat(column(n, "request_services")).isTrue();
    }

    @Test
    void FR_005_removingLastRepresentative_isBlocked() throws Exception {
        long b = resident(ResidentStatus.IN_RESIDENCE);
        long only = relate(b, member("Lê R"), true, true);

        createRequest(
                        office,
                        b,
                        "\"type\": \"REMOVE_REPRESENTATIVE\", \"relationshipId\": " + only + ", \"evidenceFileId\": "
                                + file())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DBR_02_VIOLATED"));
    }

    @Test
    void FR_013_receiveUrgentTurnedOff_publishesRightChanged() throws Exception {
        long urgent = fixtures.relate(
                fixtures.familyMember("Trần U", "0902000004"), a, 8, false, false, FamilyRight.RECEIVE_URGENT);

        long request =
                id(requestRight(office, a, urgent, "RECEIVE_URGENT", false).andExpect(status().isCreated()));
        requestCommand(familySession(rMember), request, "confirm", "{}").andExpect(status().isOk());

        assertThat(column(urgent, "receive_urgent")).isFalse();
        assertThat(events.stream(FamilyRightChanged.class))
                .anyMatch(e -> e.relationshipId() == urgent && e.right() == FamilyRight.RECEIVE_URGENT && !e.value());
    }

    @Test
    void FR_013_selfReturnFlag_onAfterConfirmation_offAtOnce() throws Exception {
        long on = id(createRequest(office, a, "\"type\": \"SELF_RETURN\", \"newValue\": true")
                .andExpect(status().isCreated()));
        requestCommand(familySession(rMember), on, "confirm", "{}").andExpect(status().isOk());
        assertThat(requestStatus(on)).isEqualTo("EFFECTIVE");

        JsonNode off = json(createRequest(office, a, "\"type\": \"SELF_RETURN\", \"newValue\": false")
                .andExpect(status().isCreated()));

        assertThat(off.get("appliedOnCreation").asBoolean()).isTrue();
        assertThat(off.get("status").asString()).isEqualTo("PENDING_CONFIRMATION");
    }

    @Test
    void FR_006_endingRelationship_cancelsItsPendingRequests() throws Exception {
        long request = id(requestRight(office, a, n, "VIEW_COSTS", true).andExpect(status().isCreated()));

        command(n, "end", reason("Không còn liên lạc")).andExpect(status().isOk());

        assertThat(requestStatus(request)).isEqualTo("CANCELLED");
    }

    @Test
    void FR_075_finalStatus_cancelsPendingRequests() throws Exception {
        long request = id(requestRight(office, a, n, "VIEW_COSTS", true).andExpect(status().isCreated()));
        jdbc.update("UPDATE resident SET status = 'DECEASED' WHERE id = ?", a);

        // Tác động chạy trong lệnh trạng thái của nhân viên (001), nên có phiên hiện tại như lệnh thật
        var doctor = users.staff(Role.DOCTOR);
        SecurityContextHolder.getContext()
                .setAuthentication(new SessionAuthentication(new ActiveSession(
                        doctor.accountId(),
                        AccountType.STAFF,
                        Optional.of(doctor.staffId()),
                        Optional.empty(),
                        java.util.Set.of(Role.DOCTOR),
                        "m10-it",
                        AccessChannel.WEB)));
        try {
            tx.executeWithoutResult(t -> familyParticipant()
                    .applyEffects(new TransitionResult(
                            a,
                            ResidentStatus.IN_RESIDENCE,
                            ResidentStatus.DECEASED,
                            StatusCommand.RECORD_DEATH,
                            0L,
                            time.now(),
                            null)));
        } finally {
            SecurityContextHolder.clearContext();
        }

        assertThat(requestStatus(request)).isEqualTo("CANCELLED");
    }

    @Test
    void FR_013_listForRepresentative_showsRequestsOfTheResident() throws Exception {
        long request = id(requestRight(office, a, n, "VIEW_COSTS", true).andExpect(status().isCreated()));

        JsonNode list = getJsonAs(
                familySession(rMember),
                "/api/v1/right-change-requests?residentId=" + a + "&status=PENDING_CONFIRMATION");

        assertThat(list).extracting(node -> node.get("id").asLong()).containsExactly(request);
    }

    private JsonNode requestJson(long requestId) throws Exception {
        for (JsonNode node : getJson("/api/v1/right-change-requests?residentId=" + a)) {
            if (node.get("id").asLong() == requestId) {
                return node;
            }
        }
        throw new AssertionError("Không thấy yêu cầu " + requestId);
    }

    private List<String> pickupNames() throws Exception {
        List<String> names = new ArrayList<>();
        getJson("/api/v1/residents/" + a + "/pickup-list")
                .forEach(node -> names.add(node.get("fullName").asString()));
        return names;
    }

    private List<NotificationRequest> notices(String eventType) {
        return events.stream(HasNotificationRequests.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(r -> r.eventType().equals(eventType))
                .filter(r -> r.residentId().equals(Optional.of(a)))
                .toList();
    }
}
