package com.eldercare.backend.admission;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.approval.scheduling.ApplyDueRequestsJob;
import com.eldercare.backend.resident.api.AssessmentApi;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationships;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Nền chung cho test thay đổi lưu trú của 004 US5 (T042, T043, T075): hợp đồng Hiệu lực dựng qua HTTP như {@link
 * ContractTestBase}, yêu cầu thay đổi lập qua {@code POST /residency-change-requests}, duyệt qua các endpoint của 000, áp
 * dụng bằng job {@link ApplyDueRequestsJob} với đồng hồ giả lập. Lớp con khai {@code @AutoConfigureMockMvc} và {@code
 * @WithMockUser}.
 */
abstract class ChangeTestBase extends ContractTestBase {

    protected static final String REASON = "Gia đình đề nghị";

    @Autowired
    protected ApplyDueRequestsJob applyJob;

    @Autowired
    protected FakeFamilyRelationships families;

    @Autowired
    protected AssessmentApi assessmentApi;

    /** Người cao tuổi có người đại diện và hợp đồng nội trú dài hạn Hiệu lực (Cơ bản, dịch vụ chăm sóc, suất ăn thuộc gói). */
    protected record Setup(long residentId, long contractId) {}

    /** Người đại diện của lần {@link #activeSetup()} gần nhất. */
    protected Long lastRepresentative;

    protected Setup activeSetup() throws Exception {
        long residentId = residentWithRepresentative();
        lastRepresentative = representativeOf(residentId);
        ObjectNode body = body(residentId);
        ((ArrayNode) body.get("services")).add(line(meal));
        return new Setup(residentId, activeContract(residentId, body));
    }

    /**
     * Thân lập yêu cầu, kèm xác nhận của Hành chính với người đại diện (mỗi lần một tệp bằng chứng mới, tệp chỉ gắn được
     * một lần).
     */
    protected ObjectNode request(long contractId, String changeTypes, String newValue, LocalDate effective) {
        ObjectNode body =
                (ObjectNode) JSON.readTree("""
                {"contractId": %d, "changeTypes": %s, "newValue": %s, "requestedEffectiveDate": "%s",
                 "reason": "%s"}""".formatted(contractId, changeTypes, newValue, effective, REASON));
        body.set("representativeConfirmation", confirmation(today(), representativeOfContract(contractId)));
        if (changeTypes.contains("CARE_LEVEL") && body.path("newValue").hasNonNull("careLevel")) {
            assessedAs(
                    contractId,
                    CareLevel.valueOf(body.get("newValue").get("careLevel").asString()));
        }
        return body;
    }

