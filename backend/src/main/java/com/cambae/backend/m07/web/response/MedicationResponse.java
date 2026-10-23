package com.cambae.backend.m07.web.response;

import com.cambae.backend.m07.internal.service.MedicationService.MedicationDetailView;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Phản hồi chi tiết thuốc theo hợp đồng OpenAPI api-006.yaml (schema Thuoc).
 */
public record MedicationResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("ten") String ten,
        @JsonProperty("hamLuong") String hamLuong,
        @JsonProperty("dangBaoChe") String dangBaoChe,
        @JsonProperty("duongDungMacDinh") String duongDungMacDinh,
        @JsonProperty("donViDung") String donViDung,
        @JsonProperty("hoatChat") List<Long> hoatChat,
        @JsonProperty("cuaSoTruocPhut") Integer cuaSoTruocPhut,
        @JsonProperty("cuaSoSauPhut") Integer cuaSoSauPhut,
        @JsonProperty("kiemSoatDacBiet") boolean kiemSoatDacBiet,
        @JsonProperty("hieuLuc") boolean hieuLuc,
        @JsonProperty("chuaCoMucVatPham") boolean chuaCoMucVatPham,
        @JsonProperty("phienBan") int phienBan) {

    public static MedicationResponse from(MedicationDetailView view) {
        return new MedicationResponse(
                view.id(),
                view.name(),
                view.concentration(),
                view.dosageForm(),
                view.defaultRoute(),
                view.usageUnit(),
                view.allergenIds(),
                view.beforeWindowMinutes(),
                view.afterWindowMinutes(),
                view.specialControl(),
                view.active(),
                view.missingPricingItem(),
                view.version());
    }
}
