package com.cambae.backend.m13.internal.delivery.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m13.internal.delivery.domain.AttemptContext;
import com.cambae.backend.m13.internal.delivery.domain.SendResult;
import com.cambae.backend.m13.internal.delivery.persistence.DeliveryStore;
import com.cambae.backend.m13.internal.domain.DeliveryOutcome;
import com.cambae.backend.m13.internal.persistence.NotificationStore;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bộ phát ({@code BoPhatKenh}; research B-03, NFR-03): gửi các lần gửi song song trên luồng ảo sau commit; mỗi kết quả
 * ghi một dòng trong transaction ngắn và tính lại trạng thái. Khẩn cấp có hàng riêng, không giới hạn, nên không xếp sau
 * thông báo thường đang dồn; thông báo thường giới hạn số lần gửi đồng thời để không chiếm hết kết nối cơ sở dữ liệu.
 */
@Service
public class DeliveryDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryDispatcher.class);

    /** Số lần gửi thường chạy đồng thời (thông số kỹ thuật). */
    static final int NORMAL_CONCURRENCY = 4;

    private final DeliveryStore deliveries;
    private final NotificationStore notifications;
    private final TransactionTemplate tx;
    private final BusinessTime time;
    private final Map<String, ChannelSender> senders;
    private final ExecutorService urgent = Executors.newVirtualThreadPerTaskExecutor();
    private final ExecutorService normal = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore normalSlots = new Semaphore(NORMAL_CONCURRENCY);
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    DeliveryDispatcher(
            DeliveryStore deliveries,
            NotificationStore notifications,
            TransactionTemplate tx,
            BusinessTime time,
            List<ChannelSender> senders) {
        this.deliveries = deliveries;
        this.notifications = notifications;
        this.tx = tx;
        this.time = time;
        this.senders = senders.stream()
                .flatMap(s -> s.channels().stream().map(c -> Map.entry(c, s)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** Gửi các lần gửi; trong transaction thì chờ tới sau commit, rollback thì không gửi. */
    public void dispatch(List<Long> attemptIds) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit(attemptIds);
                }
            });
        } else {
            submit(attemptIds);
        }
    }

    private void submit(List<Long> attemptIds) {
        for (long id : attemptIds.stream().distinct().toList()) {
            if (!inFlight.add(id)) {
                continue;
            }
            Optional<AttemptContext> attempt;
            try {
                attempt = deliveries.pending(id);
            } catch (RuntimeException e) {
                inFlight.remove(id);
                log.warn("Không đọc được lần gửi {}", id, e);
                continue;
            }
            if (attempt.isEmpty()) {
                inFlight.remove(id);
                continue;
            }
            AttemptContext a = attempt.get();
            if (a.urgent()) {
                urgent.execute(() -> send(a));
            } else {
                normal.execute(() -> {
                    normalSlots.acquireUninterruptibly();
                    try {
                        send(a);
                    } finally {
                        normalSlots.release();
                    }
                });
            }
        }
    }

    private void send(AttemptContext a) {
        try {
            SendResult result = Optional.ofNullable(senders.get(a.channel()))
                    .map(sender -> safeSend(sender, a))
                    .orElseGet(() -> SendResult.of(DeliveryOutcome.CHANNEL_NOT_CONFIGURED));
            tx.executeWithoutResult(s -> {
                if (deliveries.insertResult(a.attemptId(), result, time.now())) {
                    notifications.recompute(a.notificationId());
                }
            });
        } catch (RuntimeException e) {
            // Lần gửi còn không có kết quả; job gửi lại xét sau (FR-018)
            log.warn("Ghi kết quả lần gửi {} lỗi", a.attemptId(), e);
        } finally {
            inFlight.remove(a.attemptId());
        }
    }

    private static SendResult safeSend(ChannelSender sender, AttemptContext a) {
        try {
            return sender.send(a);
        } catch (RuntimeException e) {
            return new SendResult(DeliveryOutcome.FAILED, Optional.ofNullable(e.getMessage()));
        }
    }

    @PreDestroy
    void close() {
        urgent.close();
        normal.close();
    }
}
