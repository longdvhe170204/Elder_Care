package com.eldercare.backend.resident;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Gỡ participant thật {@code CostStatusParticipant} (010, điều kiện END_A_COSTS) khỏi context: test 001 đặt điều kiện qua
 * bản giả nên bản thật làm danh sách điều kiện thừa {@code END_A_COSTS} hoặc chặn lệnh kết thúc lưu trú. Chỉ áp cho lớp
 * import cấu hình này; {@code FinalPeriodStatusIT} (010) dùng bản thật.
 */
@TestConfiguration(proxyBeanMethods = false)
class CostParticipantOff {

    @Bean
    static BeanDefinitionRegistryPostProcessor removeRealCostParticipant() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                if (registry.containsBeanDefinition("costStatusParticipant")) {
                    registry.removeBeanDefinition("costStatusParticipant");
                }
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {}
        };
    }
}
