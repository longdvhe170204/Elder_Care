package com.eldercare.backend.admission.internal.registration.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.admission.internal.registration.domain.ResidentRegistrationFlag;
import com.eldercare.backend.common.data.TechnicalLocks;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khóa theo người và cờ thường trú cùng xã/phường của hồ sơ (data-model 018 mục 0.1, DBR-33; R-15): mọi lệnh khai báo khóa
 * {@code technical_lock} của người cao tuổi trước; cờ (FR-001, Q-215) ở {@code simple_record} loại {@code
 * M02.REGISTRATION_RESIDENT}, tạo khi xác nhận lần đầu. Không dùng {@code resident_module_state} vì dòng M02 của bảng đó là
 * trạng thái lưu trú của 004.
 */
@Repository
public class RegistrationResidentStore {

    private static final String SOURCE = "M01.RESIDENT";

    private final TechnicalLocks locks;
    private final SimpleRecordApi records;
    private final RegistrationLockedReads lockedReads;

    RegistrationResidentStore(TechnicalLocks locks, SimpleRecordApi records, RegistrationLockedReads lockedReads) {
        this.locks = locks;
        this.records = records;
        this.lockedReads = lockedReads;
    }

    private static String key(long residentId) {
        return "M02.REG_RESIDENT:" + residentId;
    }

    /** Khóa người cao tuổi tới hết transaction; trả cờ thường trú cùng xã/phường. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lock(long residentId) {
        locks.lock(key(residentId));
        return flag(residentId).map(r -> r.content().localPermanentResident()).orElse(false);
    }

    /** Tài khoản đã ghi xác nhận thường trú cùng xã/phường đang có (người ghi gốc khi đính chính, FR-026 a). */
    public Optional<Long> confirmedBy(long residentId) {
        return records.findByUniqueKey(ResidentRegistrationFlag.KIND, key(residentId))
                .filter(r -> r.content().localPermanentResident())
                .map(r -> r.content().confirmedById());
    }

    /** Gỡ xác nhận thường trú cùng xã/phường bằng đính chính (FR-001; đã khóa); căn cứ cũ còn ở nhật ký, chuỗi đính chính. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeLocalPermanentResident(long residentId) {
        flag(residentId).ifPresent(row -> save(row, new ResidentRegistrationFlag(false, null, null, null)));
    }

    /** Ghi xác nhận thường trú cùng xã/phường (đã khóa). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void confirmLocalPermanentResident(long residentId, String basisNote, long confirmedById, Instant at) {
        ResidentRegistrationFlag flag = new ResidentRegistrationFlag(true, basisNote, confirmedById, at);
        Optional<SimpleRecord<ResidentRegistrationFlag>> row = flag(residentId);
        if (row.isPresent()) {
            save(row.get(), flag);
        } else {
            records.create(
                    ResidentRegistrationFlag.KIND,
                    new NewSimpleRecord<>(
                            residentId,
                            new EntityRef(SOURCE, residentId),
                            "ACTIVE",
                            flag,
                            null,
                            null,
                            key(residentId),
                            confirmedById,
                            false));
        }
    }

    /** Cờ đọc khóa (bản mới nhất đã commit), gọi sau khi đã khóa người cao tuổi. */
    private Optional<SimpleRecord<ResidentRegistrationFlag>> flag(long residentId) {
        return lockedReads.ids(ResidentRegistrationFlag.KIND.code(), "unique_key = ?", key(residentId)).stream()
                .findFirst()
                .map(id -> records.lockForUpdate(ResidentRegistrationFlag.KIND, id));
    }

    private void save(SimpleRecord<ResidentRegistrationFlag> row, ResidentRegistrationFlag flag) {
        records.change(
                ResidentRegistrationFlag.KIND,
                row.id(),
                row.version(),
                new RecordChange<>("ACTIVE", flag, row.effectiveTo(), row.uniqueKey(), false));
    }
}
