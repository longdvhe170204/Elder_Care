package com.eldercare.backend.support.fake.admission;

import com.eldercare.backend.admission.api.dto.DepositStatus;
import com.eldercare.backend.admission.api.dto.DepositStatusView;
import com.eldercare.backend.admission.api.spi.DepositStatusPort;
import java.util.HashMap;
import java.util.Map;

/** Bản giả {@link DepositStatusPort} (017 cài): mặc định {@code NOT_REQUIRED}. */
public class FakeDepositStatuses implements DepositStatusPort {

    private final Map<Long, DepositStatusView> statuses = new HashMap<>();

    public FakeDepositStatuses status(long contractId, DepositStatus status) {
        statuses.put(contractId, new DepositStatusView(status, null, null, null));
        return this;
    }

    public FakeDepositStatuses status(long contractId, DepositStatusView view) {
        statuses.put(contractId, view);
        return this;
    }

    public FakeDepositStatuses reset() {
        statuses.clear();
        return this;
    }

    @Override
    public DepositStatusView status(long contractId) {
        return statuses.getOrDefault(contractId, new DepositStatusView(DepositStatus.NOT_REQUIRED, null, null, null));
    }
}
