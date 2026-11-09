package com.eldercare.backend.billing.internal.item.service;

import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.item.domain.CostItemRecord;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** FR-018/R-15: lịch sử lệnh theo khoản, chỉ thêm; không quét nhật ký toàn viện khi mở chi tiết. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class CostItemHistory {
    private static final HistoryTrack<Void> COMMANDS = new HistoryTrack<>("M11.COST_ITEM", "COMMAND", Void.class);
    private final StatusHistoryApi history;
    private final CostAccess access;
    private final BusinessTime time;

    public CostItemHistory(StatusHistoryApi history, CostAccess access, BusinessTime time) {
        this.history = history;
        this.access = access;
        this.time = time;
    }

    public void changed(CostItemRecord item, String status, String operation, Reason reason) {
        history.add(
                COMMANDS,
                item.id(),
                new HistoryEntry<Void>(
                        HistoryKind.POINT,
                        item.status(),
                        status,
                        null,
                        time.now(),
                        operation,
                        access.user().accountId(),
                        false,
                        reason,
                        null,
                        null,
                        null,
                        null,
                        null));
    }

    public List<Map<String, Object>> read(long id) {
        return history.list(COMMANDS, id).stream()
                .map(row -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("operation", row.command());
                    value.put("actorId", row.actorId());
                    value.put("at", row.startsAt());
                    if (row.reason() != null) value.put("reason", row.reason());
                    return value;
                })
                .toList();
    }
}
