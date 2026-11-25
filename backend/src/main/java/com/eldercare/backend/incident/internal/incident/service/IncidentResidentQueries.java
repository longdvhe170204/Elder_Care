package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.incident.internal.incident.domain.IncidentResident;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tìm người cao tuổi khi gửi yêu cầu hỗ trợ hoặc ghi sự cố (đề xuất 2026-11-23): tìm trong toàn viện vì lúc khẩn người báo
 * có thể không phụ trách người đó; bù lại chỉ trả họ tên, mã, trạng thái, phòng, tầng. Gõ ít nhất 2 ký tự, không phân biệt
 * hoa thường và dấu; tối đa 5 người; bỏ người đã ở trạng thái cuối.
 */
@Service
public class IncidentResidentQueries {
    static final int MIN_QUERY = 2;
    static final int MAX_RESULTS = 5;
    private static final Set<PermissionKey> USERS = Set.of(
            new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.CREATE),
            new PermissionKey(Function.EMERGENCY_ASSIST_BUTTON, Operation.CREATE));
    private static final Set<ResidentStatus> ACTIVE = Set.of(
            ResidentStatus.ADMISSION_IN_PROGRESS,
            ResidentStatus.IN_RESIDENCE,
            ResidentStatus.TEMPORARILY_ABSENT,
            ResidentStatus.OFF_SITE_ACTIVITY,
            ResidentStatus.HOSPITALIZED);

    private final PermissionApi permissions;
    private final Optional<ResidentApi> residents;
    private final Optional<ResidentLocationApi> locations;

    public IncidentResidentQueries(
            PermissionApi permissions,
            @Autowired(required = false) ResidentApi residents,
            @Autowired(required = false) ResidentLocationApi locations) {
        this.permissions = permissions;
        this.residents = Optional.ofNullable(residents);
        this.locations = Optional.ofNullable(locations);
    }

    @Transactional(readOnly = true)
    public List<IncidentResident> search(String query, CurrentUser user) {
        if (permissions.permissions(user).stream().noneMatch(USERS::contains)) {
            throw new PermissionDeniedException("Không có quyền ghi nhận sự cố hoặc gửi yêu cầu hỗ trợ");
        }
        String needle = normalize(query == null ? "" : query.trim());
        if (needle.length() < MIN_QUERY) throw new InvalidValueException("Gõ ít nhất " + MIN_QUERY + " ký tự để tìm");
        if (residents.isEmpty()) return List.of();
        // "P101", "phòng 101": bỏ tiền tố để khớp số phòng
        String stripped = needle.replaceFirst("^(phong|p)\s*", "");
        final String roomNeedle = stripped.equals(needle) ? "" : stripped;
        var api = residents.get();
        var active = api.byStatus(ACTIVE).stream().map(api::identity).toList();
        // Phòng hiện tại của mọi người đang ở viện, để gõ số phòng ("P101") cũng tìm ra các cụ trong phòng đó (BF-06)
        var where = locations
                .map(port -> port.current(
                        active.stream().map(identity -> identity.residentId()).toList()))
                .orElse(Map.of());
        var matched = active.stream()
                .filter(identity -> {
                    var place = where.get(identity.residentId());
                    String room = place == null || place.roomName() == null ? "" : place.roomName();
                    return normalize(identity.fullName() + " " + identity.profileCode() + " " + room)
                                    .contains(needle)
                            || (!roomNeedle.isEmpty() && normalize(room).contains(roomNeedle));
                })
                .sorted(Comparator.comparing(identity -> identity.fullName()))
                .limit(MAX_RESULTS)
                .toList();
        return matched.stream()
                .map(identity -> {
                    var location = where.get(identity.residentId());
                    return new IncidentResident(
                            identity.residentId(),
                            identity.fullName(),
                            identity.profileCode(),
                            api.status(identity.residentId()).name(),
                            location == null ? null : location.roomName(),
                            location == null ? null : location.floorId(),
                            location == null ? null : location.floorName());
                })
                .toList();
    }

    /** Chữ thường, bỏ dấu tiếng Việt (cả đ), để "an" khớp "An", "Ấn". */
    static String normalize(String text) {
        String lower = text.toLowerCase(Locale.ROOT).replace('đ', 'd');
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
