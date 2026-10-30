package com.eldercare.backend.support;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

/**
 * Bắt các dòng log của logger {@code jobs} ({@link SchedulerRuns}) trong test, thay cho đọc bảng {@code scheduler_run}
 * đã bỏ (research R-15 mục 4). Dùng trong {@code try (var log = new JobLogCapture()) { … }}.
 */
public final class JobLogCapture implements AutoCloseable {

    private final Logger logger = (Logger) LoggerFactory.getLogger("jobs");
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    public JobLogCapture() {
        appender.start();
        logger.addAppender(appender);
    }

    /** Các dòng của lần chạy {@code runId}: cặp khóa giá trị của dòng, thêm {@code message}. */
    public List<Map<String, Object>> events(String runId) {
        return appender.list.stream()
                .filter(e -> runId.equals(e.getMDCPropertyMap().get(SchedulerRuns.MDC_KEY)))
                .map(e -> {
                    Map<String, Object> values = new HashMap<>();
                    List<KeyValuePair> pairs = e.getKeyValuePairs();
                    if (pairs != null) {
                        pairs.forEach(p -> values.put(p.key, p.value));
                    }
                    values.put("message", e.getFormattedMessage());
                    return values;
                })
                .toList();
    }

    /** Kết quả của lần chạy ({@code SUCCEEDED}, {@code FAILED}), {@code null} khi chưa kết thúc. */
    public Object result(String runId) {
        return events(runId).stream()
                .filter(e -> "FINISH".equals(e.get("event")))
                .map(e -> e.get("result"))
                .findFirst()
                .orElse(null);
    }

    /** Số lần chạy của {@code job} đã bắt đầu từ lúc mở bộ bắt log. */
    public long starts(String job) {
        return appender.list.stream()
                .filter(e -> e.getKeyValuePairs() != null
                        && e.getKeyValuePairs().stream().anyMatch(p -> p.key.equals("event") && "START".equals(p.value))
                        && e.getKeyValuePairs().stream().anyMatch(p -> p.key.equals("job") && job.equals(p.value)))
                .count();
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
    }
}
