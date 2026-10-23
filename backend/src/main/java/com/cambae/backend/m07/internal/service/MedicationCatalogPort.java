package com.cambae.backend.m07.internal.service;

/**
 * Cổng tra cứu dịch vụ/đơn giá thuốc nguồn viện từ Module M02 (004) theo FR-003.
 * Khi chưa có mục vật phẩm ở 004, trả về false để đánh dấu {@code chuaCoMucVatPham = true}.
 */
public interface MedicationCatalogPort {

    /**
     * Kiểm tra thuốc nguồn viện đã có mục vật phẩm (dịch vụ THUOC_NGUON_VIEN) tại 004 chưa.
     *
     * @param medicationId id thuốc
     * @return true nếu đã có mục vật phẩm, false nếu chưa có
     */
    boolean hasHospitalProcurementItem(long medicationId);
}
