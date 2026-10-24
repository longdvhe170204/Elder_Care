package com.cambae.backend.m01.internal.health.domain;

import com.cambae.backend.common.data.CatalogBase;
import com.cambae.backend.common.data.DataGroup;
import com.cambae.backend.m01.api.dto.AllergenGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Dị nguyên, danh mục nhóm 1 (data-model mục 5; FR-018a). */
@Entity
@Table(name = "allergen")
@DataGroup(DataGroup.Group.CATALOG)
public class Allergen extends CatalogBase {

    @Enumerated(EnumType.STRING)
    @Column(name = "allergen_group", nullable = false)
    private AllergenGroup group;

    protected Allergen() {}

    public Allergen(String name, AllergenGroup group) {
        super(name);
        this.group = group;
    }

    public AllergenGroup getGroup() {
        return group;
    }

    public void change(String name, AllergenGroup group) {
        rename(name);
        this.group = group;
    }
}
