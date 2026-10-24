package com.cambae.backend.m01.internal.health.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m01.api.HealthRecordApi;
import com.cambae.backend.m01.api.dto.InitialHealthRecordView;
import com.cambae.backend.m01.internal.health.domain.InitialHealthRecord;
import com.cambae.backend.m01.internal.health.domain.InitialHealthRecordContent;
import com.cambae.backend.m01.internal.health.persistence.InitialHealthRecordRepository;
import com.cambae.backend.m01.internal.profile.persistence.ResidentRepository;
import com.cambae.backend.m01.internal.status.service.ProfileOpenGuard;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CurrentValue;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hồ sơ sức khỏe ban đầu (001 US2; FR-017; research H-08): bác sĩ hoặc điều dưỡng lập và sửa bản nháp, chỉ bác sĩ xác
 * nhận; sau xác nhận chỉ đính chính qua 000 (loại {@link #RECORD_TYPE}). Không là điều kiện chặn Hoàn tất tiếp nhận.
 * Cài {@link HealthRecordApi} cho 006: thuốc đang dùng khi tiếp nhận, theo giá trị hiện hành sau đính chính.
 */
@Service
public class InitialHealthRecordService implements HealthRecordApi {

    public static final String RECORD_TYPE = "M01.INITIAL_HEALTH_RECORD";

    static final AuditAction CREATED = new AuditAction("INITIAL_HEALTH_RECORD_CREATED", false);
    static final AuditAction DRAFT_EDITED = new AuditAction("INITIAL_HEALTH_RECORD_DRAFT_EDITED", false);
    static final AuditAction CONFIRMED = new AuditAction("INITIAL_HEALTH_RECORD_CONFIRMED", false);
    private static final String ENTITY_TYPE = RECORD_TYPE;
    private static final int MAX_BLOOD_TYPE_LENGTH = 10;

    /** Hồ sơ cho client: nội dung hiện hành (đã tính đính chính), người xác nhận, ETag của bản gốc. */
    public record RecordView(
            long id,
            long residentId,
            InitialHealthRecordContent content,
            boolean confirmed,
            String confirmedBy,
            boolean corrected,
            int version) {}

    private final InitialHealthRecordRepository records;
    private final ResidentRepository residents;
    private final ProfileOpenGuard guard;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final ActorNames names;
    private final CorrectionApi corrections;
    private final JsonMapper json;
    private final ObjectProvider<CurrentUser> currentUser;

    InitialHealthRecordService(
            InitialHealthRecordRepository records,
            ResidentRepository residents,
            ProfileOpenGuard guard,
            AuditLogApi auditLog,
            BusinessTime time,
            ActorNames names,
            CorrectionApi corrections,
            JsonMapper json,
            ObjectProvider<CurrentUser> currentUser) {
        this.records = records;
        this.residents = residents;
        this.guard = guard;
        this.auditLog = auditLog;
        this.time = time;
        this.names = names;
        this.corrections = corrections;
        this.json = json;
        this.currentUser = currentUser;
    }

    @RequiresPermission(function = Function.ALLERGY_CONDITION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Optional<RecordView> get(Target target) {
        return records.findByResidentId(target.residentId()).map(this::view);
    }

    /** Lập bản nháp lần đầu, hoặc sửa bản nháp kèm {@code If-Match}; đã xác nhận thì 409 {@code ALREADY_CONFIRMED}. */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ALLERGY_CONDITION, operation = Operation.UPDATE)
    @Transactional
    public RecordView saveDraft(Target target, String ifMatch, InitialHealthRecordContent input) {
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        InitialHealthRecordContent content = normalized(input);
        long user = currentUser.getObject().accountId();
        Optional<InitialHealthRecord> existing = records.findByResidentId(residentId);
        if (existing.isEmpty()) {
            InitialHealthRecord created =
                    records.saveAndFlush(new InitialHealthRecord(residentId, content, user, time.now()));
            auditLog.write(AuditLogEntry.builder()
                    .action(CREATED)
                    .entity(ENTITY_TYPE, created.getId())
                    .resident(residentId)
                    .after(snapshot(content))
                    .build());
            return view(created);
        }
        InitialHealthRecord record = existing.get();
        requireDraft(record);
        VersionEtag.requireMatch(ifMatch, record.getVersion(), view(record));
        InitialHealthRecordContent before = record.content();
        record.editDraft(content);
        records.saveAndFlush(record);
        auditLog.write(AuditLogEntry.builder()
                .action(DRAFT_EDITED)
                .entity(ENTITY_TYPE, record.getId())
                .resident(residentId)
                .before(snapshot(before))
                .after(snapshot(content))
                .build());
        return view(record);
    }

    /** Chỉ Bác sĩ xác nhận (AC-03); sau đó trigger chặn sửa. */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ALLERGY_CONDITION, operation = Operation.UPDATE)
    @Transactional
    public RecordView confirm(Target target, String ifMatch) {
        long residentId = target.residentId();
        CurrentUser user = currentUser.getObject();
        if (!user.roles().contains(Role.DOCTOR)) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED", "Chỉ Bác sĩ xác nhận hồ sơ sức khỏe ban đầu", Map.of("failedLayer", "ROLE"));
        }
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        InitialHealthRecord record = records.findByResidentId(residentId).orElseThrow(NotFoundException::new);
        requireDraft(record);
        VersionEtag.requireMatch(ifMatch, record.getVersion(), view(record));
        Instant now = time.now();
        record.confirm(user.accountId(), now);
        records.saveAndFlush(record);
        auditLog.write(AuditLogEntry.builder()
                .action(CONFIRMED)
                .entity(ENTITY_TYPE, record.getId())
                .resident(residentId)
                .after(Map.of("confirmedAt", now.toString()))
                .build());
        return view(record);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InitialHealthRecordView> initial(long residentId) {
        return records.findByResidentId(residentId).map(r -> {
            RecordView v = view(r);
            return new InitialHealthRecordView(v.id(), residentId, v.content().currentMedications(), v.confirmed());
        });
    }

    private static void requireDraft(InitialHealthRecord record) {
        if (record.isConfirmed()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "ALREADY_CONFIRMED",
                    "Hồ sơ sức khỏe ban đầu đã xác nhận, chỉ sửa bằng đính chính");
        }
    }

    private RecordView view(InitialHealthRecord record) {
        InitialHealthRecordContent content = record.content();
        boolean corrected = false;
        if (record.isConfirmed()) {
            CurrentValue current =
                    corrections.current(RECORD_TYPE, List.of(record.getId())).get(record.getId());
            if (current != null && current.corrected()) {
                corrected = true;
                if (current.content() != null) {
                    // Nội dung đúng từ form đính chính có thể mang chuỗi rỗng cho ô để trống
                    content = normalized(json.convertValue(current.content(), InitialHealthRecordContent.class));
                }
            }
        }
        return new RecordView(
                record.getId(),
                record.getResidentId(),
                content,
                record.isConfirmed(),
                record.getConfirmedBy() == null ? null : names.of(record.getConfirmedBy()),
                corrected,
                record.getVersion());
    }

    /** Chuỗi rỗng coi như không có; nhóm máu tối đa 10 ký tự (FR-017). */
    private static InitialHealthRecordContent normalized(InitialHealthRecordContent in) {
        if (in == null) {
            throw new InvalidValueException("Thiếu nội dung hồ sơ sức khỏe ban đầu");
        }
        String bloodType = blankToNull(in.bloodType());
        if (bloodType != null && bloodType.length() > MAX_BLOOD_TYPE_LENGTH) {
            throw new InvalidValueException("Nhóm máu tối đa " + MAX_BLOOD_TYPE_LENGTH + " ký tự");
        }
        return new InitialHealthRecordContent(
                bloodType,
                blankToNull(in.mobility()),
                blankToNull(in.selfCareAbility()),
                blankToNull(in.cognition()),
                blankToNull(in.initialNeedsNote()),
                blankToNull(in.nutritionNeeds()),
                blankToNull(in.currentMedications()),
                blankToNull(in.rehabilitationNeeds()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    static Map<String, Object> snapshot(InitialHealthRecordContent c) {
        Map<String, Object> values = new LinkedHashMap<>();
        put(values, "bloodType", c.bloodType());
        put(values, "mobility", c.mobility());
        put(values, "selfCareAbility", c.selfCareAbility());
        put(values, "cognition", c.cognition());
        put(values, "initialNeedsNote", c.initialNeedsNote());
        put(values, "nutritionNeeds", c.nutritionNeeds());
        put(values, "currentMedications", c.currentMedications());
        put(values, "rehabilitationNeeds", c.rehabilitationNeeds());
        return values;
    }

    private static void put(Map<String, Object> values, String field, String value) {
        if (value != null) {
            values.put(field, value);
        }
    }
}
