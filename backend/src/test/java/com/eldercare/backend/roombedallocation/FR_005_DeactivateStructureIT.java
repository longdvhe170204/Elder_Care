package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.roombedallocation.api.spi.LocationReferenceView;
import com.eldercare.backend.roombedallocation.internal.structure.domain.StructureKind;
import org.junit.jupiter.api.Test;

/** T011: ngừng hiệu lực/xóa xét con, giường và cả lịch sử phân công. */
class FR_005_DeactivateStructureIT extends StructureTestSupport {
    @Test
    void FR_005_roomMustWaitUntilItsFloorIsReactivated() {
        jdbc.update("UPDATE room SET status='INACTIVE' WHERE id=?", room);
        jdbc.update("UPDATE floor SET status='INACTIVE' WHERE id=?", floor);
        assertThatThrownBy(() -> structures.reactivate(StructureKind.ROOM, room, "\"0\"", Reason.of("Dùng lại phòng")))
                .isInstanceOf(ConditionNotMetException.class);
    }

    @Test
    void FR_005_floorMustWaitUntilItsAreaIsReactivated() {
        jdbc.update("UPDATE floor SET status='INACTIVE' WHERE id=?", floor);
        jdbc.update("UPDATE area SET status='INACTIVE' WHERE id=?", area);
        assertThatThrownBy(() -> structures.reactivate(StructureKind.FLOOR, floor, "\"0\"", Reason.of("Dùng lại tầng")))
                .isInstanceOf(ConditionNotMetException.class);
    }

    @Test
    void Q_260_suspendedBedStillBlocksRoomDeactivation() {
        jdbc.update("UPDATE bed SET state='SUSPENDED',suspension_reason='Tạm ngừng' WHERE id=?", bed);
        assertThatThrownBy(() -> structures.deactivate(StructureKind.ROOM, room, "\"0\"", Reason.of("Ngừng phòng")))
                .isInstanceOfSatisfying(
                        ConditionNotMetException.class,
                        e -> assertThat(e.unmetConditions())
                                .anySatisfy(c -> assertThat(c.description()).contains(Long.toString(bed))));
    }

    @Test
    void FR_005_currentAssignmentBlocksDeactivationAndHistoryBlocksDeletion() {
        var location = new LocationRef("AREA", area);
        references.reference(location, new LocationReferenceView("ASSIGNMENT", 123, "Phân công 123"));
        jdbc.update("UPDATE floor SET status='INACTIVE' WHERE id=?", floor);
        assertThatThrownBy(() -> structures.deactivate(StructureKind.AREA, area, "\"0\"", Reason.of("Ngừng khu")))
                .isInstanceOfSatisfying(
                        ConditionNotMetException.class,
                        e -> assertThat(e.unmetConditions())
                                .anySatisfy(c -> assertThat(c.description()).contains("123")));
        references.clearCurrent(location);
        assertThatThrownBy(() -> structures.delete(StructureKind.AREA, area, "\"0\""))
                .isInstanceOf(ConditionNotMetException.class);
    }

    @Test
    void FR_005_activeChildBlocksParentDeactivation() {
        assertThatThrownBy(() -> structures.deactivate(StructureKind.AREA, area, "\"0\"", Reason.of("Ngừng khu")))
                .isInstanceOfSatisfying(
                        ConditionNotMetException.class,
                        e -> assertThat(e.unmetConditions())
                                .anySatisfy(c -> assertThat(c.description()).contains(Long.toString(floor))));
    }
}