    /**
     * FR-043j (R20): yêu cầu đổi mức cần lần đánh giá gần nhất đã xác nhận ra đúng mức mới. Bác sĩ xác nhận một lần đánh
     * giá (lần đầu nếu chưa có, không thì đánh giá lại) ra mức {@code level} khi lần gần nhất chưa ra mức này.
     */
    protected void assessedAs(long contractId, CareLevel level) {
        long residentId = jdbc.queryForObject("SELECT resident_id FROM contract WHERE id = ?", Long.class, contractId);
        var latest = assessmentApi.latestConfirmed(residentId);
        if (latest.isPresent() && latest.get().careLevel() == level) {
            return;
        }
        boolean initialDone = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM assessment WHERE resident_id = ? AND type = 'INITIAL' AND status = 'CONFIRMED'",
                        Integer.class,
                        residentId)
                > 0;
        // confirmed() đổi người dùng sang Điều dưỡng, Bác sĩ; runAs trả lại người dùng trước đó
        user.runAs(
                doctor.user(),
                () -> confirmed(
                        residentId,
                        initialDone ? AssessmentType.REASSESSMENT : AssessmentType.INITIAL,
                        level,
                        "Đánh giá lại thử",
                        null));
    }

    /**
     * Xác nhận gặp trực tiếp người đại diện ngày {@code on}, kèm tệp bằng chứng mới; người đã xác nhận là người đại diện
     * của người cao tuổi dựng gần nhất bằng {@link #activeSetup()} (Q-415).
     */
    protected ObjectNode confirmation(LocalDate on) {
        return confirmation(on, lastRepresentative);
    }

    protected ObjectNode confirmation(LocalDate on, Long representativeFamilyMemberId) {
        ObjectNode node = JSON.createObjectNode();
        node.put("method", "IN_PERSON");
        node.put("confirmedOn", on.toString());
        node.put("evidenceFileId", scan());
        if (representativeFamilyMemberId != null) {
            node.put("representativeFamilyMemberId", representativeFamilyMemberId);
        }
        return node;
    }

    /** Người đại diện của người cao tuổi của hợp đồng, nếu dựng bằng {@link #residentWithRepresentative()}. */
    protected Long representativeOfContract(long contractId) {
        long residentId = jdbc.queryForObject("SELECT resident_id FROM contract WHERE id = ?", Long.class, contractId);
        try {
            return representativeOf(residentId);
        } catch (IllegalStateException none) {
            return null;
        }
    }

    protected ResultActions createRaw(String path, ObjectNode body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body.toString()));
    }

    /** Hành chính lập yêu cầu thay đổi (201); trả {@code ResidencyChangeRequest}. */
    protected JsonNode createChange(ObjectNode body) throws Exception {
        user.set(office.user());
        MvcResult result = createRaw("/api/v1/residency-change-requests", body)
                .andExpect(status().isCreated())
                .andReturn();
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    protected JsonNode getChange(long approvalRequestId) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/residency-change-requests/" + approvalRequestId))
                .andExpect(status().isOk())
                .andReturn();
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    protected String requestEtag(long requestId) throws Exception {
        return mvc.perform(get("/api/v1/approval-requests/" + requestId))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
    }

    protected ResultActions generic(long requestId, String command, String body) throws Exception {
        return mvc.perform(post("/api/v1/approval-requests/" + requestId + "/" + command)
                .with(csrf())
                .header("If-Match", requestEtag(requestId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    /** Gửi duyệt bởi người lập (Hành chính). */
    protected void submit(long requestId) throws Exception {
        user.set(office.user());
        generic(requestId, "submit", "{}").andExpect(status().isOk());
    }

    protected ResultActions approveRaw(long requestId) throws Exception {
        return generic(requestId, "approve", "{\"comment\":\"Đồng ý\"}");
    }

    /** Quản lý viện duyệt (200). */
    protected void managerApproves(long requestId) throws Exception {
        user.set(manager.user());
        approveRaw(requestId).andExpect(status().isOk());
        user.set(office.user());
    }

    /**
     * Lập, gửi duyệt rồi Quản lý viện duyệt; phụ lục Chờ ký (không phải thay đổi nhỏ, FR-043d) thì Hành chính ghi nhận bản
     * đã ký ngay hôm duyệt để ngày hiệu lực không đổi. Trả mã yêu cầu phê duyệt.
     */
    protected long createAndApprove(ObjectNode body) throws Exception {
        long id = createApproveUnsigned(body);
        if ("PENDING_SIGNATURE".equals(addendumStatus(id))) {
            sign(id, today()).andExpect(status().isOk());
            // Trước FR-043d, duyệt khi ngày hiệu lực đã tới thì áp dụng ngay; nay phụ lục áp dụng ở lượt job kế tiếp
            // sau khi ký
            applyJob.run();
        }
        return id;
    }

    /** Lập, gửi duyệt, Quản lý viện duyệt; không ghi nhận ký (FR-043d). */
    protected long createApproveUnsigned(ObjectNode body) throws Exception {
        long id = createChange(body).get("approvalRequestId").asLong();
        submit(id);
        managerApproves(id);
        return id;
    }

    /** Trạng thái phụ lục của yêu cầu ở cơ sở dữ liệu; chưa có phụ lục thì {@code null}. */
    protected String addendumStatus(long requestId) {
        return jdbc
                .query(
                        "SELECT addendum_status FROM contract_version WHERE kind = 'ADDENDUM' AND approval_request_id = ?",
                        (rs, i) -> rs.getString(1),
                        requestId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    /** Hành chính ghi nhận phụ lục đã ký ngày {@code signedDate}, người ký là người đại diện, kèm bản scan mới. */
    protected ResultActions sign(long requestId, LocalDate signedDate) throws Exception {
        long residentId =
                jdbc.queryForObject("SELECT resident_id FROM approval_request WHERE id = ?", Long.class, requestId);
        return signRaw(requestId, signedDate, representativeOf(residentId), null);
    }

    protected ResultActions signRaw(long requestId, LocalDate signedDate, long familyMemberId, Long facilityStaffId)
            throws Exception {
        user.set(office.user());
        ObjectNode body = JSON.createObjectNode();
        body.put("signedDate", signedDate.toString());
        body.put("signedByFamilyMemberId", familyMemberId);
        if (facilityStaffId != null) {
            body.put("facilitySignatoryStaffId", facilityStaffId);
        }
        body.put("fileId", scan());
        return mvc.perform(post("/api/v1/residency-change-requests/" + requestId + "/addendum-signature")
                .with(csrf())
                .header("If-Match", requestEtag(requestId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()));
    }

    protected JsonNode addenda(long contractId) throws Exception {
        user.set(office.user());
        MvcResult result = mvc.perform(get("/api/v1/contracts/" + contractId + "/addenda"))
                .andExpect(status().isOk())
                .andReturn();
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    protected String requestStatus(long requestId) {
        return jdbc.queryForObject("SELECT status FROM approval_request WHERE id = ?", String.class, requestId);
    }

    /** Đưa đồng hồ tới 12:00 ngày {@code day} rồi chạy một đợt áp dụng yêu cầu đến hạn. */
    protected void applyDueOn(LocalDate day) {
        noonOf(day);
        applyJob.run();
    }

    /** Phiên bản nội dung mới nhất của hợp đồng (JSON). */
    protected JsonNode latestContent(long contractId) {
        String content = jdbc.queryForObject(
                "SELECT content FROM contract_version WHERE kind = 'CONTENT' AND contract_id = ? ORDER BY sequence_no DESC LIMIT 1",
                String.class,
                contractId);
        return JSON.readTree(content);
    }

    protected int versionCount(long contractId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM contract_version WHERE kind = 'CONTENT' AND contract_id = ?",
                Integer.class,
                contractId);
    }
    // ---- Giường, phân bổ thật của 003 (T035, T043) ------------------------------------------------------------------

    /** Phòng thật (khu, tầng, loại phòng mức Cơ bản, không giới hạn giới tính); trả mã phòng. */
    protected long realRoom(boolean servesShortTerm) {
        long area = com.eldercare.backend.support.TestUsers.nextId();
        long floor = com.eldercare.backend.support.TestUsers.nextId();
        long type = com.eldercare.backend.support.TestUsers.nextId();
        long room = com.eldercare.backend.support.TestUsers.nextId();
        jdbc.update("INSERT INTO area(id,code,name) VALUES(?,?,?)", area, "A" + area, "Khu " + area);
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)", floor, area, "F" + floor, "Tầng " + floor);
        jdbc.update(
                "INSERT INTO room_type(id,name,standard_beds,serves_long_term,serves_short_term) VALUES(?,?,2,TRUE,?)",
                type,
                "Loại " + type,
                servesShortTerm);
        jdbc.update("UPDATE room_type SET allowed_care_levels=JSON_ARRAY('BASIC') WHERE id=?", type);
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(?,?,?,?,?,'UNRESTRICTED')",
                room,
                floor,
                type,
                "R" + room,
                "Phòng " + room);
        jdbc.update("UPDATE room SET allowed_care_levels=JSON_ARRAY('BASIC') WHERE id=?", room);
        return room;
    }

    /** Giường thật trong phòng, trạng thái cho trước; trả mã giường. */
    protected long realBed(long roomId, String state) {
        long bed = com.eldercare.backend.support.TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code,state) VALUES(?,?,?,?)", bed, roomId, "G" + bed, state);
        return bed;
    }

    /** Phân bổ Hiệu lực thật của người này ở giường (bắt đầu 10 ngày trước); trả mã phân bổ. */
    protected long activeAllocation(long residentId, long bedId) {
        cancelFixtureAllocation(residentId);
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source)"
                        + " VALUES(?,?,'ACTIVE',?,'BASIC','CURRENT')",
                residentId,
                bedId,
                java.sql.Timestamp.from(today().minusDays(10)
                        .atStartOfDay(com.eldercare.backend.common.time.BusinessTime.ZONE)
                        .toInstant()));
        return jdbc.queryForObject(
                "SELECT id FROM bed_allocation WHERE resident_id=? AND bed_id=?", Long.class, residentId, bedId);
    }
}
