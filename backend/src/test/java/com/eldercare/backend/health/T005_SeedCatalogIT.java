package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Danh mục khởi tạo Q-398 (nghiep-vu mục 9.3, 10.1): đúng bảng chốt, không thêm loại hay chỉ số ngoài bảng. */
class T005_SeedCatalogIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void Q_398_vitalSignAndIncidentTypeSeedsMatchApprovedTables() {
        var problems = new ArrayList<String>();
        // code | unit | vitalSign | thành phần:chữ số thập phân
        for (var row : List.of(
                "BLOOD_PRESSURE|mmHg|true|SYSTOLIC:0,DIASTOLIC:0",
                "HEART_RATE|nhịp/phút|true|VALUE:0",
                "BODY_TEMPERATURE|°C|true|VALUE:1",
                "SPO2|%|true|VALUE:0",
                "BLOOD_GLUCOSE|mmol/L|false|VALUE:1",
                "WEIGHT|kg|false|VALUE:1")) {
            var f = row.split("[|]");
            var item = jdbc.queryForList(
                    "SELECT id, JSON_UNQUOTE(JSON_EXTRACT(attributes,'$.unit')) u,"
                            + " JSON_EXTRACT(attributes,'$.vitalSign') v, status FROM catalog_item"
                            + " WHERE catalog_type='M06.VITAL_SIGN' AND code=?",
                    f[0]);
            if (item.size() != 1) {
                problems.add(f[0] + ": thiếu chỉ số");
                continue;
            }
            var r = item.get(0);
            if (!f[1].equals(r.get("u"))) problems.add(f[0] + ": đơn vị " + r.get("u"));
            if (!f[2].equals(String.valueOf(r.get("v")))) problems.add(f[0] + ": sinh hiệu " + r.get("v"));
            var parts = jdbc.queryForList(
                    "SELECT CONCAT(code, ':', JSON_UNQUOTE(JSON_EXTRACT(attributes,'$.decimalPlaces'))) c"
                            + " FROM catalog_item WHERE catalog_type='M06.VITAL_SIGN_COMPONENT' AND parent_id=?"
                            + " ORDER BY sort_order",
                    String.class,
                    r.get("id"));
            if (!String.join(",", parts).equals(f[3])) problems.add(f[0] + ": thành phần " + parts);
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type='M06.VITAL_SIGN'"
                                + " AND code IN ('BLOOD_PRESSURE','HEART_RATE','BODY_TEMPERATURE','SPO2','BLOOD_GLUCOSE','WEIGHT')",
                        Integer.class))
                .isEqualTo(6);
        for (var row : List.of(
                "FALL|CRITICAL",
                "MISSING_OR_NOT_RETURNED|CRITICAL",
                "BREATHING_OR_CONSCIOUSNESS|CRITICAL",
                "MEDICATION|MEDIUM",
                "CHOKING_OR_AIRWAY_OBSTRUCTION|CRITICAL",
                "WRONG_MEAL_SERVED|MEDIUM",
                "BEHAVIOR|MEDIUM",
                "OTHER_INJURY|MEDIUM",
                "OTHER_HEALTH_CARE|MEDIUM")) {
            var f = row.split("[|]");
            var attrs = jdbc.queryForList(
                    "SELECT JSON_UNQUOTE(JSON_EXTRACT(attributes,'$.defaultLevel')) l,"
                            + " JSON_EXTRACT(attributes,'$.nonHealth') n,"
                            + " JSON_EXTRACT(attributes,'$.allowsNoResident') a"
                            + " FROM catalog_item WHERE catalog_type='M05.INCIDENT_TYPE' AND code=?",
                    f[0]);
            if (attrs.size() != 1) {
                problems.add(f[0] + ": thiếu loại sự cố");
                continue;
            }
            var r = attrs.get(0);
            if (!f[1].equals(r.get("l"))
                    || !"false".equals(String.valueOf(r.get("n")))
                    || !"false".equals(String.valueOf(r.get("a")))) problems.add(f[0] + ": thuộc tính " + r);
        }
        assertThat(problems).isEmpty();
    }
}
