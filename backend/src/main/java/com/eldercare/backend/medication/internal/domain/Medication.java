package com.eldercare.backend.medication.internal.domain;

import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.sql.Types;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * Medication Catalog Entity (FR-001, FR-002, data-model section 3).
 * Stored in table {@code medication}, active ingredients in {@code medication_active_ingredient}.
 */
@Entity
@Table(name = "medication")
@DataGroup(DataGroup.Group.CATALOG)
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String concentration;

    @Column(name = "dosage_form", nullable = false, length = 100)
    private String dosageForm;

    @Column(name = "default_route", nullable = false, length = 100)
    private String defaultRoute;

    @Column(name = "usage_unit", nullable = false, length = 50)
    private String usageUnit;

    @JdbcTypeCode(Types.SMALLINT)
    @Column(name = "before_window_minutes")
    private Integer beforeWindowMinutes;

    @JdbcTypeCode(Types.SMALLINT)
    @Column(name = "after_window_minutes")
    private Integer afterWindowMinutes;

    @Column(name = "special_control", nullable = false)
    private boolean specialControl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MedicationStatus status = MedicationStatus.ACTIVE;

    @Version
    @Column(nullable = false)
    private int version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "medication_active_ingredient", joinColumns = @JoinColumn(name = "medication_id"))
    @Column(name = "allergen_id", nullable = false)
    private Set<Long> allergenIds = new HashSet<>();

    protected Medication() {}

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
        this.status = MedicationStatus.ACTIVE;
        if (allergenIds != null) {
            this.allergenIds = new HashSet<>(allergenIds);
        }
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
