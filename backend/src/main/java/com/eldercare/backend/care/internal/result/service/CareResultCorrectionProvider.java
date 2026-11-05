package com.eldercare.backend.care.internal.result.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.dto.CreationMethod;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.SleepResultRecorded;
import com.eldercare.backend.care.internal.result.domain.*;
import com.eldercare.backend.care.internal.result.persistence.CareResultStore;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** T051: chỉ thêm đính chính; bản gốc và trạng thái công việc giữ nguyên. */
@Component
public class CareResultCorrectionProvider implements CorrectableRecordProvider {
    private static final PermissionKey RECORD = new PermissionKey(Function.TASK_CHECKLIST, Operation.CONFIRM);
    private static final Set<String> FIELDS =
            Set.of("numericValue", "codeValue", "jsonValue", "upperLimitConfirmed", "note");
    private final CareResultStore results;
    private final CareTaskStore tasks;
    private final CareLockStore locks;
    private final PermissionApi permissions;
    private final CareResultValidation validation;
    private final ApplicationEventPublisher events;
    private final CareResultQueries queries;
    private final CurrentUser user;
    private final FieldVisibilityApi visibility;
    private final com.eldercare.backend.care.internal.qualitycheck.service.QualityCheckTaskVoids qualityVoids;

    public CareResultCorrectionProvider(
            CareResultStore results,
            CareTaskStore tasks,
            CareLockStore locks,
            PermissionApi permissions,
            CareResultValidation validation,
            ApplicationEventPublisher events,
            CareResultQueries queries,
            CurrentUser user,
            FieldVisibilityApi visibility,
            com.eldercare.backend.care.internal.qualitycheck.service.QualityCheckTaskVoids qualityVoids) {
        this.results = results;
        this.tasks = tasks;
        this.locks = locks;
        this.permissions = permissions;
        this.validation = validation;
        this.events = events;
        this.queries = queries;
        this.user = user;
        this.visibility = visibility;
        this.qualityVoids = qualityVoids;
    }

    @Override
    public String recordType() {
        return "M04.CARE_RESULT";
    }

