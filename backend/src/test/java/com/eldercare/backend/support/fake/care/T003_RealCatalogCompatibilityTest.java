package com.eldercare.backend.support.fake.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.eldercare.backend.care.api.TaskTypeApi;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.catalog.service.CareTaskTypeLookup;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Khi có nguồn danh mục thật,test/demo vẫn dùng đúng fake chung đã cấu hình. */
class T003_RealCatalogCompatibilityTest {
    @Test
    void T003_existingFakeRemainsSelectedWhenTheRealCatalogProviderExists() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=test")
                .withUserConfiguration(
                        FakeM04Config.class,
                        CareTaskTypeLookup.class,
                        com.eldercare.backend.support.fake.health.FakeVitalSigns.Config.class)
                .withBean(CareCatalogStore.class, () -> mock(CareCatalogStore.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(TaskTypeApi.class))
                            .isSameAs(context.getBean(FakeCareRequirements.class));
                });
    }
}
