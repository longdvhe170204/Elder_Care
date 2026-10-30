package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

/** T026 phần đọc: vị trí theo thời điểm, phạm vi và dữ liệu nguồn mâu thuẫn. */
@Transactional
class FR_017_AllocationQueriesIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    private ResidentLocationApi locations() {
        var beans = context.getBeansOfType(ResidentLocationApi.class);
        assertThat(beans).as("Cần bản thật ResidentLocationApi của M03").hasSize(1);
        return beans.values().iterator().next();
    }

    private long allocation(long resident, String status, Instant start, Instant end) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source) VALUES(?,?,?,?,?,?,'BASIC','CURRENT')",
                id,
                resident,
                bed,
                status,
                Timestamp.from(start),
                end == null ? null : Timestamp.from(end));
        return id;
    }

    @Test
    void FR_017_currentLocationBatchAndScopeComeFromActiveAllocationNotFutureReservation() {
        var locations = locations();
        var source = (BedAllocationApi) locations;
        long resident = TestUsers.nextId();
        long futureResident = TestUsers.nextId();
        Instant now = time.now();
        long id = allocation(resident, "ACTIVE", now.minusSeconds(60), null);
        var position = locations.current(resident).orElseThrow();
        assertThat(position.bedId()).isEqualTo(bed);
        assertThat(position.roomId()).isEqualTo(room);
        assertThat(position.floorId()).isEqualTo(floor);
        assertThat(position.areaId()).isEqualTo(area);
        assertThat(source.openAt(resident, now).orElseThrow().allocationId()).isEqualTo(id);
        assertThat(locations.current(List.of(resident, futureResident))).containsOnlyKeys(resident);
        assertThat(source.residentsIn(new ScopeFilter(false, Set.of(floor), Set.of(), Set.of()), now))
                .containsExactly(resident);
        assertThat(source.residentsIn(new ScopeFilter(false, Set.of(), Set.of(room), Set.of()), now))
                .containsExactly(resident);
        assertThat(source.residentsIn(new ScopeFilter(false, Set.of(), Set.of(), Set.of(resident)), now))
                .containsExactly(resident);
        assertThat(source.residentsIn(new ScopeFilter(false, Set.of(), Set.of(), Set.of()), now))
                .isEmpty();
    }

    @Test
    void FR_017_closedIntervalIncludesStartExcludesEndAndFutureDoesNotGrantScope() {
        var locations = locations();
        var source = (BedAllocationApi) locations;
        long resident = TestUsers.nextId();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = start.plusSeconds(3600);
        allocation(resident, "CLOSED", start, end);
        assertThat(locations.at(resident, start)).isPresent();
        assertThat(source.openAt(resident, end)).isEmpty();
        long future = allocation(resident, "FUTURE", time.now().plusSeconds(86400), null);
        assertThat(locations.current(resident)).isEmpty();
        assertThat(locations.currentOrFutureAllocation(resident).orElseThrow().allocationId())
                .isEqualTo(future);
        assertThat(source.openAt(resident, time.now().plusSeconds(172800))).isEmpty();
    }

    @Test
    void FR_025a_withoutBedMarkerIsIndependentFromFutureAllocation() {
        var locations = locations();
        long resident = TestUsers.nextId();
        assertThat(locations.hasNoBed(resident)).isFalse();
        jdbc.update(
                "INSERT INTO resident_position(resident_id,without_bed_since) VALUES(?,?)",
                resident,
                Timestamp.from(time.now()));
        assertThat(locations.hasNoBed(resident)).isTrue();
        assertThat(locations.current(resident)).isEmpty();
    }

    @Test
    void FR_041b_correctedAllocationsSharingOneBedFailWithM03SourceInsteadOfChoosingOne() {
        var source = (BedAllocationApi) locations();
        long first = TestUsers.nextId();
        long second = TestUsers.nextId();
        Instant now = time.now();
        allocation(first, "ACTIVE", now.minusSeconds(60), null);
        long originalBed = bed;
        bed = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G2')", bed, room);
        long allocation = allocation(second, "ACTIVE", now.minusSeconds(60), null);
        jdbc.update(
                "INSERT INTO correction(record_type,original_record_id,type,corrected_content,reason,actor_id,creation_method,recorded_at,sequence_no) VALUES('M03.BED_ALLOCATION',?,'AMEND_CONTENT',?,'Kiểm lỗi nguồn',1,'A',?,1)",
                allocation,
                "{\"bedId\":" + originalBed + "}",
                Timestamp.from(now));
        assertThatThrownBy(() -> source.openAt(first, now))
                .isInstanceOfSatisfying(
                        InvalidPermissionDataException.class,
                        e -> assertThat(e.source()).isEqualTo("M03.BED_ALLOCATION"));
        assertThatThrownBy(() -> source.residentsIn(ScopeFilter.FACILITY, now))
                .isInstanceOf(InvalidPermissionDataException.class);
    }
}
