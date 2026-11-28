package com.eldercare.backend.admission.internal.change.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.internal.change.domain.ResidencyChangeContent;
import com.eldercare.backend.resident.api.dto.CareLevel;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 004 FR-042, 003 FR-030 (chủ dự án chốt 2026-11-16): mọi phụ lục có giường mới (đổi giường, bán trú → nội trú kèm
 * giường) đặt {@code roomTypeId} của nội dung hợp đồng theo loại phòng của giường mới, để phí lưu trú và dấu chờ về phòng
 * cùng giá đọc đúng loại phòng.
 */
class FR_042_AddendumRoomTypeTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** Giường 7 thuộc loại phòng 30; giường khác không tra được loại phòng. */
    private static final AddendumContent.Pricing PRICING = new AddendumContent.Pricing() {
        @Override
        public Optional<Long> careServiceOf(CareLevel level) {
            return Optional.empty();
        }

        @Override
        public Optional<BigDecimal> unitPrice(long serviceId) {
            return Optional.empty();
        }

        @Override
        public boolean isCareService(long serviceId) {
            return false;
        }

        @Override
        public Optional<Long> roomTypeOfBed(long bedId) {
            return bedId == 7 ? Optional.of(30L) : Optional.empty();
        }
    };

    private static JsonNode base(String residenceType, String roomTypeId, String bedId) {
        return JSON.readTree("""
                {"residenceType": "%s", "roomTypeId": %s, "bedId": %s, "services": [], "arrivalSchedule": []}""".formatted(residenceType, roomTypeId, bedId));
    }

    private static ResidencyChangeContent change(String changeTypes, String newValue) {
        return ResidencyChangeContent.parse(JSON.readTree("""
                {"contractId": 1, "changeTypes": %s, "newValue": %s}""".formatted(changeTypes, newValue)));
    }

    @Test
    void bedChange_setsRoomTypeOfTheNewBed() {
        var built = AddendumContent.build(
                base("LONG_TERM_RESIDENTIAL", "20", "5"), change("[\"BED\"]", "{\"newBedId\": 7}"), PRICING);

        assertThat(built.content().get("bedId").asLong()).isEqualTo(7);
        assertThat(built.content().get("roomTypeId").asLong()).isEqualTo(30);
        assertThat(built.before().get("roomTypeId").asLong()).isEqualTo(20);
        assertThat(built.after().get("roomTypeId").asLong()).isEqualTo(30);
    }

    @Test
    void dayCareToResidentialWithBed_setsRoomTypeOfTheNewBed() {
        var built = AddendumContent.build(
                base("DAY_CARE", "null", "null"),
                change("[\"RESIDENCE_TYPE\"]", "{\"residenceType\": \"LONG_TERM_RESIDENTIAL\", \"newBedId\": 7}"),
                PRICING);

        assertThat(built.content().get("bedId").asLong()).isEqualTo(7);
        assertThat(built.content().get("roomTypeId").asLong()).isEqualTo(30);
        assertThat(built.before().get("roomTypeId").isNull()).isTrue();
        assertThat(built.after().get("roomTypeId").asLong()).isEqualTo(30);
    }

    @Test
    void Q_462_residenceTypeChange_writesTheNewBillingPeriod() {
        var built = AddendumContent.build(
                JSON.readTree("""
                        {"residenceType": "DAY_CARE", "roomTypeId": null, "bedId": null, "residencyFeeBillingPeriod": "SESSION",
                         "services": [], "arrivalSchedule": []}"""),
                change(
                        "[\"RESIDENCE_TYPE\"]",
                        "{\"residenceType\": \"LONG_TERM_RESIDENTIAL\", \"newBedId\": 7, \"residencyFeeBillingPeriod\": \"MONTH\"}"),
                PRICING);

        assertThat(built.content().get("residencyFeeBillingPeriod").asString()).isEqualTo("MONTH");
        assertThat(built.before().get("residencyFeeBillingPeriod").asString()).isEqualTo("SESSION");
        assertThat(built.after().get("residencyFeeBillingPeriod").asString()).isEqualTo("MONTH");
    }

    @Test
    void bedOfTheSameRoomType_leavesRoomTypeOutOfTheDiff() {
        var built = AddendumContent.build(
                base("LONG_TERM_RESIDENTIAL", "30", "5"), change("[\"BED\"]", "{\"newBedId\": 7}"), PRICING);

        assertThat(built.content().get("roomTypeId").asLong()).isEqualTo(30);
        assertThat(built.after().has("roomTypeId")).isFalse();
    }
}
