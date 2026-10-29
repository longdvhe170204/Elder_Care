package com.eldercare.backend.support.fake.resident;

import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.dto.ResidenceTypePeriod;
import com.eldercare.backend.resident.api.spi.ContractApi;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Bản giả {@link ContractApi} (004 cài): loại lưu trú theo khoảng ngày; ngày {@code to} không thuộc khoảng. */
public class FakeContracts implements ContractApi {

    private final Map<Long, List<ResidenceTypePeriod>> periods = new HashMap<>();

    public FakeContracts period(long residentId, ResidenceTypePeriod period) {
        periods.computeIfAbsent(residentId, id -> new ArrayList<>()).add(period);
        return this;
    }

    @Override
    public Optional<ResidenceType> currentResidenceType(long residentId, LocalDate date) {
        return residenceTypeHistory(residentId).stream()
                .filter(p ->
                        !date.isBefore(p.from()) && p.to().map(date::isBefore).orElse(true))
                .map(ResidenceTypePeriod::type)
                .findFirst();
    }

    @Override
    public List<ResidenceTypePeriod> residenceTypeHistory(long residentId) {
        return List.copyOf(periods.getOrDefault(residentId, List.of()));
    }
}
