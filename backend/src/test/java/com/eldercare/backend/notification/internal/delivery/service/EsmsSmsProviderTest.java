package com.eldercare.backend.notification.internal.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.notification.api.spi.SmsResult;
import com.eldercare.backend.notification.api.spi.SmsResult.Outcome;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 009 T026a (research B-05, FR-018): bộ chuyển eSMS gửi tin chăm sóc khách hàng ({@code SmsType = 2}) qua
 * {@code SendMultipleMessage_V4_post_json}, đổi {@code CodeResult} thành {@link SmsResult}: 100 là đã gửi; lỗi mạng,
 * HTTP 5xx, mã 99 là lỗi tạm để gửi lại theo CFG-M13-05; mã khác là lỗi vĩnh viễn kèm mã của nhà cung cấp. Máy chủ HTTP giả
 * của JDK thay cho eSMS.
 */
class EsmsSmsProviderTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private HttpServer server;
    private final AtomicReference<JsonNode> received = new AtomicReference<>();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Máy chủ trả {@code status} và {@code body} cho mọi yêu cầu, giữ thân yêu cầu cuối. */
    private URI serve(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            received.set(JSON.readTree(exchange.getRequestBody().readAllBytes()));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                + "/MainService.svc/json/SendMultipleMessage_V4_post_json/");
    }

    private static EsmsSmsProvider provider(URI endpoint, boolean sandbox) {
        return new EsmsSmsProvider(endpoint, "api-key", "secret-key", "ELDERCARE", sandbox, Duration.ofSeconds(2));
    }

    @Test
    void T026a_code100_isSent_requestCarriesCustomerCareFieldsAndSandbox() throws IOException {
        URI endpoint = serve(200, "{\"CodeResult\":\"100\",\"SMSID\":\"abc-1\",\"CountRegenerate\":0}");

        SmsResult r = provider(endpoint, true).send("0912345678", "Mã đặt lại mật khẩu: 123456");

        assertThat(r.outcome()).isEqualTo(Outcome.SENT);
        assertThat(r.providerCode()).contains("abc-1");
        JsonNode body = received.get();
        assertThat(body.get("ApiKey").asString()).isEqualTo("api-key");
        assertThat(body.get("SecretKey").asString()).isEqualTo("secret-key");
        assertThat(body.get("Phone").asString()).isEqualTo("0912345678");
        assertThat(body.get("Content").asString()).isEqualTo("Mã đặt lại mật khẩu: 123456");
        assertThat(body.get("Brandname").asString()).isEqualTo("ELDERCARE");
        assertThat(body.get("SmsType").asString()).isEqualTo("2");
        assertThat(body.get("IsUnicode").asString()).isEqualTo("1");
        assertThat(body.get("Sandbox").asString()).isEqualTo("1");
    }

    @Test
    void T026a_asciiContent_notUnicode_liveMode_sandboxOff() throws IOException {
        URI endpoint = serve(200, "{\"CodeResult\":\"100\",\"SMSID\":\"abc-2\"}");

        provider(endpoint, false).send("84912345678", "Ma xac nhan 123456");

        assertThat(received.get().get("IsUnicode").asString()).isEqualTo("0");
        assertThat(received.get().get("Sandbox").asString()).isEqualTo("0");
    }

    @Test
    void T026a_code99_orServerError_isTemporary() throws IOException {
        URI endpoint = serve(200, "{\"CodeResult\":\"99\",\"ErrorMessage\":\"Loi ket noi\"}");
        SmsResult r = provider(endpoint, false).send("0912345678", "Noi dung");
        assertThat(r.outcome()).isEqualTo(Outcome.TEMPORARY_FAILURE);
        assertThat(r.providerCode()).contains("99");
        stop();

        URI failing = serve(503, "{}");
        assertThat(provider(failing, false).send("0912345678", "Noi dung").outcome())
                .isEqualTo(Outcome.TEMPORARY_FAILURE);
    }
}
