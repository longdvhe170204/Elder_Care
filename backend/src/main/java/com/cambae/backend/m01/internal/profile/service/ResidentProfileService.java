package com.cambae.backend.m01.internal.profile.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.m01.api.events.PersonalInfoUpdated;
import com.cambae.backend.m01.api.events.ResidentCreated;
import com.cambae.backend.m01.internal.profile.domain.Contact;
import com.cambae.backend.m01.internal.profile.domain.IdDocumentType;
import com.cambae.backend.m01.internal.profile.domain.PersonalInfo;
import com.cambae.backend.m01.internal.profile.domain.Resident;
import com.cambae.backend.m01.internal.profile.persistence.ResidentLookup;
import com.cambae.backend.m01.internal.profile.persistence.ResidentLookup.Match;
import com.cambae.backend.m01.internal.profile.persistence.ResidentRepository;
import com.cambae.backend.m01.internal.status.domain.ResidentStatusHistory;
import com.cambae.backend.m01.internal.status.persistence.ResidentStatusHistoryRepository;
import com.cambae.backend.m01.internal.status.service.ProfileOpenGuard;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hồ sơ người cao tuổi (001 US1; FR-001 → FR-007): tạo hồ sơ, cập nhật thông tin cá nhân, chuỗi hồ sơ trước, xem và
 * tra cứu trong phạm vi. Hồ sơ mới ở Đang tiếp nhận kèm bản ghi lịch sử đầu; CCCD duy nhất trong hồ sơ chưa ở trạng
 * thái cuối (DBR-01); chỉ liên kết hồ sơ cũ khi trùng CCCD (FR-004, Q-319); không có CCCD mà trùng họ tên và ngày
 * sinh thì hỏi xác nhận (FR-005). Mỗi lệnh ghi đúng một bản ghi nhật ký (DBR-23).
 */
@Service
public class ResidentProfileService {

