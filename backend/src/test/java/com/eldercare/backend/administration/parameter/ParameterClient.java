package com.eldercare.backend.administration.parameter;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Gọi API tham số trong test (contracts/api-000.yaml {@code /parameters}). */
record ParameterClient(MockMvc mvc) {

    static final String URL = "/api/v1/parameters/";

    /** ETag hiện tại của tham số. */
    String etag(String code) throws Exception {
        return mvc.perform(get(URL + code)).andReturn().getResponse().getHeader("ETag");
    }

    ResultActions change(String code, String ifMatch, String valueJson, String reason) throws Exception {
        String body = "{\"value\":" + valueJson + (reason == null ? "" : ",\"reason\":\"" + reason + "\"") + "}";
        return send(code, ifMatch, body);
    }

    ResultActions restoreDefault(String code, String ifMatch, String reason) throws Exception {
        return send(code, ifMatch, "{\"restoreDefault\":true,\"reason\":\"" + reason + "\"}");
    }

    private ResultActions send(String code, String ifMatch, String body) throws Exception {
        return mvc.perform(post(URL + code + "/change-value")
                .with(csrf())
                .header("If-Match", ifMatch)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
