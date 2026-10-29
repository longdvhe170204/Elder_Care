package com.eldercare.backend.resident.internal.health.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditAction;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.CatalogBase;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.dto.HealthItemCheckSource;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.dto.HealthItemView;
import com.eldercare.backend.resident.api.dto.Warning;
import com.eldercare.backend.resident.api.events.AllergyExcluded;
import com.eldercare.backend.resident.api.events.AllergyRecorded;
import com.eldercare.backend.resident.api.events.OtherAllergyRecorded;
import com.eldercare.backend.resident.internal.health.domain.HealthItem;
import com.eldercare.backend.resident.internal.health.domain.HealthItemStatus;
import com.eldercare.backend.resident.internal.health.persistence.AllergenRepository;
import com.eldercare.backend.resident.internal.health.persistence.AllergySeverityRepository;
import com.eldercare.backend.resident.internal.health.persistence.HealthItemRepository;
import com.eldercare.backend.resident.internal.health.persistence.InformationSourceRepository;
import com.eldercare.backend.resident.internal.profile.persistence.ResidentRepository;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import com.eldercare.backend.resident.internal.status.service.ProfileOpenGuard;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mục sức khỏe: dị ứng, bệnh nền, tiền sử (001 US2; FR-018 → FR-022; research H-07). Mỗi mục là một dòng, nội dung bất
 * biến; sai thì loại trừ rồi ghi mục mới (DBR-04). Dị ứng chọn từ danh mục dị nguyên đặt hai cờ chờ kiểm tra và phát
 * {@link AllergyRecorded} để 006 (đơn thuốc), 011 (chế độ ăn) kiểm tra sau khi lệnh commit; mục vẫn được lưu dù hai module
 * đó chưa chạy (BR-M01-12). Dị ứng loại "khác" nhập tay không kiểm tra tự động và kèm cảnh báo (FR-018a). Cài
 * {@link HealthItemApi} cho 006, 007, 011.
 */
@Service
public class HealthItemService implements HealthItemApi {

    static final AuditAction RECORDED = new AuditAction("HEALTH_ITEM_RECORDED", false);
    static final AuditAction EXCLUDED = new AuditAction("HEALTH_ITEM_EXCLUDED", true);
    static final AuditAction CHECKED = new AuditAction("HEALTH_ITEM_CHECKED", false);
    private static final String ENTITY_TYPE = "M01.HEALTH_ITEM";
    private static final String CHECK_RULE = "BR-M01-12";
    private static final int MAX_TEXT_LENGTH = 1000;

    /** Lọc theo trạng thái của danh sách (contracts: ACTIVE, EXCLUDED, ALL). */
    public enum StatusFilter {
        ACTIVE,
        EXCLUDED,
        ALL
    }

    /** Lệnh ghi nhận một mục; {@code allergenId} hoặc {@code freeText} cho dị ứng, {@code freeText} cho loại còn lại. */
    public record RecordCommand(
            HealthItemType type,
            Long allergenId,
            String freeText,
            Long severityId,
            long sourceId,
            boolean confirmDuplicate) {}

    public record CatalogRef(long id, String name) {}

    public record Exclusion(Instant at, String by, String reason) {}

    public record ItemView(
            long id,
            HealthItemType type,
            CatalogRef allergen,
            String freeText,
            boolean notAutoChecked,
            boolean pendingCheck,
            String severity,
            String source,
            String recordedBy,
            Instant at,
            HealthItemStatus status,
            Exclusion exclusion) {}

    public record RecordResult(ItemView item, List<Warning> warnings) {}

    private final HealthItemRepository items;
    private final ResidentRepository residents;
    private final AllergenRepository allergens;
    private final AllergySeverityRepository severities;
    private final InformationSourceRepository sources;
    private final ProfileOpenGuard guard;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ActorNames names;
    private final ObjectProvider<CurrentUser> currentUser;

    HealthItemService(
            HealthItemRepository items,
            ResidentRepository residents,
            AllergenRepository allergens,
            AllergySeverityRepository severities,
            InformationSourceRepository sources,
            ProfileOpenGuard guard,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            ActorNames names,
            ObjectProvider<CurrentUser> currentUser) {
        this.items = items;
        this.residents = residents;
        this.allergens = allergens;
        this.severities = severities;
        this.sources = sources;
        this.guard = guard;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.names = names;
        this.currentUser = currentUser;
    }

    /** Đối tượng kiểm quyền của hồ sơ chứa mục (đường dẫn {@code /health-items/{id}} không nêu hồ sơ). */
    @Transactional(readOnly = true)
    public Target targetOf(long itemId) {
        return ResidentProfileService.target(
                items.findById(itemId).orElseThrow(NotFoundException::new).getResidentId());
    }

