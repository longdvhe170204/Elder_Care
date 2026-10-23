package com.cambae.backend.m03.internal.allocation.config;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.internal.allocation.persistence.M03GovernanceStore;
import com.cambae.backend.m03.internal.allocation.service.M03FieldClassification;
import com.cambae.backend.m03.internal.allocation.service.M03Retention;
import com.cambae.backend.m03.internal.allocation.service.M03ScopeAssignment;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.spi.ResidencyEndApi;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.m15.retention.api.RetentionKind;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** T010: đăng ký provider theo loại dữ liệu, không tạo dữ liệu nghiệp vụ. */
@Configuration
public class M03GovernanceConfiguration {
    @Bean
    M03FieldClassification areaFields() {
        return new M03FieldClassification("M03.AREA", Map.of());
    }

    @Bean
    M03FieldClassification floorFields() {
        return new M03FieldClassification("M03.FLOOR", Map.of());
    }

    @Bean
    M03FieldClassification roomTypeFields() {
        return new M03FieldClassification("M03.ROOM_TYPE", Map.of());
    }

    @Bean
    M03FieldClassification roomFields() {
        return new M03FieldClassification("M03.ROOM", Map.of("isolationReason", InfoGroup.HEALTH));
    }

    @Bean
    M03FieldClassification bedFields() {
        return new M03FieldClassification("M03.BED", Map.of("infectionControlReason", InfoGroup.HEALTH));
    }

    @Bean
    M03FieldClassification transferReasonFields() {
        return new M03FieldClassification("M03.TRANSFER_REASON", Map.of());
    }

    @Bean
    M03FieldClassification dayCareRestAreaFields() {
        return new M03FieldClassification("M03.DAY_CARE_REST_AREA", Map.of());
    }

    @Bean
    M03FieldClassification dayCareSessionFields() {
        return new M03FieldClassification("M03.DAY_CARE_SESSION", Map.of());
    }

    @Bean
    M03FieldClassification bedAllocationFields() {
        return new M03FieldClassification("M03.BED_ALLOCATION", Map.of("infectionControlReason", InfoGroup.HEALTH));
    }

    @Bean
    M03FieldClassification bedStateHistoryFields() {
        return new M03FieldClassification("M03.BED_STATE_HISTORY", Map.of("infectionControlReason", InfoGroup.HEALTH));
    }

    @Bean
    M03FieldClassification roomIsolationHistoryFields() {
        return new M03FieldClassification("M03.ROOM_ISOLATION_HISTORY", Map.of("reason", InfoGroup.HEALTH));
    }

    @Bean
    M03FieldClassification bedReadinessConfirmationFields() {
        return new M03FieldClassification("M03.BED_READINESS_CONFIRMATION", Map.of());
    }

    @Bean
    M03FieldClassification residentPositionFields() {
        return new M03FieldClassification("M03.RESIDENT_POSITION", Map.of());
    }

    @Bean
    M03ScopeAssignment bedScope(M03GovernanceStore store) {
        return new M03ScopeAssignment("M03.BED", store);
    }

    @Bean
    M03ScopeAssignment roomScope(M03GovernanceStore store) {
        return new M03ScopeAssignment("M03.ROOM", store);
    }

    @Bean
    M03ScopeAssignment bedAllocationScope(M03GovernanceStore store) {
        return new M03ScopeAssignment("M03.BED_ALLOCATION", store);
    }

    @Bean
    M03Retention bedAllocationRetention(
            M03GovernanceStore store,
            RetentionDatabase database,
            ObjectProvider<ResidencyEndApi> ends,
            ParameterApi parameters,
            BusinessTime time) {
        return new M03Retention(
                "M03.BED_ALLOCATION",
                "bed_allocation",
                new RetentionKind.ResidentRecord(),
                store,
                database,
                ends,
                parameters,
                time);
    }

    @Bean
    M03Retention bedStateHistoryRetention(
            M03GovernanceStore store,
            RetentionDatabase database,
            ObjectProvider<ResidencyEndApi> ends,
            ParameterApi parameters,
            BusinessTime time) {
        return new M03Retention(
                "M03.BED_STATE_HISTORY",
                "bed_state_history",
                new RetentionKind.NotResidentLinked(),
                store,
                database,
                ends,
                parameters,
                time);
    }

    @Bean
    M03Retention roomIsolationHistoryRetention(
            M03GovernanceStore store,
            RetentionDatabase database,
            ObjectProvider<ResidencyEndApi> ends,
            ParameterApi parameters,
            BusinessTime time) {
        return new M03Retention(
                "M03.ROOM_ISOLATION_HISTORY",
                "room_isolation_history",
                new RetentionKind.NotResidentLinked(),
                store,
                database,
                ends,
                parameters,
                time);
    }
}
