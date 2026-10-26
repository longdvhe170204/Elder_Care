package com.cambae.backend.m03.web;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.api.dto.LocationRef;
import com.cambae.backend.m03.internal.history.domain.HistoryStay;
import com.cambae.backend.m03.internal.history.service.*;
import com.cambae.backend.m03.web.request.RecordCorrection;
import com.cambae.backend.m03.web.response.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Phase 7: chuyển DTO web theo contract, không đổi chữ ký LocationHistoryApi đã công bố. */
@RestController
@RequestMapping("/api/v1")
public class LocationHistoryController {
    private final LocationHistoryQueries history;
    private final HistoryCorrections corrections;
    private final BusinessTime time;

    public LocationHistoryController(
            LocationHistoryQueries history, HistoryCorrections corrections, BusinessTime time) {
        this.history = history;
        this.corrections = corrections;
        this.time = time;
    }

    @GetMapping("/location-history/residents/{residentId}")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Lịch sử vị trí",
                content =
                        @Content(
                                mediaType = "application/json",
                                array =
                                        @ArraySchema(
                                                schema = @Schema(implementation = ResidentLocationHistoryRow.class)))),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public List<ResidentLocationHistoryRow> resident(
            @PathVariable long residentId,
            @RequestParam(required = false) Instant at,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        if ((at == null) == (from == null || to == null) || (at != null && (from != null || to != null)))
            throw new InvalidValueException("Chọn thời điểm hoặc khoảng từ–đến");
        var rows = at == null ? history.residentStays(residentId, from, to) : history.residentAt(residentId, at);
        boolean pending = history.pending(residentId, at == null ? from : at, at == null ? to : at.plusNanos(1000));
        return rows.stream()
                .map(row -> new ResidentLocationHistoryRow(
                        row.allocationId(),
                        LocationResponse.of(row.location()),
                        row.from(),
                        row.to(),
                        row.corrected(),
                        pending))
                .toList();
    }

    @GetMapping("/location-history/beds")
    @ApiResponse(
            responseCode = "200",
            description = "Lịch sử theo giường",
            content =
                    @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = BedLocationHistoryRow.class))))
    public List<BedLocationHistoryRow> beds(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Instant at,
            @RequestParam(required = false) LocalDate date) {
        if ((roomId == null ? 0 : 1) + (floorId == null ? 0 : 1) + (areaId == null ? 0 : 1) != 1
                || (at == null) == (date == null))
            throw new InvalidValueException("Chọn một phòng/tầng/khu vực và thời điểm hoặc ngày");
        var ref = roomId != null
                ? new LocationRef("ROOM", roomId)
                : floorId != null ? new LocationRef("FLOOR", floorId) : new LocationRef("AREA", areaId);
        Instant from = at == null ? time.startOfDay(date) : at,
                to = at == null ? time.startOfDay(date.plusDays(1)) : at;
        var rows = history.locationStays(ref, from, to, at != null);
        Map<Long, LocationResponse> locations = new LinkedHashMap<>();
        history.beds(ref, from, at == null ? to : at.plusNanos(1000))
                .forEach(position -> locations.put(position.bedId(), LocationResponse.of(position)));
        rows.forEach(row -> locations.put(row.location().bedId(), LocationResponse.of(row.location())));
        Map<Long, String> names = new HashMap<>();
        return locations.values().stream()
                .map(location -> new BedLocationHistoryRow(
                        location,
                        rows.stream()
                                .filter(row -> row.location().bedId() == location.bedId())
                                .map(row -> new BedResidentHistoryRow(
                                        row.residentId(),
                                        names.computeIfAbsent(row.residentId(), history::name),
                                        row.from(),
                                        row.to(),
                                        row.corrected()))
                                .toList()))
                .toList();
    }

    @GetMapping("/location-history/roommates")
    @ApiResponse(
            responseCode = "200",
            description = "Người cùng phòng",
            content =
                    @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = RoommateHistoryRow.class))))
    public List<RoommateHistoryRow> roommates(
            @RequestParam long residentId, @RequestParam Instant from, @RequestParam Instant to) {
        var rows = history.roommateStays(residentId, from, to);
        Map<String, List<HistoryStay>> groups = new LinkedHashMap<>();
        rows.forEach(row -> groups.computeIfAbsent(
                        row.residentId() + ":" + row.location().roomId(), key -> new ArrayList<>())
                .add(row));
        Map<Long, List<HistoryAbsenceResponse>> absences = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        return groups.values().stream()
                .map(group -> {
                    var first = group.getFirst();
                    long other = first.residentId();
                    var facts = new ArrayList<HistoryAbsenceResponse>();
                    for (long id : List.of(residentId, other))
                        for (var absent : absences.computeIfAbsent(
                                id,
                                key -> history.absenceDetails(key, from, to).stream()
                                        .map(period -> new HistoryAbsenceResponse(
                                                period.residentId(), period.type(), period.from(), period.to()))
                                        .toList()))
                            for (var overlap : group) {
                                Instant start = absent.from().isBefore(overlap.from()) ? overlap.from() : absent.from();
                                Instant end = absent.to().isAfter(overlap.to()) ? overlap.to() : absent.to();
                                if (start.isBefore(end))
                                    facts.add(
                                            new HistoryAbsenceResponse(absent.residentId(), absent.type(), start, end));
                            }
                    return new RoommateHistoryRow(
                            other,
                            names.computeIfAbsent(other, history::name),
                            first.location().roomName(),
                            group.stream()
                                    .map(row -> new HistoryInterval(row.from(), row.to()))
                                    .toList(),
                            facts,
                            group.stream().anyMatch(HistoryStay::corrected),
                            history.pending(residentId, from, to) || history.pending(other, from, to));
                })
                .toList();
    }

    @PostMapping("/bed-allocations/{id}/corrections")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã đính chính hoặc lập yêu cầu", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "Điều kiện chưa thỏa",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<Void> allocationCorrection(@PathVariable long id, @Valid @RequestBody RecordCorrection body) {
        corrections.allocation(id, body.type(), body.correctedContent(), new Reason(body.reason()));
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/beds/{id}/state-history/{historyId}/corrections")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã đính chính hoặc lập yêu cầu", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "Điều kiện chưa thỏa",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<Void> readinessCorrection(
            @PathVariable long id, @PathVariable long historyId, @Valid @RequestBody RecordCorrection body) {
        corrections.readiness(id, historyId, body.type(), body.correctedContent(), new Reason(body.reason()));
        return ResponseEntity.status(201).build();
    }
}
