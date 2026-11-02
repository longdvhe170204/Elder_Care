package com.eldercare.backend.admission.internal.domain;

import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;

/** Đổi mã mức chăm sóc, loại lưu trú giữa 001 ({@code REGULAR}) và SPI phân bổ của 003 ({@code FREQUENT}; glossary mục 9). */
public final class CareLevelCodes {

    private CareLevelCodes() {}

    public static CareLevel of(CareLevelCode code) {
        return switch (code) {
            case BASIC -> CareLevel.BASIC;
            case FREQUENT -> CareLevel.REGULAR;
            case SPECIAL -> CareLevel.SPECIAL;
        };
    }

    public static CareLevelCode of(CareLevel level) {
        return switch (level) {
            case BASIC -> CareLevelCode.BASIC;
            case REGULAR -> CareLevelCode.FREQUENT;
            case SPECIAL -> CareLevelCode.SPECIAL;
        };
    }

    public static ResidencyType of(ResidenceType type) {
        return switch (type) {
            case LONG_TERM_RESIDENTIAL -> ResidencyType.LONG_TERM;
            case SHORT_TERM_RESIDENTIAL -> ResidencyType.SHORT_TERM;
            case DAY_CARE -> ResidencyType.DAY_CARE;
        };
    }
}
