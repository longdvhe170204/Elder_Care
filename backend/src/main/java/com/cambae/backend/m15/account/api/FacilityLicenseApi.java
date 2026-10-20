package com.cambae.backend.m15.account.api;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

/**
 * SPI: giấy phép cơ sở cho lớp pháp lý (FR-037 (c), FR-039; 002 research A-09). Khai báo ở 002; M06 (007, FR-070) cài
 * (007 contracts/module-interfaces.md, S-16). Tên logic {@code GiayPhepCoSoApi}.
 */
public interface FacilityLicenseApi {

    /** Cơ sở có giấy phép còn hiệu lực trong ngày, bao phạm vi hoạt động đã cho ({@code coPhamVi(phamVi, ngay)}). */
    boolean coversScope(String operationScope, LocalDate date);

    /** Giấy phép cơ sở hết hạn trong khoảng tới, cho cảnh báo FR-039 ({@code sapHetHan(trongVong)}). */
    List<ExpiringFacilityLicense> expiringWithin(Period within);
}
