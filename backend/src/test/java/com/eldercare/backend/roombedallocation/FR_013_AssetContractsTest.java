package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** T004: cổng chiều tài sản gọi giường và bản giả các hợp đồng mới cho thành viên khác dùng. */
class FR_013_AssetContractsTest {
    @Test
    void FR_013_assetCommandsStayInsideTheCallersTransaction() throws Exception {
        String name = "com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetCommandPort";
        assertThat(getClass().getClassLoader().getResource(name.replace('.', '/') + ".class"))
                .isNotNull();
        Class<?> type = Class.forName(name);
        assertThat(type.getDeclaredMethods()).hasSize(5);
        for (var method : type.getDeclaredMethods()) {
            assertThat(method.getAnnotation(Transactional.class))
                    .as(method.getName())
                    .isNotNull();
            assertThat(method.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.MANDATORY);
        }
    }

    @Test
    void FR_003_publicApiFakesRequireExplicitTestDataAndCanBeConfigured() throws Exception {
        String config = "com.eldercare.backend.support.fake.roombedallocation.FakeM03ApiConfig";
        assertThat(getClass().getClassLoader().getResource(config.replace('.', '/') + ".class"))
                .isNotNull();
        new ApplicationContextRunner()
                .withUserConfiguration(Class.forName(config))
                .run(context -> {
                    assertThat(context).hasSingleBean(ResidentLocationApi.class);
                    ResidentLocationApi fake = context.getBean(ResidentLocationApi.class);
                    assertThatThrownBy(() -> fake.current(1)).isInstanceOf(IllegalStateException.class);
                    doReturn(Optional.empty()).when(fake).current(1);
                    assertThat(fake.current(1)).isEmpty();
                });
    }
}
