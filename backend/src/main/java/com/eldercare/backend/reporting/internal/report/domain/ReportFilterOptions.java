package com.eldercare.backend.reporting.internal.report.domain;

import java.util.List;

/** Giá trị lọc tầng/khu trong phạm vi của người xem (FR-013; contracts {@code Report.filterOptions}). */
public record ReportFilterOptions(boolean facilityWide, List<FloorArea> floorAreas) {

    public record FloorArea(long id, String name) {}
}
