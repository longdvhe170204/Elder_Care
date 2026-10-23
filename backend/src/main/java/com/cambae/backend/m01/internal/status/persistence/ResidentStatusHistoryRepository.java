package com.cambae.backend.m01.internal.status.persistence;

import com.cambae.backend.common.data.AppendOnlyRepository;
import com.cambae.backend.m01.internal.status.domain.ResidentStatusHistory;
import java.util.List;

public interface ResidentStatusHistoryRepository extends AppendOnlyRepository<ResidentStatusHistory, Long> {

    List<ResidentStatusHistory> findByResidentIdOrderByIdAsc(long residentId);
}