    private CareResult original(long id) {
        return results.find(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public boolean isConfirmed(long id) {
        var result = results.find(id);
        if (result.isEmpty()) return false;
        return Set.of(TaskStatus.COMPLETED, TaskStatus.COMPLETED_LATE)
                .contains(tasks.get(id, false).status());
    }

    @Override
    public long originalRecorder(long id) {
        return original(id).recordedBy();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.of(tasks.get(id, false).floorAreaId());
    }

    @Override
    public Optional<Long> residentId(long id) {
        return Optional.ofNullable(original(id).residentId());
    }

    @Override
    public Object originalContent(long id) {
        var task = tasks.get(id, false);
        var target = new Target(recordType(), Long.toString(id), task.residentId(), task.floorAreaId());
        if (visibility.canView(user, InfoGroup.HEALTH, target) != Visibility.FULL)
            throw new PermissionDeniedException("Không có quyền xem đầy đủ giá trị kết quả chăm sóc");
        return queries.original(target, id).orElseThrow(NotFoundException::new).values();
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return permissions.permissions(user).contains(RECORD);
    }

    @Override
    public boolean allows(CurrentUser user, CreationMethod method, long id) {
        // allows chỉ nằm trên lệnh tạo; đọc chain/isConfirmed không thay dữ liệu hoặc đòi isolation ghi.
        requireWriteIsolation();
        var original = original(id);
        if (original.residentId() != null) locks.lock(List.of(original.residentId()));
        var task = tasks.get(id, true);
        if (method == CreationMethod.C || task.data().resultType().equals("MEASURED_VALUE")) return false;
        if (!permissions
                .hasPermission(user, RECORD, CareTaskService.target(task))
                .allowed()) return false;
        if (task.data().billable()) throw unavailable("Chờ 010 thống nhất quantity cho đính chính kết quả tính phí");
        return true;
    }

    private CareResultInput input(Object value) {
        if (!(value instanceof Map<?, ?> map) || !FIELDS.containsAll(map.keySet()))
            throw new InvalidValueException("Chỉ đính chính các trường giá trị kết quả; không đổi metadata");
        BigDecimal numeric = null;
        if (map.get("numericValue") != null) {
            if (!(map.get("numericValue") instanceof Number number))
                throw new InvalidValueException("Giá trị số chưa hợp lệ");
            try {
                numeric = new BigDecimal(number.toString());
            } catch (NumberFormatException e) {
                throw new InvalidValueException("Giá trị số chưa hợp lệ");
            }
        }
        if (map.get("codeValue") != null && !(map.get("codeValue") instanceof String))
            throw new InvalidValueException("Mã kết quả chưa hợp lệ");
        if (map.get("note") != null && !(map.get("note") instanceof String))
            throw new InvalidValueException("Ghi chú chưa hợp lệ");
        if (map.containsKey("upperLimitConfirmed") && !(map.get("upperLimitConfirmed") instanceof Boolean))
            throw new InvalidValueException("Cờ xác nhận vượt giới hạn chưa hợp lệ");
        var values = new LinkedHashMap<String, Object>();
        if (map.get("jsonValue") != null) {
            if (!(map.get("jsonValue") instanceof Map<?, ?> json))
                throw new InvalidValueException("Giá trị có cấu trúc chưa hợp lệ");
            for (var entry : json.entrySet()) {
                if (!(entry.getKey() instanceof String key))
                    throw new InvalidValueException("Khóa giá trị chưa hợp lệ");
                values.put(key, entry.getValue());
            }
        }
        return new CareResultInput(
                null,
                numeric,
                (String) map.get("codeValue"),
                values,
                Boolean.TRUE.equals(map.get("upperLimitConfirmed")),
                (String) map.get("note"),
                null);
    }

    @Override
    public void validateContent(Object correctedContent) {
        input(correctedContent);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void afterCorrection(CorrectionView correction) {
        if (correction.creationMethod() == CreationMethod.C)
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "CORRECTION_NOT_ALLOWED",
                    "Kết quả chăm sóc chỉ hỗ trợ đính chính cách A hoặc B");
        requireWriteIsolation();
        var task = tasks.get(correction.originalRecordId(), false);
        if (task.data().billable()) throw unavailable("Chờ 010 thống nhất quantity cho đính chính kết quả tính phí");
        if (task.data().resultType().equals("MEASURED_VALUE"))
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "CORRECTION_NOT_ALLOWED", "Kết quả đo chỉ số đính chính tại 007");
        if (correction.type() == CorrectionType.VOID) {
            if (task.data().resultType().equals("SLEEP"))
                throw unavailable("Hủy ghi nhận ngủ đang chờ tín hiệu sự kiện của 007");
            // 014 FR-062: mục kiểm tra chất lượng đang chờ của công việc này thành Không kiểm tra được.
            qualityVoids.resultVoided(task.id());
            return;
        }
        var input = input(correction.correctedContent());
        validation.validate(task.data(), input);
        if (task.data().resultType().equals("SLEEP") && task.residentId() != null) {
            var context = new CareEventContext(
                    recordType(),
                    task.id(),
                    "CORRECTION:" + correction.id(),
                    correction.recordedAt(),
                    Optional.of(task.floorAreaId()),
                    List.of());
            events.publishEvent(new SleepResultRecorded(
                    task.id(),
                    task.residentId(),
                    task.plannedAt().atZone(BusinessTime.ZONE).toLocalDate(),
                    validation.catalogGroup("SLEEP_QUALITY", input.codeValue()),
                    context));
        }
        // CorrectionService phát CorrectionMade sau callback; listener Phase8 tính lại quy tắc F đúng một lần.
    }

    private void requireWriteIsolation() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                || !Objects.equals(
                        TransactionSynchronizationManager.getCurrentTransactionIsolationLevel(),
                        Isolation.READ_COMMITTED.value()))
            throw unavailable(
                    "Đính chính kết quả cần transaction READ_COMMITTED; dùng endpoint care-results, đường generic đang chờ phần nền thống nhất isolation");
    }

    private BusinessException unavailable(String message) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "CARE_SOURCE_UNAVAILABLE", message);
    }
}
