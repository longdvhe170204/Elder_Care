package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.document.ExcelDocument;
import com.eldercare.backend.staffing.staff.internal.domain.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StaffImportRowValidatorTest {
    private List<StaffImportData.Error> errors(String phone, String date, String email) {
        var bytes = StaffImportIT.file(List.of(Arrays.asList("[Nhân viên]", "Điều dưỡng", phone, date, null, email)));
        var errors = new ArrayList<StaffImportData.Error>();
        StaffImportRowValidator.validate(
                ExcelDocument.read(bytes, StaffImportIT.HEADERS, 5).getFirst(), errors);
        return errors;
    }

    @ParameterizedTest
    @ValueSource(strings = {"31/02/2026", "25-10-2026", "2026-10-25", "29/02/2025"})
    void FR_001b_invalidCalendarDatesIdentifyStartDateColumn(String date) {
        assertThat(errors("0901234567", date, null)).anySatisfy(e -> {
            assertThat(e.column()).isEqualTo("startDate");
            assertThat(e.errorCode()).isEqualTo(StaffImportData.ErrorCode.INVALID_DATE_FORMAT);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"901234567", "1901234567", "09012345678", "09012abc67"})
    void FR_001b_phoneMustHaveTenDigitsStartingZero(String phone) {
        assertThat(errors(phone, "25/10/2026", null)).anySatisfy(e -> {
            assertThat(e.column()).isEqualTo("phoneNumber");
            assertThat(e.errorCode()).isEqualTo(StaffImportData.ErrorCode.INVALID_PHONE_NUMBER_FORMAT);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"09 01.234-567", "0901234567"})
    void FR_001b_normalizedPhoneAndOptionalEmailAreValid(String phone) {
        assertThat(errors(phone, "25/10/2026", null)).isEmpty();
    }
}
