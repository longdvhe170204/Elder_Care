package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.data.CatalogBase;
import com.cambae.backend.common.data.DataGroup;
import jakarta.persistence.Entity;
import java.util.List;
import org.junit.jupiter.api.Test;

/** T014: entity cơ cấu theo nhóm danh mục, giường giữ trạng thái qua gateway riêng. */
class FR_001_StructureEntitiesTest {
    @Test
    void FR_001_structureEntitiesUseTheSharedCatalogLifecycle() throws Exception {
        for (String name : List.of("Area", "Floor", "RoomType", "Room", "BedTransferReason")) {
            String type = "com.cambae.backend.m03.internal.structure.domain." + name;
            assertThat(getClass().getClassLoader().getResource(type.replace('.', '/') + ".class"))
                    .as(name)
                    .isNotNull();
            Class<?> entity = Class.forName(type);
            assertThat(entity.isAnnotationPresent(Entity.class)).isTrue();
            assertThat(CatalogBase.class.isAssignableFrom(entity)).isTrue();
            assertThat(entity.getAnnotation(DataGroup.class).value()).isEqualTo(DataGroup.Group.CATALOG);
        }
    }

    @Test
    void FR_011_bedStateCannotBeChangedThroughTheEntity() throws Exception {
        String type = "com.cambae.backend.m03.internal.bed.domain.Bed";
        assertThat(getClass().getClassLoader().getResource(type.replace('.', '/') + ".class"))
                .isNotNull();
        var entity = Class.forName(type);
        assertThat(entity.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(entity.getAnnotation(DataGroup.class).value()).isEqualTo(DataGroup.Group.BUSINESS);
        assertThat(java.util.Arrays.stream(entity.getMethods()).map(java.lang.reflect.Method::getName))
                .doesNotContain("setState", "changeState");
        var state = entity.getDeclaredField("state").getAnnotation(jakarta.persistence.Column.class);
        assertThat(state.updatable()).isFalse();
    }
}
