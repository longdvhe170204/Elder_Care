package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class T006_ActivityGovernanceIT extends MySqlIntegrationTestBase {
    @Autowired
    List<FieldClassificationProvider> fields;

    @Test
    void FR_068_classifiesAttendancePreferencesAndQualityCheckFields() {
        assertFields(
                "M04.SESSION_ATTENDANCE",
                InfoGroup.INTERNAL_PUBLIC,
                "participationLevel",
                "communicationLevel",
                "conditionAfter",
                "note",
                "absenceReason");
        assertFields("M04.PREFERENCE", InfoGroup.WISHES, "description", "level", "source");
        assertFields("M04.QUALITY_CHECK_ITEM", InfoGroup.INTERNAL_PUBLIC, "status", "note", "photoFileIds");
    }

    private void assertFields(String type, InfoGroup group, String... names) {
        assertThat(fields.stream().filter(provider -> type.equals(provider.entityType())))
                .as("Provider phân loại của %s", type)
                .singleElement()
                .satisfies(provider -> {
                    for (String name : names) {
                        assertThat(provider.fieldGroups()).containsEntry(name, group);
                    }
                });
    }
}
