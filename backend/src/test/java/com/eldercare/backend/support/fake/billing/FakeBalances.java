package com.eldercare.backend.support.fake.billing;

import com.eldercare.backend.admission.api.dto.ConditionStatus;
import com.eldercare.backend.billing.api.BalanceApi;
import com.eldercare.backend.billing.api.dto.BalanceDocumentView;
import com.eldercare.backend.billing.api.dto.BalanceView;
import com.eldercare.backend.billing.api.dto.SettlementConditionCode;
import com.eldercare.backend.billing.api.dto.SettlementConditionView;
import com.eldercare.backend.billing.api.dto.SettlementConditionsView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bản giả {@link BalanceApi} (017, A giữ) cho 012, 016: chưa đặt thì chưa có sổ, hai mã điều kiện quyết toán Chưa đạt,
 * không có chứng từ. Test dùng {@code @Import(FakeBalanceConfig.class)}.
 */
public class FakeBalances implements BalanceApi {

    private final Map<Long, BalanceView> balances = new HashMap<>();
    private final Map<Long, SettlementConditionsView> conditions = new HashMap<>();
    private final Map<Long, List<BalanceDocumentView>> documents = new HashMap<>();

    public FakeBalances balance(BalanceView view) {
        balances.put(view.residentId(), view);
        return this;
    }

    public FakeBalances settlementConditions(SettlementConditionsView view) {
        conditions.put(view.residentId(), view);
        return this;
    }

    public FakeBalances documents(long residentId, List<BalanceDocumentView> items) {
        documents.put(residentId, List.copyOf(items));
        return this;
    }

    public void reset() {
        balances.clear();
        conditions.clear();
        documents.clear();
    }

    @Override
    public Optional<BalanceView> balance(long residentId) {
        return Optional.ofNullable(balances.get(residentId));
    }

    @Override
    public SettlementConditionsView settlementConditions(long residentId) {
        return conditions.getOrDefault(
                residentId,
                new SettlementConditionsView(
                        residentId,
                        List.of(
                                new SettlementConditionView(
                                        SettlementConditionCode.END_H_SETTLEMENT,
                                        ConditionStatus.NOT_MET,
                                        true,
                                        List.of()),
                                new SettlementConditionView(
                                        SettlementConditionCode.END_H_SETTLEMENT_CONFIRMATION,
                                        ConditionStatus.NOT_MET,
                                        false,
                                        List.of())),
                        null));
    }

    @Override
    public List<BalanceDocumentView> documents(long residentId) {
        return documents.getOrDefault(residentId, List.of());
    }
}
