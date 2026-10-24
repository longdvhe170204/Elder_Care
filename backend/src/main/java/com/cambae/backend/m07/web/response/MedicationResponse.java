package com.cambae.backend.m07.web.response;

import com.cambae.backend.m07.internal.service.MedicationService.MedicationDetailView;
import java.util.List;

/**
 * Medication detail response per OpenAPI contract api-006.yaml (schema Thuoc).
 */
public record MedicationResponse(
        Long id,
        String name,
        String concentration,
        String dosageForm,
        String defaultRoute,
        String usageUnit,
        List<Long> allergenIds,
        Integer beforeWindowMinutes,
        Integer afterWindowMinutes,
        boolean specialControl,
        boolean active,
        boolean missingPricingItem,
        int version) {

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

    // Compatibility accessors
    public String ten() {
        return name;
    }

    public String hamLuong() {
        return concentration;
    }

    public String dangBaoChe() {
        return dosageForm;
    }

    public String duongDungMacDinh() {
        return defaultRoute;
    }

    public String donViDung() {
        return usageUnit;
    }

    public List<Long> hoatChat() {
        return allergenIds;
    }

    public Integer cuaSoTruocPhut() {
        return beforeWindowMinutes;
    }

    public Integer cuaSoSauPhut() {
        return afterWindowMinutes;
    }

    public boolean kiemSoatDacBiet() {
        return specialControl;
    }

    public boolean hieuLuc() {
        return active;
    }

    public boolean chuaCoMucVatPham() {
        return missingPricingItem;
    }

    public int phienBan() {
        return version;
    }
}
