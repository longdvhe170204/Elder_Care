package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

/** FR-001b/FR-037: multipart thật qua servlet và chống lặp, không chỉ MockMvc. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StaffImportTransportIT extends MySqlIntegrationTestBase {
    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Test
    void FR_001b_realMultipartConfirmAndRetryDoNotLoseFileOrDuplicateProfiles() throws Exception {
        var users = new TestUsers(jdbc, time);
        String bearer = TestUsers.bearer(
                users.session(users.staff(Role.NURSING_HOME_MANAGER).accountId()));
        String phone = String.format("07%08d", TestUsers.nextId() % 100000000L);
        byte[] file = StaffImportIT.file(
                List.of(Arrays.asList("[Import servlet]", "Điều dưỡng", phone, "25/10/2026", null, null)));
        String key = UUID.randomUUID().toString();
        try (var client = HttpClient.newHttpClient()) {
            var first = send(client, bearer, key, "ElderCareFirst", file);
            assertThat(first.statusCode()).as(first.body()).isEqualTo(201);
            var repeat = send(client, bearer, key, "ElderCareBoundaryChanged", file);
            assertThat(repeat.statusCode()).isEqualTo(201);
            assertThat(repeat.body()).isEqualTo(first.body());
            byte[] changed = StaffImportIT.file(
                    List.of(Arrays.asList("[Nội dung khác]", "Điều dưỡng", phone, "25/10/2026", null, null)));
            var rejected = send(client, bearer, key, "ElderCareThird", changed);
            assertThat(rejected.statusCode()).isEqualTo(422);
            assertThat(rejected.body()).contains("IDEMPOTENCY_KEY_REUSED");
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM staff_profile WHERE normalized_phone=?", Integer.class, phone))
                    .isOne();
        }
    }

    private HttpResponse<String> send(HttpClient client, String bearer, String key, String boundary, byte[] file)
            throws Exception {
        var out = new ByteArrayOutputStream();
        out.write(("--" + boundary
                        + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"staff.xlsx\"\r\nContent-Type: "
                        + StaffImportIT.MIME + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write(file);
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/staff/import/confirm"))
                .header("Authorization", bearer)
                .header("Idempotency-Key", key)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
