package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.roombedallocation.api.events.BedAllocationCorrected;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationCorrectionOriginal;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationCorrectionStore;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** T047: generic SPI chưa truyền id vào validateContent; kiểm FR-019 ở callback cùng transaction để rollback. */
@Component
public class AllocationCorrectionProvider implements CorrectableRecordProvider {
    private static final Set<String> FIELDS = Set.of("bedId", "startAt", "endAt");
    private final AllocationCorrectionStore store;
    private final AllocationLocks locks;
    private final PermissionApi permissions;
    private final ObjectProvider<CorrectionApi> corrections;
    private final ApplicationEventPublisher events;
    private final tools.jackson.databind.json.JsonMapper json;

    public AllocationCorrectionProvider(
            AllocationCorrectionStore store,
            AllocationLocks locks,
            PermissionApi permissions,
            ObjectProvider<CorrectionApi> corrections,
            ApplicationEventPublisher events,
            tools.jackson.databind.json.JsonMapper json) {
        this.store = store;
        this.locks = locks;
        this.permissions = permissions;
        this.corrections = corrections;
        this.events = events;
        this.json = json;
    }

    @Override
    public String recordType() {
        return "M03.BED_ALLOCATION";
    }

    @Override
    public boolean isConfirmed(long id) {
        return store.find(id)
                .map(row -> Set.of("CLOSED", "CANCELLED").contains(row.status()))
                .orElse(false);
    }

    @Override
    public long originalRecorder(long id) {
        var recorder = store.required(id).recorder();
        return recorder == null ? 0 : recorder;
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.of(store.required(id).floorId());
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        var result = permissions.hasPermission(
                user, new PermissionKey(Function.BED_ALLOCATION, Operation.CREATE), Target.UNSPECIFIED);
        return result.allowed()
                || result.failedLayer()
                        .filter(layer -> layer == FailedLayer.SCOPE)
                        .isPresent();
    }

    @Override
    public boolean allows(CurrentUser user, CreationMethod method, long id) {
        if (method == CreationMethod.B) return true;
        var row = store.required(id);
        return permissions
                .hasPermission(
                        user,
                        new PermissionKey(Function.BED_ALLOCATION, Operation.CREATE),
                        new Target(recordType(), Long.toString(id), row.residentId(), row.floorId()))
                .allowed();
    }

    @Override
    public Object originalContent(long id) {
        return snapshot(store.required(id));
    }

    @Override
    public void validateContent(Object value) {
        if (!(value instanceof Map<?, ?> map) || map.isEmpty() || !FIELDS.containsAll(map.keySet()))
            throw new InvalidValueException("Chỉ đính chính giường, bắt đầu, kết thúc");
        if (map.containsKey("bedId")) bedId(map.get("bedId"));
        if (map.containsKey("startAt")) timestamp(map.get("startAt"), false);
        if (map.containsKey("endAt")) timestamp(map.get("endAt"), true);
    }

    @Override
    public void afterCorrection(CorrectionView correction) {
        var original = store.required(correction.originalRecordId());
        var after = snapshot(original);
        if (correction.type() == CorrectionType.VOID) after.clear();
        else ((Map<?, ?>) correction.correctedContent()).forEach((key, value) -> after.put((String) key, value));
        long bed = after.isEmpty() ? original.bedId() : bedId(after.get("bedId"));
        locks.lock(original.residentId(), List.of(original.bedId(), bed));
        if (!after.isEmpty()) {
            var from = timestamp(after.get("startAt"), false);
            var end = timestamp(after.get("endAt"), true);
            if ("CLOSED".equals(original.status()) && (end == null || !from.isBefore(end)))
                throw new InvalidValueException("Phân bổ đã đóng cần kết thúc sau bắt đầu");
            Instant effectiveEnd = end == null ? original.expectedEnd() : end;
            if (effectiveEnd != null && !from.isBefore(effectiveEnd))
                throw new InvalidValueException("Khoảng phân bổ không hợp lệ");
            if (!"CANCELLED".equals(original.status())
                    && store.overlaps(original.id(), original.residentId(), bed, from, effectiveEnd))
                throw new BusinessException(
                        HttpStatus.CONFLICT, "ALLOCATION_TIME_OVERLAP", "Đính chính làm chồng thời gian phân bổ");
        }
        var before = snapshot(original);
        var chain = corrections.getObject().chain(recordType(), original.id());
        var previous = chain.corrections().stream()
                .filter(row -> row.sequenceNo() < correction.sequenceNo())
                .max(Comparator.comparingInt(CorrectionView::sequenceNo));
        Object previousContent = previous.map(CorrectionView::correctedContent).orElse(null);
        if (!Integer.valueOf(java.sql.Connection.TRANSACTION_READ_COMMITTED)
                .equals(org.springframework.transaction.support.TransactionSynchronizationManager
                        .getCurrentTransactionIsolationLevel())) {
            var committed = store.latestCommitted(original.id());
            // Chuỗi đã được 000 khóa; id tăng theo sequence trong cùng chuỗi. Giữ cả prior chưa commit của parent.
            if (committed.isPresent()
                    && committed.get().correctionId()
                            > previous.map(CorrectionView::id).orElse(0L))
                previousContent = json.readValue(committed.get().content(), Object.class);
        }
        if (previousContent instanceof Map<?, ?> content)
            content.forEach((key, value) -> before.put((String) key, value));
        events.publishEvent(new BedAllocationCorrected(
                original.id(), Collections.unmodifiableMap(before), Collections.unmodifiableMap(after)));
    }

    private static Map<String, Object> snapshot(AllocationCorrectionOriginal row) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("bedId", row.bedId());
        content.put("startAt", row.startAt().toString());
        content.put("endAt", row.endAt() == null ? null : row.endAt().toString());
        return content;
    }

    private static long bedId(Object value) {
        if (!(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
                || ((Number) value).longValue() <= 0) throw new InvalidValueException("Giường phải là số nguyên dương");
        return ((Number) value).longValue();
    }

    private static Instant timestamp(Object value, boolean nullable) {
        if (value == null && nullable) return null;
        try {
            String text = (String) value;
            Instant parsed = Instant.parse(text);
            int year = java.time.LocalDateTime.ofInstant(parsed, java.time.ZoneOffset.UTC)
                    .getYear();
            if (!text.endsWith("Z")
                    || !parsed.equals(parsed.truncatedTo(java.time.temporal.ChronoUnit.MICROS))
                    || year < 1000
                    || year > 9999) throw new IllegalArgumentException();
            return parsed;
        } catch (Exception error) {
            throw new InvalidValueException(
                    "Nội dung đính chính dùng ISO-8601 UTC Z tới microsecond, trong miền DATETIME của MySQL");
        }
    }
}
