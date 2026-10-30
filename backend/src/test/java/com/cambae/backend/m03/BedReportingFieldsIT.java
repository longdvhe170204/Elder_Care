package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m03.api.BedStatisticsApi;
import com.cambae.backend.m03.api.dto.BedStateOnDate;
import com.cambae.backend.m03.api.dto.BedStatus;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trường bổ sung của {@code BedStatisticsApi} cho chỉ tiêu 016 (luật "Đổi api", chủ dự án duyệt 2026-10-27, C review):
 * phòng và lý do giữ chỗ của giường tại một thời điểm (016 FR-020, FR-024 tách hai loại Đang giữ chỗ); giờ bắt đầu thật
 * của khoảng Chờ sẵn sàng khi khoảng tra cứu cắt ngang (016 FR-033, Q-390).
 */
@Transactional
class BedReportingFieldsIT extends StructureTestSupport {
    @Autowired
    BedStatisticsApi statistics;

    @Autowired
    TestClockControl clock;

    Instant at;

    @BeforeEach
    void fixture() {
        clock.advanceBy(Duration.ofDays(1));
        at = time.now().minusSeconds(36000).truncatedTo(ChronoUnit.MICROS);
    }

    @AfterEach
    void reset() {
        clock.reset();
    }

    private void fact(long id, String state, String event, Instant when) {
        jdbc.update(
                "INSERT INTO bed_state_history(bed_id,to_state,event,occurred_at,basis_type,basis_id,floor_area_id,disinfection_required) VALUES(?,?,?,?,'TEST',1,?,?)",
                id,
                state,
                event,
                Timestamp.from(when),
                floor,
                event.equals("CONFIRM_READY") ? Boolean.FALSE : null);
    }

    private long anotherBed(String code) {
        long id = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,?)", id, room, code);
        fact(id, "EMPTY", "CREATE_BED", at);
        return id;
    }

    private ScopeFilter floorScope() {
        return new ScopeFilter(false, Set.of(floor), Set.of(), Set.of());
    }

    @Test
    void FR_024_stateCarriesRoomAndHoldReason_waitingRecordVersusAbsentResident() {
        fact(bed, "EMPTY", "CREATE_BED", at);
        fact(bed, "HELD", "HOLD_WAITING_RECORD", at.plusSeconds(60));
        long absent = anotherBed("ABSENT");
        fact(absent, "OCCUPIED", "ALLOCATION_STARTED", at.plusSeconds(60));
        fact(absent, "HELD", "ABSENT_KEEP_BED", at.plusSeconds(120));
        long empty = anotherBed("EMPTY");

        Map<Long, BedStateOnDate> rows = new HashMap<>();
        statistics.stateAt(floorScope(), at.plusSeconds(600)).forEach(r -> rows.put(r.bedId(), r));

        assertThat(rows.get(bed).state()).isEqualTo(BedStatus.HELD);
        assertThat(rows.get(bed).holdReason()).isEqualTo("WAITING_RECORD");
        assertThat(rows.get(absent).holdReason()).isEqualTo("ABSENT_RESIDENT");
        assertThat(rows.get(empty).holdReason()).isNull();
        assertThat(rows.values()).extracting(BedStateOnDate::roomId).containsOnly(room);
    }

    @Test
    void Q_390_awaitingReadyCarriesRealStart_whenPeriodCutsTheInterval() {
        fact(bed, "EMPTY", "CREATE_BED", at);
        fact(bed, "AWAITING_READY", "REUSE", at.plusSeconds(3600));
        fact(bed, "EMPTY", "CONFIRM_READY", at.plusSeconds(10800));

        var rows = statistics
                .awaitingReady(floorScope(), at.plusSeconds(7200), at.plusSeconds(36000))
                .toList();

        assertThat(rows).singleElement().satisfies(r -> {
            assertThat(r.from()).isEqualTo(at.plusSeconds(7200));
            assertThat(r.startedAt()).isEqualTo(at.plusSeconds(3600));
            assertThat(r.to()).isEqualTo(at.plusSeconds(10800));
        });
    }
}
