package com.eldercare.backend.administration.sample;

import com.eldercare.backend.common.data.CatalogBase;
import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Danh mục mẫu, nhóm 1 (000 T048). */
@Entity
@Table(name = "sample_catalog")
@DataGroup(DataGroup.Group.CATALOG)
public class SampleCatalogItem extends CatalogBase {

    protected SampleCatalogItem() {}

    public SampleCatalogItem(String name) {
        super(name);
    }
}
