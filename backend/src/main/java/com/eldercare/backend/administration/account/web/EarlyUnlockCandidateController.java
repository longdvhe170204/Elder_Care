package com.eldercare.backend.administration.account.web;

import com.eldercare.backend.administration.account.internal.auth.service.AccountService;
import com.eldercare.backend.administration.account.web.response.EarlyUnlockCandidatePage;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tài khoản nhân viên Khóa tạm người dùng được mở khóa sớm (002 FR-019c; đổi api-002 2026-10-27): màn mở khóa sớm của
 * app nhân viên lấy id và phiên bản ở đây rồi gọi {@code POST /accounts/{id}/early-unlock}.
 */
@RestController
@RequestMapping("/api/v1/early-unlock-candidates")
class EarlyUnlockCandidateController {

    private final AccountService accounts;

    EarlyUnlockCandidateController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    @Tag(name = "accounts")
    EarlyUnlockCandidatePage list() {
        return new EarlyUnlockCandidatePage(accounts.earlyUnlockCandidates().stream()
                .map(c -> new EarlyUnlockCandidatePage.Item(c.accountId(), c.fullName(), c.lockedUntil(), c.version()))
                .toList());
    }
}
