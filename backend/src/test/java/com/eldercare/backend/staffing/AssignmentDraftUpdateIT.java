package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class AssignmentDraftUpdateIT extends AssignmentHttpTestBase {
    @Test
    void FR_031_editPreviewAndSaveReturnTheContractWarningCode() throws Exception {
        long type = com.eldercare.backend.support.TestUsers.nextId();
        tasks.require(
                        type,
                        new com.eldercare.backend.care.api.dto.TaskRequirements(
                                Role.CAREGIVER, Set.of("MISSING_TRAINING")))
                .plan(resident, Set.of(type));
        mvc.perform(post(url() + "/draft-update-preview")
                        .header("Authorization", bearer)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings[0].code").value("TASK_TYPE_REQUIREMENTS_NOT_MET"));
        mvc.perform(edit(body(), "\"0\"", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings[0].code").value("TASK_TYPE_REQUIREMENTS_NOT_MET"));
    }

    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    private String url() {
        return "/api/v1/assignments/" + assignment;
    }

    private MockHttpServletRequestBuilder edit(Map<String, Object> body, String etag, String key) throws Exception {
        return post(url() + "/update-draft")
                .header("Authorization", bearer)
                .header("If-Match", etag)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body));
    }

    @Test
    void FR_025_detailHasVersionWithoutChangingExistingList() throws Exception {
        mvc.perform(get(url()).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.shiftId").value(shift));
        mvc.perform(get("/api/v1/shifts/" + shift + "/assignments").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assignment))
                .andExpect(jsonPath("$[0].version").doesNotExist());
    }

    @Test
    void FR_025_previewExcludesTheOriginalRowAndDoesNotWrite() throws Exception {
        mvc.perform(post(url() + "/draft-update-preview")
                        .header("Authorization", bearer)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings").isArray())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(header().string("ETag", "\"0\""));
        assertThat(jdbc.queryForObject("SELECT version FROM staff_assignment WHERE id=?", Long.class, assignment))
                .isZero();
    }

    @Test
    void FR_025_changeStaffKeepsIdSourceAndCreationMetadata() throws Exception {
        var before =
                jdbc.queryForMap("SELECT created_at,created_by,source FROM staff_assignment WHERE id=?", assignment);
        var value = body();
        value.put("staffId", other.staffId());
        value.put("targetType", "ROOM");
        value.put("targetId", room);
        mvc.perform(edit(value, "\"0\"", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.assignment.id").value(assignment))
                .andExpect(jsonPath("$.assignment.staffId").value(other.staffId()))
                .andExpect(jsonPath("$.assignment.version").value(1))
                .andExpect(jsonPath("$.assignment.status").value("DRAFT"));
        assertThat(jdbc.queryForMap("SELECT created_at,created_by,source FROM staff_assignment WHERE id=?", assignment))
                .isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_assignment WHERE shift_id=?", Integer.class, shift))
                .isOne();
    }

    @Test
    void FR_025_invalidChangePreservesOriginalRecord() throws Exception {
        var value = body();
        value.put("staffId", person(Role.CAREGIVER).staffId());
        mvc.perform(edit(value, "\"0\"", UUID.randomUUID().toString())).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT staff_id FROM staff_assignment WHERE id=?", Long.class, assignment))
                .isEqualTo(worker.staffId());
        assertThat(jdbc.queryForObject("SELECT version FROM staff_assignment WHERE id=?", Long.class, assignment))
                .isZero();
    }

    @Test
    void FR_028_otherPrimaryStillConflictsAndOriginalIsNotDeleted() throws Exception {
        assign(other.staffId(), "ROOM", room, "CAREGIVER", "PRIMARY", false, null);
        var value = body();
        value.put("targetType", "ROOM");
        value.put("targetId", room);
        mvc.perform(edit(value, "\"0\"", UUID.randomUUID().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PRIMARY_ASSIGNMENT_OVERLAP"));
        assertThat(jdbc.queryForObject("SELECT target_type FROM staff_assignment WHERE id=?", String.class, assignment))
                .isEqualTo("FLOOR");
    }

    @Test
    void FR_025_staleVersionIncludesCurrentDetail() throws Exception {
        mvc.perform(edit(body(), "\"0\"", UUID.randomUUID().toString())).andExpect(status().isOk());
        mvc.perform(edit(body(), "\"0\"", UUID.randomUUID().toString()))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.currentValue.id").value(assignment))
                .andExpect(jsonPath("$.currentValue.version").value(1));
    }

    @Test
    void FR_025_publishedAssignmentCannotBeEdited() throws Exception {
        publish();
        mvc.perform(edit(body(), "\"1\"", UUID.randomUUID().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
    }

    @Test
    void FR_025_requiresHeadersAndDoesNotGrantWriteToMembers() throws Exception {
        mvc.perform(post(url() + "/update-draft")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body())))
                .andExpect(status().isBadRequest());
        bearer = token(worker);
        // BR-M15-01: không được xem đối tượng thì che bằng 404 trước khi kiểm quyền sửa.
        mvc.perform(edit(body(), "\"0\"", UUID.randomUUID().toString())).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT version FROM staff_assignment WHERE id=?", Long.class, assignment))
                .isZero();
    }

    @Test
    void FR_025_sameCommandKeyReplaysWithoutSecondUpdate() throws Exception {
        String key = UUID.randomUUID().toString();
        String first = mvc.perform(edit(body(), "\"0\"", key))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        mvc.perform(edit(body(), "\"0\"", key))
                .andExpect(status().isOk())
                .andExpect(content().json(first));
        assertThat(jdbc.queryForObject("SELECT version FROM staff_assignment WHERE id=?", Long.class, assignment))
                .isEqualTo(1L);
    }

    @Test
    void FR_025_concurrentEditorsOnlyOneCanUseTheVersion() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var gate = new CountDownLatch(1);
            var calls = new ArrayList<Future<Integer>>();
            for (int i = 0; i < 2; i++)
                calls.add(pool.submit(() -> {
                    gate.await();
                    return mvc.perform(edit(body(), "\"0\"", UUID.randomUUID().toString()))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            gate.countDown();
            var codes = new ArrayList<Integer>();
            for (var call : calls) codes.add(call.get(30, TimeUnit.SECONDS));
            assertThat(codes).containsExactlyInAnyOrder(200, 412);
        }
        assertThat(jdbc.queryForObject("SELECT version FROM staff_assignment WHERE id=?", Long.class, assignment))
                .isEqualTo(1L);
    }

    @Test
    void FR_025_publicationLockPreventsEditingThePublishedSnapshot() throws Exception {
        try (var pool = Executors.newSingleThreadExecutor()) {
            var pending = new java.util.concurrent.atomic.AtomicReference<Future<Integer>>();
            new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(tx -> {
                jdbc.queryForObject("SELECT id FROM shift_schedule WHERE id=? FOR UPDATE", Long.class, schedule);
                pending.set(pool.submit(() -> mvc.perform(
                                edit(body(), "\"0\"", UUID.randomUUID().toString()))
                        .andReturn()
                        .getResponse()
                        .getStatus()));
                assertThatThrownBy(() -> pending.get().get(150, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);
                publish();
            });
            assertThat(pending.get().get(30, TimeUnit.SECONDS)).isEqualTo(412);
        }
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assignment))
                .isEqualTo("SCHEDULED");
        assertThat(jdbc.queryForObject("SELECT staff_id FROM staff_assignment WHERE id=?", Long.class, assignment))
                .isEqualTo(worker.staffId());
    }
}
