package com.cambae.backend.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.transaction.support.TransactionTemplate;

/** R-09 (000 T033): sự kiện lưu vào EVENT_PUBLICATION trong transaction và chỉ giao sau commit. */
@Import(EventPublicationIT.Listener.class)
class EventPublicationIT extends MySqlIntegrationTestBase {

    record SampleEvent(String key) {}

    @TestConfiguration
    static class Listener {
        // Tĩnh vì listener bất đồng bộ được bọc proxy, trường của proxy không dùng được
        static final BlockingQueue<String> RECEIVED = new LinkedBlockingQueue<>();

        @ApplicationModuleListener
        void on(SampleEvent event) {
            RECEIVED.add(event.key());
        }
    }

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Listener listener;

    @Test
    void R_09_eventCommitted_isStoredAndDelivered() throws InterruptedException {
        String key = UUID.randomUUID().toString();

        tx.executeWithoutResult(s -> events.publishEvent(new SampleEvent(key)));

        assertThat(Listener.RECEIVED.poll(10, TimeUnit.SECONDS)).isEqualTo(key);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE SERIALIZED_EVENT LIKE ?",
                        Integer.class,
                        "%" + key + "%"))
                .isEqualTo(1);
    }

    @Test
    void R_09_eventRolledBack_isNeitherStoredNorDelivered() throws InterruptedException {
        String key = UUID.randomUUID().toString();

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
                    events.publishEvent(new SampleEvent(key));
                    throw new IllegalStateException("lệnh lỗi");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(Listener.RECEIVED.poll(2, TimeUnit.SECONDS)).isNull();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE SERIALIZED_EVENT LIKE ?",
                        Integer.class,
                        "%" + key + "%"))
                .isZero();
    }
}