    static final AuditAction CREATED = new AuditAction("RESIDENT_CREATED", false);
    static final AuditAction UPDATED = new AuditAction("RESIDENT_UPDATED", true);
    private static final String ENTITY_TYPE = ResidentScopeAssignment.ENTITY_TYPE;
    private static final int MAX_NAME_LENGTH = 200;
    private static final PermissionKey VIEW_KEY = new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW);

    /** Hồ sơ cho client: thông tin cá nhân cùng trạng thái hiện tại và mức chăm sóc hiện hành. */
    public record ProfileView(
            long id,
            String profileCode,
            PersonalInfo personalInfo,
            ResidentStatus status,
            Instant statusSince,
            String currentCareLevel,
            Long previousProfileId,
            int version) {}

    public record CreateResult(ProfileView profile, List<Warning> warnings) {}

    /** Một hồ sơ trước trong chuỗi; chỉ có mã khi người xem không có quyền trên chính hồ sơ đó ({@code status} rỗng). */
    public record LinkedProfile(long id, String profileCode, ResidentStatus status, boolean viewable) {}

    public record SearchPage(List<ProfileView> items, String nextCursor) {}

    /** Chủ sở hữu của ảnh người cao tuổi là hồ sơ (id hồ sơ); quyền tải ở {@code ResidentPhotoFilePermission}. */
    static final String PHOTO_OWNER = "M01.RESIDENT_PHOTO";

    private final ResidentRepository residents;
    private final ResidentStatusHistoryRepository history;
    private final ResidentLookup lookup;
    private final ProfileOpenGuard guard;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final JsonMapper json;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<PermissionApi> permissions;
    private final ObjectProvider<ScopeApi> scope;
    private final FileApi files;

    ResidentProfileService(
            ResidentRepository residents,
            ResidentStatusHistoryRepository history,
            ResidentLookup lookup,
            ProfileOpenGuard guard,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            JsonMapper json,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<PermissionApi> permissions,
            ObjectProvider<ScopeApi> scope,
            FileApi files) {
        this.residents = residents;
        this.history = history;
        this.lookup = lookup;
        this.guard = guard;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.json = json;
        this.currentUser = currentUser;
        this.permissions = permissions;
        this.scope = scope;
        this.files = files;
    }

    /** Đối tượng kiểm quyền và phạm vi của một hồ sơ (kiểm quyền ở {@code RequiresPermissionAspect}). */
    public static Target target(long residentId) {
        return new Target(ENTITY_TYPE, String.valueOf(residentId), residentId, null);
    }

    @RequiresPermission(function = Function.RESIDENT_PROFILE, operation = Operation.CREATE)
    @Transactional
    public CreateResult create(PersonalInfo input, boolean confirmNotDuplicate) {
        PersonalInfo info = normalized(input);
        List<Warning> warnings = new ArrayList<>();
        Long previousProfileId = null;
        boolean confirmed = false;
        if (info.idDocumentType() == IdDocumentType.CITIZEN_ID) {
            String citizenId = info.idDocumentNumber();
            lookup.findOpenByCitizenId(citizenId).ifPresent(open -> {
                throw citizenIdInUse(open.profileCode());
            });
            Optional<Match> closed = lookup.findLatestClosedByCitizenId(citizenId);
            if (closed.isPresent() && closed.get().status() == ResidentStatus.DECEASED) {
                warnings.add(new Warning(
                        "CITIZEN_ID_MATCHES_DECEASED_PROFILE",
                        "CCCD trùng hồ sơ đã qua đời, cần kiểm tra lại giấy tờ"));
            } else if (closed.isPresent()) {
                previousProfileId = closed.get().id();
            }
        } else {
            List<Match> same = lookup.findSameNameAndBirthDate(info.fullName(), info.dateOfBirth());
            if (!same.isEmpty() && !confirmNotDuplicate) {
                throw possibleDuplicate(same);
            }
            confirmed = !same.isEmpty();
        }
        CurrentUser user = currentUser.getObject();
        Instant now = time.now();
        Resident resident = new Resident(info, contactJson(info.contact()), previousProfileId, now, user.accountId());
        try {
            residents.saveAndFlush(resident);
        } catch (DataIntegrityViolationException e) {
            // UNIQUE (open_citizen_id): hai lệnh tạo cùng CCCD chạy cùng lúc
            throw citizenIdInUse(lookup.findOpenByCitizenId(info.idDocumentNumber())
                    .map(Match::profileCode)
                    .orElse(null));
        }
        resident.assignProfileCode(time.today().getYear());
        residents.saveAndFlush(resident);
        attachPhoto(info.photoFileId(), resident.getId());
        history.append(ResidentStatusHistory.profileCreated(resident.getId(), now, user.accountId()));
        AuditLogEntry.Builder entry = AuditLogEntry.builder()
                .action(CREATED)
                .entity(ENTITY_TYPE, resident.getId())
                .resident(resident.getId())
                .after(snapshot(info));
        if (confirmed) {
            entry.details(Map.of("confirmedNotDuplicate", true));
        }
        auditLog.write(entry.build());
        events.publishEvent(new ResidentCreated(resident.getId(), Optional.ofNullable(previousProfileId)));
        return new CreateResult(view(resident), warnings);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.RESIDENT_PROFILE, operation = Operation.UPDATE)
    @Transactional
    public ProfileView updatePersonalInfo(Target target, String ifMatch, PersonalInfo input, Reason reason) {
        long id = target.residentId();
        Resident resident = residents.lockById(id).orElseThrow(NotFoundException::new);
        guard.requireOpen(id);
        VersionEtag.requireMatch(ifMatch, resident.getVersion(), view(resident));
        PersonalInfo after = normalized(input);
        PersonalInfo before = personalInfo(resident);
        Set<String> changed = changedFields(before, after);
        if (changed.isEmpty()) {
            return view(resident);
        }
        if (changed.contains("idDocumentType") || changed.contains("idDocumentNumber")) {
            if (after.idDocumentType() == IdDocumentType.CITIZEN_ID) {
                lookup.findOpenByCitizenId(after.idDocumentNumber())
                        .filter(open -> open.id() != id)
                        .ifPresent(open -> {
                            throw citizenIdInUse(open.profileCode());
                        });
            }
        }
        if (!Objects.equals(before.photoFileId(), after.photoFileId())) {
            attachPhoto(after.photoFileId(), id);
        }
        resident.changePersonalInfo(after, contactJson(after.contact()));
        try {
            residents.saveAndFlush(resident);
        } catch (DataIntegrityViolationException e) {
            throw citizenIdInUse(lookup.findOpenByCitizenId(after.idDocumentNumber())
                    .map(Match::profileCode)
                    .orElse(null));
        }
        Map<String, Object> beforeValues = snapshot(before);
        Map<String, Object> afterValues = snapshot(after);
        auditLog.write(AuditLogEntry.builder()
                .action(UPDATED)
                .entity(ENTITY_TYPE, id)
                .resident(id)
                .before(only(beforeValues, changed))
                .after(only(afterValues, changed))
                .reason(reason)
                .build());
        events.publishEvent(new PersonalInfoUpdated(id, changed));
        return view(resident);
    }

    /**
     * Gắn ảnh vào hồ sơ trong transaction của lệnh (research H-13): tệp không có thật thì 404, đã gắn nơi khác thì 409
     * và cả lệnh rollback. Ảnh cũ khi thay vẫn gắn với hồ sơ làm lịch sử, không bị job dọn tệp xóa.
     */
    private void attachPhoto(Long photoFileId, long residentId) {
        if (photoFileId != null) {
            files.attach(photoFileId, PHOTO_OWNER, residentId);
        }
    }

    @RequiresPermission(function = Function.RESIDENT_PROFILE, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public ProfileView view(Target target) {
        return view(residents.findById(target.residentId()).orElseThrow(NotFoundException::new));
    }

    /** Chuỗi hồ sơ trước, gần nhất trước; mỗi hồ sơ kiểm quyền xem riêng (FR-004). */
    @RequiresPermission(function = Function.RESIDENT_PROFILE, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<LinkedProfile> linkedProfiles(Target target) {
        CurrentUser user = currentUser.getObject();
        List<LinkedProfile> chain = new ArrayList<>();
        Long previous = residents
                .findById(target.residentId())
                .orElseThrow(NotFoundException::new)
                .getPreviousProfileId();
        while (previous != null) {
            Resident older = residents.findById(previous).orElseThrow(NotFoundException::new);
            boolean viewable = permissions
                    .getObject()
                    .hasPermission(user, VIEW_KEY, target(older.getId()))
                    .allowed();
            chain.add(new LinkedProfile(
                    older.getId(), older.getProfileCode(), viewable ? older.getStatus() : null, viewable));
            previous = older.getPreviousProfileId();
        }
        return chain;
    }

    /**
     * Tra cứu trong phạm vi của người xem (FR-007, FR-035): tìm theo họ tên, mã hồ sơ hoặc số giấy tờ, lọc trạng thái,
     * mới nhất trước, phân trang theo con trỏ. Không dùng {@code @RequiresPermission}: quyền theo phân công không đạt
     * với đối tượng chưa xác định, nên kiểm vai trò có khóa Xem rồi lọc bằng bộ lọc phạm vi. Phạm vi theo tầng hoặc
     * phòng cần phân bổ giường của 003, nên hiện lọc theo tập hồ sơ mà 002 trả về.
     */
    @Transactional(readOnly = true)
    public SearchPage search(String q, Set<ResidentStatus> statuses, String cursor, int limit) {
        CurrentUser user = currentUser.getObject();
        if (!permissions.getObject().permissions(user).contains(VIEW_KEY)) {
            throw new PermissionDeniedException("PERMISSION_DENIED", "Không có quyền", Map.of("failedLayer", "ROLE"));
        }
        ScopeFilter filter = scope.getObject().filter(user, VIEW_KEY);
        Set<Long> scopedIds = filter.facilityWide() ? null : filter.residentIds();
        int size = Math.min(Math.max(limit, 1), ResidentLookup.MAX_PAGE_SIZE);
        List<Long> ids = lookup.search(q, statuses, scopedIds, parseCursor(cursor), size);
        boolean more = ids.size() > size;
        List<Long> page = more ? ids.subList(0, size) : ids;
        Map<Long, Resident> byId = new LinkedHashMap<>();
        residents.findAllById(page).forEach(r -> byId.put(r.getId(), r));
        List<ProfileView> items = page.stream().map(byId::get).map(this::view).toList();
        return new SearchPage(items, more ? String.valueOf(page.getLast()) : null);
    }

    private static Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cursor.strip());
        } catch (NumberFormatException e) {
            throw new InvalidValueException("Con trỏ trang không hợp lệ");
        }
    }

    private ProfileView view(Resident r) {
        return new ProfileView(
                r.getId(),
                r.getProfileCode(),
                personalInfo(r),
                r.getStatus(),
                r.getStatusSince(),
                r.getCurrentCareLevel(),
                r.getPreviousProfileId(),
                r.getVersion());
    }

    private PersonalInfo personalInfo(Resident r) {
        return new PersonalInfo(
                r.getFullName(),
                r.getDateOfBirth(),
                r.getGender(),
                r.getIdDocumentType(),
                r.getIdDocumentNumber(),
                r.getPhotoFileId(),
                r.getPermanentAddress(),
                r.getContactAddress(),
                r.getContact() == null ? null : json.readValue(r.getContact(), Contact.class),
                r.getSpecialNotes());
    }

    private String contactJson(Contact contact) {
        return contact == null ? null : json.writeValueAsString(contact);
    }

    /** Kiểm trường bắt buộc và đồng bộ loại với số giấy tờ; chuỗi rỗng coi như không có (FR-001). */
    private PersonalInfo normalized(PersonalInfo in) {
        if (in == null || blankToNull(in.fullName()) == null) {
            throw new InvalidValueException("Họ tên là bắt buộc");
        }
        String fullName = in.fullName().strip();
        if (fullName.length() > MAX_NAME_LENGTH) {
            throw new InvalidValueException("Họ tên tối đa " + MAX_NAME_LENGTH + " ký tự");
        }
        if (in.dateOfBirth() == null || in.dateOfBirth().isAfter(time.today())) {
            throw new InvalidValueException("Ngày sinh là bắt buộc và không ở tương lai");
        }
        if (in.gender() == null) {
            throw new InvalidValueException("Giới tính là bắt buộc");
        }
        String number = blankToNull(in.idDocumentNumber());
        if ((in.idDocumentType() == null) != (number == null)) {
            throw new InvalidValueException("Loại và số giấy tờ định danh phải đi cùng nhau");
        }
        Contact contact = in.contact() == null
                ? null
                : new Contact(
                        blankToNull(in.contact().phoneNumber()),
                        blankToNull(in.contact().email()));
        if (contact != null && contact.phoneNumber() == null && contact.email() == null) {
            contact = null;
        }
        return new PersonalInfo(
                fullName,
                in.dateOfBirth(),
                in.gender(),
                in.idDocumentType(),
                number,
                in.photoFileId(),
                blankToNull(in.permanentAddress()),
                blankToNull(in.contactAddress()),
                contact,
                blankToNull(in.specialNotes()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static Set<String> changedFields(PersonalInfo before, PersonalInfo after) {
        Map<String, Object> was = snapshot(before);
        Map<String, Object> now = snapshot(after);
        Set<String> changed = new LinkedHashSet<>(was.keySet());
        changed.addAll(now.keySet());
        changed.removeIf(field -> Objects.equals(was.get(field), now.get(field)));
        return changed;
    }

    private static Map<String, Object> only(Map<String, Object> values, Set<String> fields) {
        Map<String, Object> picked = new LinkedHashMap<>();
        fields.forEach(field -> picked.put(field, values.get(field)));
        return picked;
    }

    /** Giá trị các trường có dữ liệu, dạng ghi nhật ký. */
    private static Map<String, Object> snapshot(PersonalInfo info) {
        Map<String, Object> values = new LinkedHashMap<>();
        putIfPresent(values, "fullName", info.fullName());
        putIfPresent(
                values,
                "dateOfBirth",
                info.dateOfBirth() == null ? null : info.dateOfBirth().toString());
        putIfPresent(
                values, "gender", info.gender() == null ? null : info.gender().name());
        putIfPresent(
                values,
                "idDocumentType",
                info.idDocumentType() == null ? null : info.idDocumentType().name());
        putIfPresent(values, "idDocumentNumber", info.idDocumentNumber());
        putIfPresent(values, "photoFileId", info.photoFileId());
        putIfPresent(values, "permanentAddress", info.permanentAddress());
        putIfPresent(values, "contactAddress", info.contactAddress());
        if (info.contact() != null) {
            Map<String, Object> contact = new LinkedHashMap<>();
            putIfPresent(contact, "phoneNumber", info.contact().phoneNumber());
            putIfPresent(contact, "email", info.contact().email());
            values.put("contact", contact);
        }
        putIfPresent(values, "specialNotes", info.specialNotes());
        return values;
    }

    private static void putIfPresent(Map<String, Object> values, String field, Object value) {
        if (value != null) {
            values.put(field, value);
        }
    }

    private static BusinessException citizenIdInUse(String profileCode) {
        Map<String, Object> properties = profileCode == null ? Map.of() : Map.of("profileCode", profileCode);
        return new BusinessException(
                HttpStatus.CONFLICT, "CITIZEN_ID_IN_USE", "CCCD đang được dùng ở một hồ sơ chưa kết thúc", properties);
    }

    private static BusinessException possibleDuplicate(List<Match> same) {
        List<Map<String, Object>> candidates = same.stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.id(),
                        "profileCode", m.profileCode(),
                        "fullName", m.fullName(),
                        "dateOfBirth", m.dateOfBirth().toString(),
                        "status", m.status().name()))
                .toList();
        return new BusinessException(
                HttpStatus.CONFLICT,
                "POSSIBLE_DUPLICATE_PROFILE",
                "Đã có hồ sơ cùng họ tên và ngày sinh, cần xác nhận không trùng",
                Map.of("candidates", candidates));
    }
}
