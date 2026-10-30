package com.eldercare.backend.support.fake.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class T002_FakeSwitchHttpTest {
    @Test
    void T002_approvedRouteValidatesBodyAndTogglesEveryModule() throws Exception {
        var switches = new FakeM04Switches();
        var mvc =
                MockMvcBuilders.standaloneSetup(new FakeM04Controller(switches)).build();
        for (String module : new String[] {"006", "007", "008", "011", "012", "014"}) {
            mvc.perform(put("/api/v1/testing/fakes/" + module)
                            .contentType("application/json")
                            .content("{\"enabled\":false}"))
                    .andExpect(status().isNoContent());
            assertThatThrownBy(() -> switches.requireEnabled(module)).isInstanceOf(IllegalStateException.class);
            mvc.perform(put("/api/v1/testing/fakes/" + module)
                            .contentType("application/json")
                            .content("{\"enabled\":true}"))
                    .andExpect(status().isNoContent());
            assertThatCode(() -> switches.requireEnabled(module)).doesNotThrowAnyException();
        }
        mvc.perform(put("/api/v1/testing/fakes/999")
                        .contentType("application/json")
                        .content("{\"enabled\":false}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/testing/fakes/007")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/testing/fakes/007")
                        .contentType("application/json")
                        .content("{\"enabled\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void T002_controllerIsAbsentWithoutImportAndInProduction() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=test")
                .run(context -> assertThat(context).doesNotHaveBean(FakeM04Controller.class));
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=prod")
                .withUserConfiguration(FakeM04Config.class)
                .run(context -> assertThat(context).doesNotHaveBean(FakeM04Controller.class));
    }
}
