package com.cambae.backend.m15.account.api;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

/**
 * SPI: giấy phép người hành nghề cho lớp pháp lý (FR-037 → FR-039; 002 research A-09). Khai báo ở 002; M09 (008) cài,
 * hiệu lực tới hết ngày hết hạn (008 contracts mục 1.1).
 */
public interface LicenseApi {

    /** Giấy phép loại đã cho còn hiệu lực trong ngày; rỗng khi không có. */
    Optional<LicenseValidity> valid(long staffId, String licenseType, LocalDate date);

    /** Giấy phép hết hạn trong khoảng tới (CFG-M06-02). */
    List<ExpiringLicense> expiringWithin(Period within);
}
