package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.infection.domain.ContactLookup;
import com.eldercare.backend.incident.web.request.ContactBody;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Schema {@code ContactLookupResult} của api-007 (FR-060): khoảng truy vết, khoảng vắng của người nghi nhiễm, nhóm. */
@Schema(name = "ContactLookupResult")
public record ContactLookupResponse(
        Instant traceFrom, Instant traceTo, List<Map<String, Object>> suspectedPersonAbsences, List<Group> group) {

    public static ContactLookupResponse from(ContactLookup lookup) {
        return new ContactLookupResponse(
                lookup.traceFrom(),
                lookup.traceTo(),
                lookup.suspectedPersonAbsences().stream()
                        .map(a -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("status", a.status());
                            row.put("from", a.from());
                            row.put("to", a.to());
                            return row;
                        })
                        .toList(),
                lookup.groups().stream()
                        .map(g -> new Group(
                                g.group().name(),
                                g.people().stream().map(ContactBody::from).toList(),
                                g.noData()))
                        .toList());
    }

    @Schema(name = "ContactLookupGroup")
    public record Group(String group, List<ContactBody> people, boolean noData) {}
}
