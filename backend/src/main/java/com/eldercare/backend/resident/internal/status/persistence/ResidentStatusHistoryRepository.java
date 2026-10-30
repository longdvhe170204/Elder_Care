package com.eldercare.backend.resident.internal.status.persistence;

import com.eldercare.backend.common.data.AppendOnlyRepository;
import com.eldercare.backend.resident.internal.status.domain.ResidentStatusHistory;
import java.util.List;

public interface ResidentStatusHistoryRepository extends AppendOnlyRepository<ResidentStatusHistory, Long> {

    List<ResidentStatusHistory> findByResidentIdOrderByIdAsc(long residentId);
}
