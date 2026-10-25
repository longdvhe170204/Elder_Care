package com.cambae.backend.support.fake.m01;

import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m01.api.dto.IdentityView;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bản giả {@link ResidentApi} (001 cài) cho 002, 003, 007…: test đặt người cao tuổi bằng {@link #resident}, đổi trạng
 * thái bằng {@link #changeStatus}. Trạng thái cuối là Kết thúc lưu trú, Qua đời, Hủy tiếp nhận (mục 5.5).
 */
public class FakeResidents implements ResidentApi {

    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private record Entry(
            IdentityView identity, Optional<Gender> gender, ResidentStatus status, Optional<Instant> finalStatusAt) {}

    private final Map<Long, Entry> residents = new HashMap<>();
    private final Map<Long, Long> previousProfiles = new HashMap<>();

    public FakeResidents resident(long residentId, String fullName, Gender gender, ResidentStatus status) {
        IdentityView identity = new IdentityView(residentId, "NCT-2026-" + residentId, fullName, Optional.empty());
        residents.put(residentId, new Entry(identity, Optional.ofNullable(gender), status, Optional.empty()));
        return this;
    }

    /** Đổi trạng thái; sang trạng thái cuối thì ghi thời điểm {@code at}. */
    public FakeResidents changeStatus(long residentId, ResidentStatus status, Instant at) {
        Entry e = entry(residentId);
        Optional<Instant> finalAt = FINAL_STATUSES.contains(status) ? Optional.of(at) : Optional.empty();
        residents.put(residentId, new Entry(e.identity(), e.gender(), status, finalAt));
        return this;
    }

    /** Đặt hồ sơ trước của {@code residentId} (liên kết lúc tạo hồ sơ cùng CCCD). */
    public FakeResidents previousProfile(long residentId, long previousProfileId) {
        previousProfiles.put(residentId, previousProfileId);
        return this;
    }

    private Entry entry(long residentId) {
        Entry e = residents.get(residentId);
        if (e == null) {
            throw new IllegalArgumentException("Chưa đặt người cao tuổi " + residentId + " trong FakeResidents");
        }
        return e;
    }

    @Override
    public ResidentStatus status(long residentId) {
        return entry(residentId).status();
    }

    @Override
    public IdentityView identity(long residentId) {
        return entry(residentId).identity();
    }

    @Override
    public Optional<Instant> finalStatusAt(long residentId) {
        return entry(residentId).finalStatusAt();
    }

    @Override
    public boolean isFinalStatus(long residentId) {
        return FINAL_STATUSES.contains(entry(residentId).status());
    }

    @Override
    public Optional<Gender> gender(long residentId) {
        return entry(residentId).gender();
    }

    @Override
    public Optional<Long> previousProfileId(long residentId) {
        entry(residentId);
        return Optional.ofNullable(previousProfiles.get(residentId));
    }

    @Override
    public List<Long> byStatus(Set<ResidentStatus> statuses) {
        return residents.entrySet().stream()
                .filter(e -> statuses.contains(e.getValue().status()))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }
}
