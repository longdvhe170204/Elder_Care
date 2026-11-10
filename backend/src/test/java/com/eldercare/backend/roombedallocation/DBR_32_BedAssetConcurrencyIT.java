package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.roombedallocation.internal.allocation.service.AllocationCommands;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationInfectionStatus;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 019 T019, DBR-32: hai transaction thật, dùng BedAssetPort thật; chỉ nguồn 004 và 007 là bản giả. */
@Import(DBR_32_BedAssetConcurrencyIT.Sources.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DBR_32_BedAssetConcurrencyIT extends AssetTestBase {
    @TestConfiguration
    static class Sources {
        @Bean
        @Primary
        FakeAllocationResidencySource assetConcurrencyResidency() {
            return new FakeAllocationResidencySource();
        }

        @Bean
        @Primary
        FakeAllocationInfectionStatus assetConcurrencyInfection() {
            return new FakeAllocationInfectionStatus();
        }
    }

    @Autowired
    AllocationCommands allocations;

    @Autowired
    FakeAllocationResidencySource residency;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.roombedallocation.internal.bed.persistence.BedAssetStateStore bedAllocations;

    @Test
    void DBR_32_damageAndFutureAllocationCannotBothCommitForTheSameBed() throws Exception {
        var actor = new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF, Role.DOCTOR);
        as(actor);
        duties.departmentLead(medical, actor.staffId());
        jdbc.update(
                "INSERT INTO room_type_care_level VALUES(?,'BASIC')",
                jdbc.queryForObject("SELECT room_type_id FROM room WHERE id=?", Long.class, room));
        jdbc.update("UPDATE room SET allowed_care_levels=JSON_ARRAY('BASIC') WHERE id=?", room);
        long bed = TestUsers.nextId(), asset = TestUsers.nextId(), resident = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,?)", bed, room, "G-" + bed);
        jdbc.update(
                "INSERT INTO asset(id,code,type_id,name,department_id,source,bed_id,status,location_type,"
                        + "location_id,floor_area_id,created_at) VALUES(?,?,?,?,?,'BED',?,'READY','ROOM',?,?,?)",
                asset,
                "BED-" + bed,
                bedType,
                "Giường " + bed,
                medical,
                bed,
                room,
                floor,
                LocalDateTime.ofInstant(time.now(), BusinessTime.ZONE));
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,"
                        + "created_at,created_by) VALUES(?,'[Họ tên NCT]','1950-01-01','FEMALE',"
                        + "'ADMISSION_IN_PROGRESS',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                actor.user().accountId());
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
        var initialRead = new CountDownLatch(1);
        var allocationCommitted = new CountDownLatch(1);
        var first = new java.util.concurrent.atomic.AtomicBoolean(true);
        org.mockito.Mockito.doAnswer(call -> {
                    Object snapshot = call.callRealMethod();
                    if (first.compareAndSet(true, false)) {
                        initialRead.countDown();
                        assertThat(allocationCommitted.await(20, TimeUnit.SECONDS))
                                .isTrue();
                    }
                    return snapshot;
                })
                .when(bedAllocations)
                .future(bed, false);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var allocation = executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                try {
                    assertThat(initialRead.await(10, TimeUnit.SECONDS)).isTrue();
                    allocations.create(resident, bed, time.now().plusSeconds(3600), Reason.of("Phân bổ đồng thời"));
                    return 201;
                } catch (BusinessException error) {
                    assertThat(error.errorCode()).isIn("UNMET_CONDITIONS", "ALLOCATION_TIME_OVERLAP");
                    return 409;
                } finally {
                    allocationCommitted.countDown();
                }
            });
            var damage = executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return mvc.perform(post(URL + "/" + asset + "/report-damage")
                                .with(user("asset-concurrency"))
                                .header("Idempotency-Key", "damage-" + TestUsers.nextId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"description\":\"Gãy khung\"}"))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int allocated = allocation.get(30, TimeUnit.SECONDS), damaged = damage.get(30, TimeUnit.SECONDS);
            assertThat(allocated).isIn(201, 409);
            assertThat(damaged).isIn(200, 409);
            assertThat((allocated == 201 ? 1 : 0) + (damaged == 200 ? 1 : 0)).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM bed_allocation WHERE bed_id=? AND status='FUTURE'", Long.class, bed))
                    .isEqualTo(allocated == 201 ? 1L : 0L);
            assertThat(stateOf(asset)).isEqualTo(damaged == 200 ? "DAMAGED" : "READY");
            assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                    .isEqualTo(damaged == 200 ? "UNDER_MAINTENANCE" : "EMPTY");
        } finally {
            start.countDown();
        }
    }

    /** 003 T017 (Q-243): lỗi ở phần tài sản (đã thanh lý, không đổi vị trí) làm lệnh sửa giường rollback cả hai. */
    @Test
    void Q_243_assetFailureRollsBackTheBedRoomChange() throws Exception {
        long bed = TestUsers.nextId(), asset = TestUsers.nextId(), newRoom = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code,state) VALUES(?,?,?,'RETIRED')", bed, room, "G-" + bed);
        jdbc.update(
                "INSERT INTO asset(id,code,type_id,name,department_id,source,bed_id,status,location_type,"
                        + "location_id,floor_area_id,created_at,disposed_at) VALUES(?,?,?,?,?,'BED',?,'DISPOSED','ROOM',?,?,?,?)",
                asset,
                "BED-" + bed,
                bedType,
                "Giường " + bed,
                medical,
                bed,
                room,
                floor,
                LocalDateTime.ofInstant(time.now(), BusinessTime.ZONE),
                LocalDateTime.ofInstant(time.now(), BusinessTime.ZONE));
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) SELECT ?,?,room_type_id,?,"
                        + "'Phòng mới','UNRESTRICTED' FROM room WHERE id=?",
                newRoom,
                floor,
                "P" + newRoom,
                room);
        as(manager);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/beds/" + bed)
                        .with(user("bed-move"))
                        .header("If-Match", com.eldercare.backend.common.web.VersionEtag.of(0))
                        .header("Idempotency-Key", "bed-move-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":" + newRoom + ",\"code\":\"G-" + bed + "\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isConflict())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.errorCode")
                        .value("STATE_CHANGED"));
        assertThat(jdbc.queryForObject("SELECT room_id FROM bed WHERE id=?", Long.class, bed))
                .isEqualTo(room);
        assertThat(jdbc.queryForObject("SELECT version FROM bed WHERE id=?", Long.class, bed))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT location_id FROM asset WHERE id=?", Long.class, asset))
                .isEqualTo(room);
    }
}
