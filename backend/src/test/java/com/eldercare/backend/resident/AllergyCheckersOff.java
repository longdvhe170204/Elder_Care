package com.eldercare.backend.resident;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Gỡ hai listener kiểm tra dị ứng thật của 006 ({@code PrescriptionAllergyListener}) và 011
 * ({@code NutritionAllergyListener}) khỏi context: test của BR-M01-12 tự quyết khi nào mỗi module gọi
 * {@code markChecked}. Hai listener này chạy ngay sau commit nên gỡ cả hai cờ trước khi test kịp kiểm (có từ PR #121,
 * #122).
 */
@TestConfiguration(proxyBeanMethods = false)
class AllergyCheckersOff {

    @Bean
    static BeanDefinitionRegistryPostProcessor removeRealAllergyCheckers() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                for (String name : new String[] {"prescriptionAllergyListener", "nutritionAllergyListener"}) {
                    if (registry.containsBeanDefinition(name)) {
                        registry.removeBeanDefinition(name);
                    }
                }
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {}
        };
    }
}
