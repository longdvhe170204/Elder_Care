package com.eldercare.backend.care;

import static com.eldercare.backend.care.internal.catalog.domain.CareCatalogKinds.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.care.internal.catalog.domain.TaskTypeAttributes;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.BusinessException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

/** Hai lệnh có thể cùng qua precheck;UNIQUE là cổng chặn cuối,cũng phải trả409. */
class FR_038_CatalogRaceTest {
    private TaskTypeAttributes attributes() {
        return new TaskTypeAttributes(
                "OTHER",
                false,
                "PERFORMED",
                Role.CAREGIVER,
                "NORMAL",
                null,
                null,
                false,
                null,
                null,
                null,
                Set.of(),
                null);
    }

    @Test
    void FR_038_concurrentCreateDuplicateIsAConflict() {
        var api = mock(CatalogApi.class);
        when(api.create(eq(TASK_TYPES), any())).thenThrow(new DuplicateKeyException("uq_catalog_item_active_name"));
        var store = new CareCatalogStore(api);
        assertThatThrownBy(() -> store.create(TASK_TYPES, "Tên trùng", attributes()))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.errorCode()).isEqualTo("DUPLICATE_NAME");
                });
    }

    @Test
    void FR_038_concurrentRenameDuplicateIsAConflict() {
        var api = mock(CatalogApi.class);
        var data = attributes();
        var row = new CatalogItem<>(7L, null, "Tên trước", null, 0, CatalogStatus.ACTIVE, data, null, null, 0);
        when(api.update(eq(TASK_TYPES), eq(7L), eq(0), anyString(), eq(0), any()))
                .thenThrow(new DuplicateKeyException("uq_catalog_item_active_name"));
        var store = new CareCatalogStore(api);
        assertThatThrownBy(() -> store.update(TASK_TYPES, row, "Tên trùng", data))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("DUPLICATE_NAME"));
    }
}
