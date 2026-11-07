package com.eldercare.backend.belongings.internal.incident.service;

/**
 * Tra mã loại sự cố của 007 ({@code BELONGINGS_LOST}, {@code BELONGINGS_DAMAGED}) ra mã dòng danh mục
 * {@code M05.INCIDENT_TYPE} để lập {@code RecordIncidentCommand} (013 T021, plan mục "Kế hoạch triển khai R-15" điểm 14).
 */
public interface IncidentTypeLookup {

    long idOf(String typeCode);
}
