package com.eldercare.backend.medication.internal.catalog.domain;

import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.common.data.CatalogStatus;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Danh mục thuốc của M07, được lưu trong {@code catalog_item} loại {@code M07.MEDICATION}. */
public class Medication {

    private Long id;
    private String name;
    private String concentration;
    private String dosageForm;
    private String defaultRoute;
    private String usageUnit;
    private Integer beforeWindowMinutes;
    private Integer afterWindowMinutes;
    private boolean specialControl;
    private MedicationStatus status = MedicationStatus.ACTIVE;
    private int version;
    private Set<Long> allergenIds = new HashSet<>();

    public Medication(
            String name,
            String concentration,
            String dosageForm,
            String defaultRoute,
            String usageUnit,
            Integer beforeWindowMinutes,
            Integer afterWindowMinutes,
            boolean specialControl,
            Set<Long> allergenIds) {
        this.name = name;
        this.concentration = concentration;
        this.dosageForm = dosageForm;
        this.defaultRoute = defaultRoute;
        this.usageUnit = usageUnit;
        this.beforeWindowMinutes = beforeWindowMinutes;
        this.afterWindowMinutes = afterWindowMinutes;
        this.specialControl = specialControl;
        if (allergenIds != null) {
            this.allergenIds = new HashSet<>(allergenIds);
        }
    }

    public static Medication fromCatalog(CatalogItem<MedicationAttributes> item) {
        MedicationAttributes attributes = item.attributes();
        Medication medication = new Medication(
                attributes.name(),
                attributes.concentration(),
                attributes.dosageForm(),
                attributes.defaultRoute(),
                attributes.usageUnit(),
                attributes.beforeWindowMinutes(),
                attributes.afterWindowMinutes(),
                attributes.specialControl(),
                Set.copyOf(attributes.activeIngredientIds()));
        medication.synchronizeFromCatalog(item);
        return medication;
    }

    /** Đồng bộ định danh và phiên bản sau khi ghi qua bảng danh mục dùng chung. */
    public void synchronizeFromCatalog(CatalogItem<MedicationAttributes> item) {
        this.id = item.id();
        this.status = item.status() == CatalogStatus.ACTIVE ? MedicationStatus.ACTIVE : MedicationStatus.INACTIVE;
        this.version = item.version();
    }

    public MedicationAttributes attributes() {
        return new MedicationAttributes(
                name,
                concentration,
                dosageForm,
                defaultRoute,
                usageUnit,
                beforeWindowMinutes,
                afterWindowMinutes,
                specialControl,
                allergenIds.stream().sorted().toList(),
                null);
    }

    public String catalogName() {
        return name + " | " + concentration + " | " + dosageForm;
    }

    public void update(
            String name,
            String concentration,
            String dosageForm,
            String defaultRoute,
            String usageUnit,
            Integer beforeWindowMinutes,
            Integer afterWindowMinutes,
            boolean specialControl,
            Set<Long> allergenIds) {
        this.name = name;
        this.concentration = concentration;
        this.dosageForm = dosageForm;
        this.defaultRoute = defaultRoute;
        this.usageUnit = usageUnit;
        this.beforeWindowMinutes = beforeWindowMinutes;
        this.afterWindowMinutes = afterWindowMinutes;
        this.specialControl = specialControl;
        this.allergenIds.clear();
        if (allergenIds != null) {
            this.allergenIds.addAll(allergenIds);
        }
    }

    public void deactivate() {
        this.status = MedicationStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == MedicationStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getConcentration() {
        return concentration;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public String getDefaultRoute() {
        return defaultRoute;
    }

    public String getUsageUnit() {
        return usageUnit;
    }

    public Integer getBeforeWindowMinutes() {
        return beforeWindowMinutes;
    }

    public Integer getAfterWindowMinutes() {
        return afterWindowMinutes;
    }

    public boolean isSpecialControl() {
        return specialControl;
    }

    public MedicationStatus getStatus() {
        return status;
    }

    public int getVersion() {
        return version;
    }

    public Set<Long> getAllergenIds() {
        return Collections.unmodifiableSet(allergenIds);
    }
}
