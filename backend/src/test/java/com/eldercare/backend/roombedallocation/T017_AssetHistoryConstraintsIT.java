package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;

/**
 * 019 T017 (data-model mục 0.1, cột "Cách giữ sau gộp"; constitution XII): ràng buộc của lịch sử vị trí và lần bảo trì
 * của tài sản vẫn giữ sau khi gộp vào bảng chung: mỗi tài sản có tối đa một khoảng vị trí mở, khoảng đã đóng không đặt lại
 * {@code ends_at}, dòng lịch sử không xóa; lần bảo trì đã ghi kết quả không sửa.
 */
class T017_AssetHistoryConstraintsIT extends AssetTestBase {

    long assetInUse() throws Exception {
        long id = created(donated("Tặng"));
        command(id, "put-into-use", "{\"location\":{\"type\":\"ROOM\",\"id\":" + room + "}}");
        return id;
    }

    long openLocationRows(long assetId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM status_history WHERE entity_type='M03.ASSET' AND entity_id=?"
                        + " AND attribute='LOCATION' AND ends_at IS NULL",
                Long.class,
                assetId);
    }

    @Test
    void anAssetHasAtMostOneOpenLocationPeriod() throws Exception {
        long id = assetInUse();
        assertThat(openLocationRows(id)).isOne();
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value,starts_at,by_system)"
                                + " VALUES('M03.ASSET',?,'LOCATION','M03','PERIOD','PARKING_LOT',NOW(6),TRUE)",
                        id))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void aClosedLocationPeriodIsNotChangedAndHistoryRowsAreNotDeleted() throws Exception {
        long id = assetInUse();
        long closed = jdbc.queryForObject(
                "SELECT id FROM status_history WHERE entity_type='M03.ASSET' AND entity_id=? AND attribute='LOCATION'"
                        + " AND ends_at IS NOT NULL",
                Long.class,
                id);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE status_history SET ends_at = DATE_ADD(ends_at, INTERVAL 1 DAY) WHERE id=?", closed))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM status_history WHERE id=?", closed))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void aRecordedMaintenanceEventIsNotChanged() throws Exception {
        long id = created(donated("Tặng"));
        command(id, "start-maintenance", "{}");
        command(
                id,
                "record-maintenance-result",
                "{\"result\":\"PASS\",\"date\":\"2026-11-06\",\"content\":\"Thay lọc\",\"performedBy\":\"Tổ kỹ thuật\"}");
        long event = jdbc.queryForObject(
                "SELECT MAX(id) FROM record_event WHERE entity_type='M03.ASSET' AND entity_id=?"
                        + " AND kind='M03.ASSET_MAINTENANCE' AND confirmed_at IS NOT NULL",
                Long.class,
                id);
        assertThatThrownBy(() -> jdbc.update("UPDATE record_event SET content = JSON_OBJECT() WHERE id=?", event))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM record_event WHERE id=?", event))
                .isInstanceOf(DataAccessException.class);
        command(
                        id,
                        "record-maintenance-result",
                        "{\"result\":\"PASS\",\"date\":\"2026-11-06\",\"content\":\"Lần hai\",\"performedBy\":\"Tổ\"}")
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isConflict());
    }
}
