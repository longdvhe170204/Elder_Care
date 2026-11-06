package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * 012 T039 (SC-001; quickstart mục 5, Q13 → Q17; FR-021 → FR-024; BR-M10-03): bộ 100 lượt đón trộn đủ loại: 30 người
 * trong danh sách, 15 người ngoài danh sách, 15 giấy tờ không khớp, 10 ngoại lệ Hiệu lực, 5 dùng lại ngoại lệ đã dùng, 15
 * người vừa bị bỏ khỏi danh sách, 10 ngoại lệ đã hết hạn. Chỉ 40 lượt có bản ghi đón; 60 lượt bị chặn đều ghi lần chặn;
 * không bản ghi nào của người không thuộc danh sách mà không có ngoại lệ Hiệu lực; mọi bản ghi có đủ người đón, căn cứ,
 * giấy tờ đã đối chiếu, thời điểm, nhân viên bàn giao.
 */
class SC_001_HundredPickupsIT extends PickupTestBase {

    private static final String[] LISTED_TYPES = {"TEMPORARY_ABSENCE", "FAMILY_OUTING", "DAY_CARE_DEPARTURE"};

    private int attempts;
    private int created;
    private int blocked;

    private String outsiderDoc(int n) {
        return String.format("0791002%05d", n);
    }

    private void expectCreated(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        result.andExpect(status().isCreated());
        attempts++;
        created++;
    }

    private void expectBlocked(org.springframework.test.web.servlet.ResultActions result, String errorCode)
            throws Exception {
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value(errorCode));
        attempts++;
        blocked++;
    }

    @Test
    void SC_001_hundredPickups_onlyListedOrEffectiveException_leave_eachRecordComplete() throws Exception {
        int blocksBefore = blocks();
        String manager = staffSession(Role.NURSING_HOME_MANAGER);

        // Đợt 1, trộn theo thứ tự cố định: trong danh sách, ngoài danh sách, giấy tờ sai, ngoại lệ Hiệu lực
        List<Integer> kinds = new ArrayList<>();
        kinds.addAll(Collections.nCopies(30, 0));
        kinds.addAll(Collections.nCopies(15, 1));
        kinds.addAll(Collections.nCopies(15, 2));
        kinds.addAll(Collections.nCopies(10, 3));
        Collections.shuffle(kinds, new Random(12));
        List<Long> usedExceptions = new ArrayList<>();
        List<String> usedExceptionDocs = new ArrayList<>();
        int n = 0;
        for (int kind : kinds) {
            n++;
            switch (kind) {
                case 0 -> expectCreated(pickup(office, LISTED_TYPES[n % 3], rMember, R_DOC));
                case 1 -> expectBlocked(pickup(office, "FAMILY_OUTING", xMember, X_DOC), "PICKUP_BLOCKED");
                case 2 -> expectBlocked(pickup(office, "TEMPORARY_ABSENCE", mMember, "0000" + n), "PICKUP_BLOCKED");
                default -> {
                    String doc = outsiderDoc(n);
                    long exception =
                            id(createException(office, "Người đón hộ " + n, doc).andExpect(status().isCreated()));
                    // Xen kẽ: người đại diện xác nhận qua cổng hoặc Quản lý viện duyệt kèm lý do (FR-024)
                    if (usedExceptions.size() % 2 == 0) {
                        exceptionCommand(family(rMember), exception, "confirm", "{}")
                                .andExpect(status().isOk());
                    } else {
                        exceptionCommand(manager, exception, "approve", "{\"reason\": \"Đã gọi người đại diện\"}")
                                .andExpect(status().isOk());
                    }
                    expectCreated(pickupWithException(office, "FAMILY_OUTING", exception, doc));
                    usedExceptions.add(exception);
                    usedExceptionDocs.add(doc);
                }
            }
        }

        // Đợt 2: dùng lại ngoại lệ đã dùng
        for (int i = 0; i < 5; i++) {
            expectBlocked(
                    pickupWithException(office, "FAMILY_OUTING", usedExceptions.get(i), usedExceptionDocs.get(i)),
                    "PICKUP_BLOCKED");
        }

        // Đợt 3: M vừa bị người đại diện bỏ khỏi danh sách được phép đón
        requestRight(family(rMember), a, m, "PICKUP_ALLOWED", false).andExpect(status().isCreated());
        for (int i = 0; i < 15; i++) {
            expectBlocked(pickup(office, "TEMPORARY_ABSENCE", mMember, M_DOC), "PICKUP_BLOCKED");
        }

        // Đợt 4: ngoại lệ Hiệu lực quá CFG-M10-08 (4 giờ)
        List<Long> expiring = new ArrayList<>();
        List<String> expiringDocs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String doc = outsiderDoc(500 + i);
            long exception =
                    id(createException(office, "Người đón muộn " + i, doc).andExpect(status().isCreated()));
            exceptionCommand(family(rMember), exception, "confirm", "{}").andExpect(status().isOk());
            expiring.add(exception);
            expiringDocs.add(doc);
        }
        at(time.now().plus(Duration.ofHours(4)).plusSeconds(60));
        String laterOffice = staffSession(Role.OFFICE_STAFF);
        for (int i = 0; i < 10; i++) {
            expectBlocked(
                    pickupWithException(laterOffice, "TEMPORARY_ABSENCE", expiring.get(i), expiringDocs.get(i)),
                    "PICKUP_EXCEPTION_EXPIRED");
        }

        assertThat(attempts).isEqualTo(100);
        assertThat(created).isEqualTo(40);
        assertThat(blocked).isEqualTo(60);
        assertThat(blocks()).isEqualTo(blocksBefore + 60);

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT family_member_id, basis, exception_id, picker_name, id_document_number, at,"
                        + " handover_staff_id FROM pickup_record WHERE resident_id = ?",
                a);
        assertThat(rows).hasSize(40);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.get("picker_name")).isNotNull();
            assertThat(row.get("id_document_number")).isNotNull();
            assertThat(row.get("at")).isNotNull();
            assertThat(((Number) row.get("handover_staff_id")).longValue()).isEqualTo(handoverStaff);
            if ("PICKUP_LIST".equals(row.get("basis"))) {
                assertThat(((Number) row.get("family_member_id")).longValue()).isEqualTo(rMember);
                assertThat(row.get("id_document_number")).isEqualTo(R_DOC);
            } else {
                assertThat(row.get("basis")).isIn("REPRESENTATIVE_EXCEPTION", "MANAGER_EXCEPTION");
                assertThat(usedExceptions).contains(((Number) row.get("exception_id")).longValue());
            }
        });
        assertThat(rows).filteredOn(r -> "PICKUP_LIST".equals(r.get("basis"))).hasSize(30);
        assertThat(rows)
                .filteredOn(r -> "REPRESENTATIVE_EXCEPTION".equals(r.get("basis")))
                .hasSize(5);
        assertThat(rows)
                .filteredOn(r -> "MANAGER_EXCEPTION".equals(r.get("basis")))
                .hasSize(5);
    }
}
