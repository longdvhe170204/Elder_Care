package com.eldercare.backend.common.web;

import com.eldercare.backend.common.time.BusinessTime;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Chống lặp lệnh theo header {@code Idempotency-Key} (research R-03, R-13; FR-037): cùng khóa, cùng người gửi thì trả
 * lại kết quả lần đầu, không chạy lệnh lần hai. Chỉ lưu kết quả 2xx; lệnh lỗi thì bỏ khóa để gửi lại được. Cùng khóa
 * mà nội dung khác thì 422 {@code IDEMPOTENCY_KEY_REUSED}; lần đầu còn đang chạy thì 409 {@code IDEMPOTENCY_IN_PROGRESS}.
 */
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER = "Idempotency-Key";
    static final int MAX_KEY_LENGTH = 100;
    public static final Duration RETENTION = Duration.ofDays(7);
    private static final Set<String> METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final List<String> REPLAYED_HEADERS = List.of(HttpHeaders.ETAG, HttpHeaders.LOCATION);

    private final JdbcTemplate jdbc;
    private final BusinessTime time;
    private final JsonMapper json;

    public IdempotencyFilter(JdbcTemplate jdbc, BusinessTime time, JsonMapper json) {
        this.jdbc = jdbc;
        this.time = time;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getHeader(HEADER) == null || !METHODS.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String username = currentUsername();
        if (username == null) {
            chain.doFilter(request, response);
            return;
        }
        String key = request.getHeader(HEADER).strip();
        if (key.isEmpty() || key.length() > MAX_KEY_LENGTH) {
            writeProblem(response, HttpStatus.BAD_REQUEST, "INVALID_VALUE", "Idempotency-Key không hợp lệ");
            return;
        }
        CachedBodyRequest cached = new CachedBodyRequest(request);
        String hash = hash(cached);
        Instant now = time.now();

        jdbc.update(
                "DELETE FROM idempotency_key WHERE key_value = ? AND username = ? AND expires_at <= ?",
                key,
                username,
                now);
        try {
            jdbc.update(
                    "INSERT INTO idempotency_key (key_value, username, request_hash, expires_at) VALUES (?, ?, ?, ?)",
                    key,
                    username,
                    hash,
                    now.plus(RETENTION));
        } catch (DuplicateKeyException e) {
            replayOrReject(response, key, username, hash);
            return;
        }

        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        boolean stored = false;
        try {
            chain.doFilter(cached, wrapped);
            if (HttpStatus.valueOf(wrapped.getStatus()).is2xxSuccessful()) {
                jdbc.update(
                        "UPDATE idempotency_key SET result_json = ? WHERE key_value = ? AND username = ?",
                        json.writeValueAsString(snapshot(wrapped)),
                        key,
                        username);
                stored = true;
            }
        } finally {
            if (!stored) {
                jdbc.update("DELETE FROM idempotency_key WHERE key_value = ? AND username = ?", key, username);
            }
            wrapped.copyBodyToResponse();
        }
    }

    private void replayOrReject(HttpServletResponse response, String key, String username, String hash)
            throws IOException {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT request_hash, result_json FROM idempotency_key WHERE key_value = ? AND username = ?",
                key,
                username);
        if (!hash.equals(row.get("request_hash"))) {
            writeProblem(
                    response,
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key đã dùng cho một yêu cầu khác");
            return;
        }
        Object result = row.get("result_json");
        if (result == null) {
            writeProblem(response, HttpStatus.CONFLICT, "IDEMPOTENCY_IN_PROGRESS", "Yêu cầu trước đang được xử lý");
            return;
        }
        Map<?, ?> saved = json.readValue(result.toString(), Map.class);
        response.setStatus(((Number) saved.get("status")).intValue());
        if (saved.get("contentType") != null) {
            response.setContentType(saved.get("contentType").toString());
        }
        if (saved.get("headers") instanceof Map<?, ?> headers) {
            headers.forEach((name, value) -> response.setHeader(name.toString(), value.toString()));
        }
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(saved.get("body").toString());
    }

    private static Map<String, Object> snapshot(ContentCachingResponseWrapper response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : REPLAYED_HEADERS) {
            if (response.getHeader(name) != null) {
                headers.put(name, response.getHeader(name));
            }
        }
        Map<String, Object> saved = new LinkedHashMap<>();
        saved.put("status", response.getStatus());
        saved.put("contentType", response.getContentType());
        saved.put("headers", headers);
        saved.put("body", new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
        return saved;
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth.getName();
    }

    private static String hash(CachedBodyRequest request) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String target = request.getMethod() + " " + request.getRequestURI() + "?"
                    + (request.getQueryString() == null ? "" : request.getQueryString()) + "\n";
            digest.update(target.getBytes(StandardCharsets.UTF_8));
            if (request.parts == null) {
                digest.update(request.body);
            } else {
                // Băm phần thực, không băm boundary do trình duyệt tạo lại khi gửi lại cùng file.
                for (Part part : request.parts.stream()
                        .sorted(Comparator.comparing(Part::getName)
                                .thenComparing(p -> p.getSubmittedFileName() == null ? "" : p.getSubmittedFileName()))
                        .toList()) {
                    for (String value :
                            new String[] {part.getName(), part.getSubmittedFileName(), part.getContentType()}) {
                        byte[] metadata = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
                        digest.update(ByteBuffer.allocate(Integer.BYTES)
                                .putInt(metadata.length)
                                .array());
                        digest.update(metadata);
                    }
                    digest.update(ByteBuffer.allocate(Long.BYTES)
                            .putLong(part.getSize())
                            .array());
                    try (var input = part.getInputStream()) {
                        byte[] chunk = new byte[8192];
                        int read;
                        while ((read = input.read(chunk)) != -1) digest.update(chunk, 0, read);
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void writeProblem(HttpServletResponse response, HttpStatus status, String errorCode, String detail)
            throws IOException {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "about:blank");
        problem.put("title", status.getReasonPhrase());
        problem.put("status", status.value());
        problem.put("detail", detail);
        problem.put("errorCode", errorCode);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json.writeValueAsString(problem));
    }

    /** JSON đọc lại từ bộ nhớ; multipart giữ các phần servlet đã phân tích để controller vẫn nhận được file. */
    static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;
        private final Collection<Part> parts;

        CachedBodyRequest(HttpServletRequest request) throws IOException, ServletException {
            super(request);
            if (request.getContentType() != null
                    && request.getContentType().toLowerCase(Locale.ROOT).startsWith("multipart/")) {
                this.parts = List.copyOf(request.getParts());
                this.body = new byte[0];
            } else {
                this.parts = null;
                this.body = request.getInputStream().readAllBytes();
            }
        }

        @Override
        public Collection<Part> getParts() throws IOException, ServletException {
            return parts == null ? super.getParts() : parts;
        }

        @Override
        public Part getPart(String name) throws IOException, ServletException {
            return parts == null
                    ? super.getPart(name)
                    : parts.stream()
                            .filter(p -> p.getName().equals(name))
                            .findFirst()
                            .orElse(null);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read() {
                    return in.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
