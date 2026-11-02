package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.RoomPolicyApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

/** T017: đọc cơ cấu/vị trí thật qua API, trạng thái hiệu lực tính cả tổ tiên. */
class FR_017_StructureApiIT extends StructureTestSupport {
    @Autowired
    ObjectProvider<LocationStructureApi> locations;

    @Autowired
    ObjectProvider<RoomPolicyApi> policies;

    @Test
    void FR_003_locationApiReturnsFullHierarchyAndRespectsInactiveAncestors() {
        var api = locations.getIfAvailable();
        assertThat(api).isNotNull();
        var place = api.bed(bed).orElseThrow();
        assertThat(place.roomId()).isEqualTo(room);
        assertThat(place.floorId()).isEqualTo(floor);
        assertThat(place.areaId()).isEqualTo(area);
        assertThat(api.active(new LocationRef("ROOM", room))).isTrue();
        jdbc.update("UPDATE area SET status='INACTIVE' WHERE id=?", area);
        assertThat(api.active(new LocationRef("ROOM", room))).isFalse();
        assertThat(api.dayCareRestAreaLocation()).isEmpty();
    }

    @Test
    void FR_004_policyApiReadsRoomCopyAndRoomTypeResidencyProperties() {
        var api = policies.getIfAvailable();
        assertThat(api).isNotNull();
        var props = api.allocationProperties(bed);
        assertThat(props.careLevels()).containsExactly(CareLevelCode.BASIC);
        assertThat(props.longTerm()).isTrue();
        assertThat(props.shortTerm()).isTrue();
        assertThat(props.isolated()).isFalse();
        assertThat(props.awaitingResidentTransfer()).isFalse();
    }
}
