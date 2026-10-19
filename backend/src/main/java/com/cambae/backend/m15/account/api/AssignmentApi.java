package com.cambae.backend.m15.account.api;

import java.time.Instant;
import java.util.List;

/**
 * SPI: phân công của nhân viên cho phạm vi dữ liệu (FR-028 → FR-035a). Khai báo ở 002; M09 (008) cài (008 contracts
 * mục 1.1, FR-037a).
 */
public interface AssignmentApi {

    /** Phân công đang hiệu lực tại thời điểm đã cho. */
    List<AssignmentView> activeAt(long staffId, Instant at);

    /** Phân công có hiệu lực giao với khoảng đã cho (FR-035a). */
    List<AssignmentView> history(long staffId, Instant from, Instant to);
}
