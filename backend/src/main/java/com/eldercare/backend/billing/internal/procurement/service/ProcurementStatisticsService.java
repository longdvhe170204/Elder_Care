package com.eldercare.backend.billing.internal.procurement.service;

import com.eldercare.backend.billing.api.ProcurementStatisticsApi;
import com.eldercare.backend.billing.api.dto.ProcurementRequestView;
import com.eldercare.backend.billing.api.dto.ProcurementTotalView;
import com.eldercare.backend.billing.internal.procurement.persistence.ProcurementRequestStore;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T023, contracts mục 2.3: chỉ đọc, không khóa. Luồng lấy từ danh sách đã đọc xong nên kết nối trả lại trước khi
 * hàm trả về (như AssetStatisticsService); số yêu cầu mua sắm trong khoảng tối đa CFG-M14-01 nhỏ.
 */
@Service
@Transactional(readOnly = true)
public class ProcurementStatisticsService implements ProcurementStatisticsApi {
    private final ProcurementRequestStore store;

    public ProcurementStatisticsService(ProcurementRequestStore store) {
        this.store = store;
    }

    @Override
    public Stream<ProcurementRequestView> byDepartment(Set<Long> departmentIds, LocalDate from, LocalDate to) {
        return store.statistics(departmentIds, from, to).stream();
    }

    @Override
    public List<ProcurementTotalView> totals(Set<Long> departmentIds, LocalDate from, LocalDate to) {
        return store.totals(departmentIds, from, to);
    }
}
