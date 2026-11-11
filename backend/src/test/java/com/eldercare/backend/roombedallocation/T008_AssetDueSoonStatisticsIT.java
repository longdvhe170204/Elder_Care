package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.AssetStatisticsApi;
import com.eldercare.backend.roombedallocation.api.dto.AssetFlag;
import com.eldercare.backend.roombedallocation.api.dto.OverdueAssetView;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 016 T008 (FR-020 dòng "Tài sản quá hạn": số tài sản tới mốc nhắc CFG-M03-09; BR-M03-17): bản thật
 * {@code AssetStatisticsApi.dueSoon} trả hạn trong [ngày xem, ngày xem + CFG-M03-09] chưa qua hạn, không gồm tài sản Đã
 * thanh lý; tầng riêng của ca để không lẫn dữ liệu sót.
 */
@Transactional
class T008_AssetDueSoonStatisticsIT extends MySqlIntegrationTestBase {

    @Autowired
    AssetStatisticsApi statistics;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Test
    void FR_020_dueSoonWithinReminderWindow_notOverdue_notDisposed() {
        LocalDate today = time.today();
        long floor = TestUsers.nextId();
        long soon = asset(floor, "READY", today.plusDays(30), null, null);
        long todayDue = asset(floor, "READY", null, today, null);
        asset(floor, "READY", null, null, today.plusDays(31));
        asset(floor, "READY", today.minusDays(1), null, null);
        asset(floor, "DISPOSED", today.plusDays(5), null, null);
        asset(TestUsers.nextId(), "READY", today.plusDays(5), null, null);

        var rows = statistics
                .dueSoon(new ScopeFilter(false, Set.of(floor), Set.of(), Set.of()), today)
                .toList();

        assertThat(rows)
                .extracting(OverdueAssetView::assetId, OverdueAssetView::flag, OverdueAssetView::dueOn)
                .containsExactlyInAnyOrder(
                        tuple(soon, AssetFlag.MAINTENANCE_OVERDUE, today.plusDays(30)),
                        tuple(todayDue, AssetFlag.VEHICLE_INSPECTION_OVERDUE, today));
    }

    private long asset(long floor, String status, LocalDate maintenance, LocalDate inspection, LocalDate insurance) {
        long id = TestUsers.nextId();
        Long type = jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type='M03.ASSET_TYPE' AND code='VEHICLE'", Long.class);
        jdbc.update(
                "INSERT INTO asset(id,code,type_id,name,department_id,source,source_description,status,location_type,"
                        + "location_id,floor_area_id,maintenance_due_on,vehicle_inspection_due_on,insurance_due_on,disposed_at,"
                        + "created_at) VALUES(?,?,?,?,1,'DONATED','Tặng',?,'FLOOR',?,?,?,?,?,?,?)",
                id,
                "TS-" + id,
                type,
                "Tài sản " + id,
                status,
                floor,
                floor,
                maintenance,
                inspection,
                insurance,
                "DISPOSED".equals(status) ? Timestamp.from(time.now()) : null,
                Timestamp.from(time.now()));
        return id;
    }
}
