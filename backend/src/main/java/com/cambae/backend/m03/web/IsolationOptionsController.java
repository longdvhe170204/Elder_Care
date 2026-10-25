package com.cambae.backend.m03.web;

import com.cambae.backend.m03.internal.isolation.service.*;
import com.cambae.backend.m03.web.response.*;
import com.cambae.backend.m15.account.api.AccountApi;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Ba API đọc tối thiểu đã được chủ dự án duyệt riêng cho Phase9. */
@RestController
@RequestMapping("/api/v1")
public class IsolationOptionsController {
    private final RoomIsolationService rooms;
    private final ControlledTransferService transfers;
    private final AccountApi accounts;

    public IsolationOptionsController(
            RoomIsolationService rooms, ControlledTransferService transfers, AccountApi accounts) {
        this.rooms = rooms;
        this.transfers = transfers;
        this.accounts = accounts;
    }

    @GetMapping("/room-isolation/rooms")
    public List<RoomResponse> rooms() {
        return rooms.rooms().stream()
                .map(x -> RoomResponse.of(x.room(), x.blocked()))
                .toList();
    }

    @GetMapping("/bed-allocations/prescribing-doctors")
    public List<PrescribingDoctorResponse> doctors() {
        return transfers.doctors().stream()
                .map(x -> new PrescribingDoctorResponse(x.id(), x.fullName()))
                .toList();
    }

    @GetMapping("/room-isolation/pending-stays")
    public List<BedAllocationResponse> pending() {
        return transfers.pendingStays().stream()
                .map(x -> BedAllocationResponse.of(x, accounts))
                .toList();
    }
}
