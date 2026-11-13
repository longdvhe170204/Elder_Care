package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.care.api.spi.TaskSourcePort;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.care.FakeM04Config;
import com.eldercare.backend.support.fake.care.FakeTaskSource;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * T002, team-plan mục 8 (C, D: bean thật {@code TaskSourcePort} của 007): {@code CareTaskValidator} đòi đúng một nguồn
 * mỗi loại căn cứ. Bản giả nhường loại đã có bean thật (007 theo dõi sau ngã, theo dõi tiếp xúc; 014 buổi hoạt động);
 * {@code MEAL} là bản thật {@code MealTaskSource} của 011 (011 T042).
 */
@Import(FakeM04Config.class)
class T002_FakeTaskSourcesIT extends MySqlIntegrationTestBase {

    @Autowired
    List<TaskSourcePort> sources;

    private List<TaskSourcePort> managing(SourceType type) {
        return sources.stream().filter(s -> s.managedTypes().contains(type)).toList();
    }

    @Test
    void exactlyOneSourcePerBasisType_realBeanWinsOverFake() {
        for (SourceType type : List.of(
                SourceType.MEASUREMENT,
                SourceType.MEAL,
                SourceType.ACTIVITY_SESSION,
                SourceType.POST_FALL_MONITORING,
                SourceType.CONTACT_MONITORING,
                SourceType.WATER_INTAKE_ALERT,
                SourceType.REWORK)) {
            assertThat(managing(type)).as(type.name()).hasSize(1);
        }
        assertThat(managing(SourceType.POST_FALL_MONITORING).getFirst()).isNotInstanceOf(FakeTaskSource.class);
        assertThat(managing(SourceType.CONTACT_MONITORING).getFirst()).isNotInstanceOf(FakeTaskSource.class);
        assertThat(managing(SourceType.ACTIVITY_SESSION).getFirst()).isNotInstanceOf(FakeTaskSource.class);
        // Chưa có bean thật của 007 cho lịch đo, cảnh báo uống nước: bản giả quản
        assertThat(managing(SourceType.MEASUREMENT).getFirst()).isInstanceOf(FakeTaskSource.class);
        assertThat(managing(SourceType.MEAL).getFirst()).isNotInstanceOf(FakeTaskSource.class);
    }
}
