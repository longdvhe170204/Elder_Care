package com.eldercare.backend.administration.shared;

import static com.eldercare.backend.administration.shared.SharedTestKinds.CATALOG;
import static com.eldercare.backend.administration.shared.SharedTestKinds.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.shared.SharedTestKinds.Attributes;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.Change;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.common.error.NotFoundException;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * Hợp đồng của {@link CatalogApi} (FR-002, FR-003, BR-M15-06; module-interfaces 000 mục 1.8), chạy trên bản thật
 * ({@code CatalogApiIT}) và bản giả ({@code FakeCatalogApiTest}) theo constitution XVI (L).
 */
public interface CatalogApiContract {

    CatalogApi catalog();

    /** Chạy trong transaction (bản thật), như lệnh của module gọi. */
    <R> R tx(Supplier<R> work);

    private CatalogItem<Attributes> create(String name, Long parentId) {
        return tx(() ->
                catalog().create(CATALOG, new NewCatalogItem<>(null, name, parentId, 0, new Attributes("đỏ", 1))));
    }

    @Test
    default void confirm_recordsWhoConfirmed() {
        CatalogItem<Attributes> item = create(unique("Món"), null);

        Change<CatalogItem<Attributes>> change = tx(() -> catalog().confirm(CATALOG, item.id(), item.version(), 42L));

        assertThat(change.after().confirmedBy()).isEqualTo(42L);
        assertThat(change.after().confirmedAt()).isNotNull();
    }

    @Test
    default void FR_002_delete_unused_removesItem() {
        CatalogItem<Attributes> item = create(unique("Xóa"), null);

        assertThat(tx(() -> catalog().delete(CATALOG, item.id()))).isEqualTo(item);
        assertThatThrownBy(() -> tx(() -> catalog().require(CATALOG, item.id())))
                .isInstanceOf(NotFoundException.class);
    }
}
