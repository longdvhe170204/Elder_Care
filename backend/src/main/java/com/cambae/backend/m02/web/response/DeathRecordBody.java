package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.internal.absence.service.AbsenceNames;
import com.cambae.backend.m02.internal.death.service.DeathRecordService;
import com.cambae.backend.m02.internal.death.service.DeathRecordService.Effective;
import com.cambae.backend.m15.account.api.FieldGroup;
import com.cambae.backend.m15.account.api.VisibilityTarget;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Bản ghi qua đời (contracts/api-004.yaml, schema {@code DeathRecord}; FR-069, BR-M02-21). {@code cause} thuộc nhóm {@code
 * HEALTH}: 002 bỏ khỏi phản hồi khi người xem không được xem (Hành chính, Kế toán, kể cả người đã ghi nhận) và ghi tên vào
 * {@code hiddenFields}. {@code target} chỉ để xét phạm vi, không ra ngoài.
 */
@Schema(name = "DeathRecord")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeathRecordBody(
        long id,
        Instant deathAt,
        String place,
        String discoveredBy,
        String confirmedBy,
        @FieldGroup(InfoGroup.HEALTH) String cause,
        Long evidenceFileId,
        boolean corrected,
        List<String> hiddenFields,
        @JsonIgnore @Schema(hidden = true) Target target)
        implements VisibilityTarget {

    public static DeathRecordBody of(Effective record, AbsenceNames names) {
        return new DeathRecordBody(
                record.id(),
                record.deathAt(),
                record.place(),
                record.discoveredBy(),
                names.staff(record.confirmedById()),
                record.cause(),
                record.evidenceFileId(),
                record.corrected(),
                List.of(),
                DeathRecordService.target(record.id(), record.residentId()));
    }

    @Override
    @JsonIgnore
    public Target visibilityTarget() {
        return target;
    }
}
