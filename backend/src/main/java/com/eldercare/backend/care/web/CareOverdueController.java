package com.eldercare.backend.care.web;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.internal.overdue.service.*;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.care.web.response.CareTaskResponse;
import com.eldercare.backend.resident.api.ResidentApi;
import java.util.*;
import org.springframework.web.bind.annotation.*;

/** T046: endpoint đã duyệt; danh sách chỉ đọc, quyền và dữ liệu sức khỏe kiểm trên từng công việc. */
@RestController
@RequestMapping("/api/v1/care-tasks")
public class CareOverdueController {
    private final CareOverdueQueries queries;
    private final CareHandoverQueries handover;
    private final CurrentUser user;
    private final FieldVisibilityApi visibility;
    private final ResidentApi residents;
    private final AccountApi accounts;

    public CareOverdueController(
            CareOverdueQueries queries,
            CareHandoverQueries handover,
            CurrentUser user,
            FieldVisibilityApi visibility,
            ResidentApi residents,
            AccountApi accounts) {
        this.queries = queries;
        this.handover = handover;
        this.user = user;
        this.visibility = visibility;
        this.residents = residents;
        this.accounts = accounts;
    }

    @GetMapping("/overdue")
    public List<CareTaskResponse> list(@RequestParam(required = false) Long floorAreaId) {
        return queries.list(floorAreaId).stream()
                .map(t -> {
                    var card = t.residentId() == null
                            ? null
                            : new CareTaskResponse.ResidentCard(
                                    residents.identity(t.residentId()).fullName(), null, null, null);
                    var performer = t.performerId() == null
                            ? null
                            : accounts.byStaff(t.performerId())
                                    .flatMap(a -> accounts.fullName(a.accountId()))
                                    .orElse(Long.toString(t.performerId()));
                    return CareTaskResponse.of(
                            t,
                            card,
                            performer,
                            visibility.canView(user, InfoGroup.HEALTH, CareTaskService.target(t)) == Visibility.FULL,
                            Set.of(),
                            handover.reminderCount(t.id()));
                })
                .toList();
    }
}