    /** Người thân chỉ thấy mục Hiệu lực (FR-022), bất kể bộ lọc. */
    @RequiresPermission(function = Function.ALLERGY_CONDITION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<ItemView> list(Target target, StatusFilter filter) {
        StatusFilter effective =
                currentUser.getObject().type() == AccountType.FAMILY_MEMBER ? StatusFilter.ACTIVE : filter;
        long residentId = target.residentId();
        List<HealthItem> found = switch (effective) {
            case ACTIVE -> items.findByResidentIdAndStatusOrderByIdDesc(residentId, HealthItemStatus.ACTIVE);
            case EXCLUDED -> items.findByResidentIdAndStatusOrderByIdDesc(residentId, HealthItemStatus.EXCLUDED);
            case ALL -> items.findByResidentIdOrderByIdDesc(residentId);
        };
        return views(found);
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ALLERGY_CONDITION, operation = Operation.CREATE)
    @Transactional
    public RecordResult record(Target target, RecordCommand command) {
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        HealthItemType type = validated(command);
        String text = blankToNull(command.freeText());
        if (!command.confirmDuplicate()) {
            findDuplicate(residentId, type, command.allergenId(), text).ifPresent(existing -> {
                throw duplicate(existing);
            });
        }
        HealthItem item = items.saveAndFlush(new HealthItem(
                residentId,
                type,
                command.allergenId(),
                text,
                type == HealthItemType.ALLERGY ? command.severityId() : null,
                command.sourceId(),
                currentUser.getObject().accountId(),
                time.now()));
        auditLog.write(AuditLogEntry.builder()
                .action(RECORDED)
                .entity(ENTITY_TYPE, item.getId())
                .resident(residentId)
                .after(snapshot(item))
                .build());
        List<Warning> warnings = new ArrayList<>();
        if (item.getAllergenId() != null) {
            events.publishEvent(new AllergyRecorded(item.getId(), residentId, item.getAllergenId()));
        } else if (item.isNotAutoChecked()) {
            events.publishEvent(new OtherAllergyRecorded(item.getId(), residentId));
            warnings.add(new Warning(
                    "ALLERGY_NOT_AUTO_CHECKED",
                    "Dị ứng nhập tay không có trong danh mục: xung đột với thuốc và chế độ ăn sẽ không được phát hiện"
                            + " tự động"));
        }
        return new RecordResult(views(List.of(item)).getFirst(), warnings);
    }

    /** Loại trừ một chiều, lý do bắt buộc; không sửa, không xóa (FR-019). */
    @BusinessCommand
    @RequiresPermission(function = Function.ALLERGY_CONDITION, operation = Operation.UPDATE)
    @Transactional
    public ItemView exclude(Target target, long itemId, Reason reason) {
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        HealthItem item = items.lockById(itemId)
                .filter(i -> i.getResidentId() == residentId)
                .orElseThrow(NotFoundException::new);
        if (!item.isActive()) {
            throw new BusinessException(HttpStatus.CONFLICT, "ALREADY_EXCLUDED", "Mục đã được loại trừ");
        }
        Instant now = time.now();
        long user = currentUser.getObject().accountId();
        Map<String, Object> before = snapshot(item);
        item.exclude(user, now, reason.value());
        items.saveAndFlush(item);
        auditLog.write(AuditLogEntry.builder()
                .action(EXCLUDED)
                .entity(ENTITY_TYPE, itemId)
                .resident(residentId)
                .before(before)
                .after(snapshot(item))
                .reason(reason)
                .build());
        if (item.getType() == HealthItemType.ALLERGY) {
            events.publishEvent(new AllergyExcluded(itemId, residentId));
        }
        return views(List.of(item)).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthItemView> active(long residentId, HealthItemType... types) {
        Set<HealthItemType> wanted = types == null || types.length == 0
                ? EnumSet.allOf(HealthItemType.class)
                : EnumSet.copyOf(List.of(types));
        return items
                .findByResidentIdAndStatusAndTypeInOrderByIdAsc(residentId, HealthItemStatus.ACTIVE, wanted)
                .stream()
                .map(i -> new HealthItemView(
                        i.getId(),
                        i.getResidentId(),
                        i.getType(),
                        Optional.ofNullable(i.getAllergenId()),
                        Optional.ofNullable(i.getFreeText()),
                        i.isNotAutoChecked(),
                        i.isPendingCheck()))
                .toList();
    }

    /** 006, 011 gỡ cờ của mình; chạy lại cho mục đã gỡ cờ thì không đổi gì (idempotent). */
    @Override
    @Transactional
    public void markChecked(long itemId, HealthItemCheckSource source) {
        HealthItem item = items.lockById(itemId).orElseThrow(NotFoundException::new);
        boolean pendingBefore = item.isPendingCheck();
        item.markChecked(source);
        items.saveAndFlush(item);
        auditLog.write(AuditLogEntry.builder()
                .action(CHECKED)
                .entity(ENTITY_TYPE, itemId)
                .resident(item.getResidentId())
                .details(Map.of(
                        "source", source.name(), "stillPending", item.isPendingCheck(), "wasPending", pendingBefore))
                .basis(new Basis(CHECK_RULE, null, itemId, null))
                .build());
    }

    private HealthItemType validated(RecordCommand c) {
        if (c == null || c.type() == null) {
            throw new InvalidValueException("Loại mục sức khỏe là bắt buộc");
        }
        String text = blankToNull(c.freeText());
        if (text != null && text.length() > MAX_TEXT_LENGTH) {
            throw new InvalidValueException("Nội dung tối đa " + MAX_TEXT_LENGTH + " ký tự");
        }
        requireActive(sources.findById(c.sourceId()), "Nguồn thông tin không có hoặc đã ngừng hiệu lực");
        if (c.type() == HealthItemType.ALLERGY) {
            if (c.severityId() == null) {
                throw new InvalidValueException("Mức độ là bắt buộc với dị ứng (BR-M01-11)");
            }
            requireActive(severities.findById(c.severityId()), "Mức độ dị ứng không có hoặc đã ngừng hiệu lực");
            if ((c.allergenId() == null) == (text == null)) {
                throw new InvalidValueException("Dị ứng chọn một dị nguyên trong danh mục, hoặc nhập tay loại khác");
            }
            if (c.allergenId() != null) {
                requireActive(allergens.findById(c.allergenId()), "Dị nguyên không có hoặc đã ngừng hiệu lực");
            }
        } else {
            if (c.allergenId() != null) {
                throw new InvalidValueException("Dị nguyên chỉ dùng cho dị ứng");
            }
            if (text == null) {
                throw new InvalidValueException("Nội dung là bắt buộc");
            }
        }
        return c.type();
    }

    private static void requireActive(Optional<? extends CatalogBase> catalog, String message) {
        if (catalog.filter(CatalogBase::isActive).isEmpty()) {
            throw new InvalidValueException(message);
        }
    }

    /** Trùng theo dị nguyên, hoặc cùng loại và cùng nội dung (không phân biệt hoa thường, bỏ khoảng trắng thừa). */
    private Optional<HealthItem> findDuplicate(long residentId, HealthItemType type, Long allergenId, String text) {
        return items.findByResidentIdAndTypeAndStatus(residentId, type, HealthItemStatus.ACTIVE).stream()
                .filter(i -> allergenId != null
                        ? allergenId.equals(i.getAllergenId())
                        : i.getAllergenId() == null && match(text).equals(match(i.getFreeText())))
                .findFirst();
    }

    private static String match(String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static BusinessException duplicate(HealthItem existing) {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "DUPLICATE_HEALTH_ITEM",
                "Đã có mục đang hiệu lực trùng nội dung, gửi lại với confirmDuplicate=true nếu vẫn ghi",
                Map.of("existingItemId", existing.getId()));
    }

    private List<ItemView> views(Collection<HealthItem> found) {
        Map<Long, String> allergenNames = nameMap(allergens.findAllById(found.stream()
                .map(HealthItem::getAllergenId)
                .filter(Objects::nonNull)
                .toList()));
        Map<Long, String> severityNames = nameMap(severities.findAllById(found.stream()
                .map(HealthItem::getSeverityId)
                .filter(Objects::nonNull)
                .toList()));
        Map<Long, String> sourceNames = nameMap(
                sources.findAllById(found.stream().map(HealthItem::getSourceId).toList()));
        return found.stream()
                .map(i -> new ItemView(
                        i.getId(),
                        i.getType(),
                        i.getAllergenId() == null
                                ? null
                                : new CatalogRef(i.getAllergenId(), allergenNames.get(i.getAllergenId())),
                        i.getFreeText(),
                        i.isNotAutoChecked(),
                        i.isPendingCheck(),
                        i.getSeverityId() == null ? null : severityNames.get(i.getSeverityId()),
                        sourceNames.get(i.getSourceId()),
                        names.of(i.getRecordedBy()),
                        i.getRecordedAt(),
                        i.getStatus(),
                        i.isActive()
                                ? null
                                : new Exclusion(
                                        i.getExcludedAt(), names.of(i.getExcludedBy()), i.getExclusionReason())))
                .toList();
    }

    private static Map<Long, String> nameMap(List<? extends CatalogBase> catalogs) {
        return catalogs.stream().collect(Collectors.toMap(CatalogBase::getId, CatalogBase::getName));
    }

    private static Map<String, Object> snapshot(HealthItem i) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", i.getType().name());
        if (i.getAllergenId() != null) {
            values.put("allergenId", i.getAllergenId());
        }
        if (i.getFreeText() != null) {
            values.put("freeText", i.getFreeText());
        }
        if (i.getSeverityId() != null) {
            values.put("severityId", i.getSeverityId());
        }
        values.put("sourceId", i.getSourceId());
        values.put("status", i.getStatus().name());
        return values;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
