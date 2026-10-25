package com.cambae.backend.m07.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request body for creating or updating a medication in the Medication Catalog (FR-001, FR-002; api-006.yaml).
 */
public class MedicationBody {

    @NotBlank
    private String name;

    @NotBlank
    private String concentration;

    @NotBlank
    private String dosageForm;

    @NotBlank
    private String defaultRoute;

    @NotBlank
    private String usageUnit;

    @NotNull
    private List<Long> allergenIds;

    private Integer beforeWindowMinutes;

    private Integer afterWindowMinutes;

    private boolean specialControl;

    public MedicationBody() {}

    public MedicationBody(
            String name,
            String concentration,
            String dosageForm,
            String defaultRoute,
            String usageUnit,
            List<Long> allergenIds,
            Integer beforeWindowMinutes,
            Integer afterWindowMinutes,
            boolean specialControl) {
        this.name = name;
        this.concentration = concentration;
        this.dosageForm = dosageForm;
        this.defaultRoute = defaultRoute;
        this.usageUnit = usageUnit;
        this.allergenIds = allergenIds;
        this.beforeWindowMinutes = beforeWindowMinutes;
        this.afterWindowMinutes = afterWindowMinutes;
        this.specialControl = specialControl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConcentration() {
        return concentration;
    }

    public void setConcentration(String concentration) {
        this.concentration = concentration;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }

    public String getDefaultRoute() {
        return defaultRoute;
    }

    public void setDefaultRoute(String defaultRoute) {
        this.defaultRoute = defaultRoute;
    }

    public String getUsageUnit() {
        return usageUnit;
    }

    public void setUsageUnit(String usageUnit) {
        this.usageUnit = usageUnit;
    }

    public List<Long> getAllergenIds() {
        return allergenIds;
    }

    public void setAllergenIds(List<Long> allergenIds) {
        this.allergenIds = allergenIds;
    }

    public Integer getBeforeWindowMinutes() {
        return beforeWindowMinutes;
    }

    public void setBeforeWindowMinutes(Integer beforeWindowMinutes) {
        this.beforeWindowMinutes = beforeWindowMinutes;
    }

    public Integer getAfterWindowMinutes() {
        return afterWindowMinutes;
    }

    public void setAfterWindowMinutes(Integer afterWindowMinutes) {
        this.afterWindowMinutes = afterWindowMinutes;
    }

    public boolean isSpecialControl() {
        return specialControl;
    }

    public void setSpecialControl(boolean specialControl) {
        this.specialControl = specialControl;
    }
}
