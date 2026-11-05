package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogs;
import com.eldercare.backend.belongings.internal.catalog.domain.ValuableCategories;
import java.util.List;
import org.springframework.stereotype.Component;

/** Dấu "đồ có giá trị" theo CFG-M12-01 tại thời điểm gọi (FR-009), đọc theo tên loại đồ. */
@Component
public class ValuableTypes {

    private final ParameterApi parameters;
    private final CatalogApi catalogs;

    ValuableTypes(ParameterApi parameters, CatalogApi catalogs) {
        this.parameters = parameters;
        this.catalogs = catalogs;
    }

    public boolean isValuable(String typeName) {
        var value = parameters.read(ParameterCode.CFG_M12_01);
        return value.isSet() && value.asTable(ValuableCategories.class).contains(typeName);
    }

    /** Mã các loại đồ có giá trị (kể cả loại đã ngừng, vì đồ cũ vẫn mang loại đó). */
    List<Long> typeIds() {
        var value = parameters.read(ParameterCode.CFG_M12_01);
        if (!value.isSet()) {
            return List.of();
        }
        ValuableCategories valuable = value.asTable(ValuableCategories.class);
        return catalogs.list(BelongingCatalogs.BELONGING_TYPE, null, false).stream()
                .filter(t -> valuable.contains(t.name()))
                .map(CatalogItem::id)
                .toList();
    }
}
