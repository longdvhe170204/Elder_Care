package com.eldercare.backend.administration.approval;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Gọi API yêu cầu phê duyệt trong test (contracts/api-000.yaml {@code /approval-requests}). */
record ApprovalClient(MockMvc mvc, JsonMapper json) {

    static final String URL = "/api/v1/approval-requests";

    /** Lập yêu cầu mẫu với khóa ngẫu nhiên; trả thân phản hồi. */
    JsonNode create(LocalDate effectiveDate, boolean submitImmediately) throws Exception {
        String body = "{\"type\":\"" + ApprovalFixtures.TYPE + "\",\"content\":{\"key\":\"" + UUID.randomUUID()
                + "\"},\"requestedEffectiveDate\":\"" + effectiveDate + "\",\"reason\":\"Đề nghị mẫu\""
                + ",\"submitImmediately\":" + submitImmediately + "}";
        return body(mvc.perform(post(URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn());
    }

    ResultActions createRaw(String body) throws Exception {
        return mvc.perform(
                post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    JsonNode read(long id) throws Exception {
        return body(mvc.perform(get(URL + "/" + id)).andReturn());
    }

    String etag(long id) throws Exception {
        return mvc.perform(get(URL + "/" + id)).andReturn().getResponse().getHeader("ETag");
    }

    ResultActions command(long id, String command, String ifMatch, String jsonBody) throws Exception {
        // Gắn người dùng đăng nhập vào chính request để gọi được từ luồng khác (test đồng thời)
        var request = post(URL + "/" + id + "/" + command)
                .with(csrf())
                .with(user("tester"))
                .header("If-Match", ifMatch);
        if (jsonBody != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(jsonBody);
        }
        return mvc.perform(request);
    }

    ResultActions submit(long id) throws Exception {
        return command(id, "submit", etag(id), null);
    }

    ResultActions approve(long id, String comment) throws Exception {
        return command(id, "approve", etag(id), comment == null ? "{}" : "{\"comment\":\"" + comment + "\"}");
    }

    ResultActions reject(long id, String comment) throws Exception {
        return command(id, "reject", etag(id), comment == null ? "{}" : "{\"comment\":\"" + comment + "\"}");
    }

    ResultActions cancel(long id, String reason) throws Exception {
        return command(id, "cancel", etag(id), reason == null ? "{}" : "{\"reason\":\"" + reason + "\"}");
    }

    JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
