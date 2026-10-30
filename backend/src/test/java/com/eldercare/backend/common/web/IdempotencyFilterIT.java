package com.eldercare.backend.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.testsupport.SampleController;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** R-03, FR-037 (000 T019, T020): cùng Idempotency-Key, cùng người gửi thì trả lại kết quả lần đầu. */
@AutoConfigureMockMvc
@Import(SampleController.class)
class IdempotencyFilterIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    SampleController sample;

    @BeforeEach
    void resetCounter() {
        sample.created.set(0);
    }

    private MockHttpServletRequestBuilder create(String key, String user, String name) {
        MockHttpServletRequestBuilder request = post("/api/v1/test-support/samples")
                .with(csrf())
                .with(user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\"}");
        return key == null ? request : request.header("Idempotency-Key", key);
    }

    @Test
    void R_03_sameKeySameUser_returnsFirstResultWithoutSecondRecord() throws Exception {
        String key = UUID.randomUUID().toString();

        mvc.perform(create(key, "a", "x"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(1));
        mvc.perform(create(key, "a", "x"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(1));

        assertThat(sample.created).hasValue(1);
    }

    @Test
    void R_03_sameKeyOtherUser_isIndependent() throws Exception {
        String key = UUID.randomUUID().toString();

        mvc.perform(create(key, "a", "x")).andExpect(jsonPath("$.number").value(1));
        mvc.perform(create(key, "b", "x")).andExpect(jsonPath("$.number").value(2));
    }

    @Test
    void R_03_sameKeyDifferentRequest_isRejected() throws Exception {
        String key = UUID.randomUUID().toString();

        mvc.perform(create(key, "a", "x")).andExpect(status().isCreated());
        mvc.perform(create(key, "a", "y"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REUSED"));

        assertThat(sample.created).hasValue(1);
    }

    @Test
    void R_03_noKey_runsEveryTime() throws Exception {
        mvc.perform(create(null, "a", "x")).andExpect(jsonPath("$.number").value(1));
        mvc.perform(create(null, "a", "x")).andExpect(jsonPath("$.number").value(2));
    }
}
