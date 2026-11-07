package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.emergency.domain.EmergencyCard;
import com.eldercare.backend.incident.internal.emergency.domain.TransferOutInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashMap;
import java.util.Map;

/** Thông tin màn lệnh Chuyển viện, schema {@code TransferOutInfo} của api-007 (FR-049). */
@Schema(name = "TransferOutInfo")
public record TransferOutInfoResponse(
        Map<String, Object> treatmentWish,
        Map<String, Object> endOfLifeWish,
        Map<String, Object> primaryContact,
        Boolean mustAskFirst,

        @JsonProperty("isEmergency")
        @Schema(name = "isEmergency", description = "Sự cố mức Khẩn cấp: không bắt notAskedFirstReason")
        Boolean isEmergency) {

    public static TransferOutInfoResponse of(TransferOutInfo info) {
        return new TransferOutInfoResponse(
                info.treatmentWish(),
                info.endOfLifeWish(),
                contact(info.primaryContact()),
                info.mustAskFirst(),
                info.emergency());
    }

    static Map<String, Object> contact(EmergencyCard.Contact contact) {
        if (contact == null) return null;
        var map = new LinkedHashMap<String, Object>();
        map.put("fullName", contact.fullName());
        map.put("phoneNumber", contact.phoneNumber());
        return map;
    }
}
