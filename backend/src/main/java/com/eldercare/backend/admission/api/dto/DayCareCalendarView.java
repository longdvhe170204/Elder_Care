package com.eldercare.backend.admission.api.dto;

import java.time.LocalDate;
import java.util.List;

/** Lịch bán trú của một người trong một ngày (FR-060, FR-060a); {@code notices} theo thời điểm ghi, cũ nhất trước. */
public record DayCareCalendarView(LocalDate date, boolean closureDay, List<DayCareAbsenceNoticeView> notices) {}
