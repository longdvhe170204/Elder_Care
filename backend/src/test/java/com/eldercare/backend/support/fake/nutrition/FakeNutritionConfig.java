package com.eldercare.backend.support.fake.nutrition;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả các SPI, sự kiện do 011 khai báo cho 012, 014 (module-interfaces mục 1.6). Test cần thì
 * {@code @Import(FakeNutritionConfig.class)} rồi tiêm bản giả để đặt dữ liệu; {@code @Primary} để thắng bản thật khi có.
 */
@TestConfiguration
public class FakeNutritionConfig {

    @Bean
    @Primary
    FakeStayMealPort fakeStayMealPort() {
        return new FakeStayMealPort();
    }

    @Bean
    @Primary
    FakeTripMealPort fakeTripMealPort() {
        return new FakeTripMealPort();
    }

    @Bean
    FakeNutritionEvents fakeNutritionEvents(ApplicationEventPublisher publisher) {
        return new FakeNutritionEvents(publisher);
    }
}
