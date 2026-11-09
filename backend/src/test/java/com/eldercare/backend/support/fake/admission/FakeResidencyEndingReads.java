package com.eldercare.backend.support.fake.admission;

import com.eldercare.backend.admission.api.ResidencyEndingReadApi;
import com.eldercare.backend.admission.api.dto.ResidencyEndingView;
import com.eldercare.backend.admission.api.dto.SettlementInfoView;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bản giả {@link ResidencyEndingReadApi} (004 khai, 017 dùng; 017 T006): mặc định người cao tuổi không có hồ sơ kết thúc
 * Đang chuẩn bị, không có danh sách việc sau qua đời đang mở, không có ngoại lệ. Test đặt bằng các hàm dưới.
 */
public class FakeResidencyEndingReads implements ResidencyEndingReadApi {

    private final Map<Long, ResidencyEndingView> preparing = new HashMap<>();
    private final Map<Long, SettlementInfoView> settlement = new HashMap<>();
    private final Set<Long> exceptions = new HashSet<>();

    /** Hồ sơ kết thúc Đang chuẩn bị; cũng đặt thông tin quyết toán (danh sách việc sau qua đời thì chỉ đặt {@code info}). */
    public FakeResidencyEndingReads preparing(ResidencyEndingView ending, SettlementInfoView info) {
        preparing.put(ending.residentId(), ending);
        settlement.put(ending.residentId(), info);
        return this;
    }

    /** Có thông tin quyết toán nhưng không có hồ sơ kết thúc (danh sách việc sau qua đời đang mở). */
    public FakeResidencyEndingReads settlementInfo(long residentId, SettlementInfoView info) {
        settlement.put(residentId, info);
        return this;
    }

    public FakeResidencyEndingReads approvedException(long residentId) {
        exceptions.add(residentId);
        return this;
    }

    public FakeResidencyEndingReads reset() {
        preparing.clear();
        settlement.clear();
        exceptions.clear();
        return this;
    }

    @Override
    public Optional<ResidencyEndingView> preparing(long residentId) {
        return Optional.ofNullable(preparing.get(residentId));
    }

    @Override
    public Optional<SettlementInfoView> settlementInfo(long residentId) {
        return Optional.ofNullable(settlement.get(residentId));
    }

    @Override
    public boolean hasApprovedSettlementException(long residentId) {
        return exceptions.contains(residentId);
    }
}
