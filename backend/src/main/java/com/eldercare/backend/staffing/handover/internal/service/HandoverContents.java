package com.eldercare.backend.staffing.handover.internal.service;

import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.staffing.handover.internal.domain.HandoverItem;
import com.eldercare.backend.staffing.handover.internal.persistence.HandoverStore;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** FR-046: nội dung hiện hành lấy từ 000; không viết lại bản gốc hay chạy lại chuyển công việc. */
@Component
public class HandoverContents {
    private final HandoverStore store;
    private final ObjectProvider<CorrectionApi> corrections;

    public HandoverContents(HandoverStore store, ObjectProvider<CorrectionApi> corrections) {
        this.store = store;
        this.corrections = corrections;
    }

    public Map<String, Object> original(long id) {
        var result = new LinkedHashMap<String, Object>();
        result.put("generalAssessment", store.general(id));
        result.put(
                "items",
                store.items(id).stream()
                        .map(i -> {
                            var m = new LinkedHashMap<String, Object>();
                            m.put("id", i.id());
                            m.put("description", i.description());
                            m.put("note", i.note());
                            return m;
                        })
                        .toList());
        result.put("residentAssessments", store.assessments(id));
        result.put("comments", store.comments(id));
        return result;
    }

    public Map<?, ?> corrected(long id) {
        if (!"CONFIRMED".equals(store.record(id, false).status())) return Map.of();
        var value = corrections.getObject().current("M09.HANDOVER", List.of(id)).get(id);
        return value != null && value.content() instanceof Map<?, ?> map ? map : Map.of();
    }

    public String general(long id) {
        var m = corrected(id);
        return m.containsKey("generalAssessment") ? (String) m.get("generalAssessment") : store.general(id);
    }

    public List<HandoverItem> items(long id) {
        var rows = patches(corrected(id).get("items"), "id");
        return store.items(id).stream()
                .map(i -> {
                    var p = rows.get(i.id());
                    return p == null
                            ? i
                            : new HandoverItem(
                                    i.id(),
                                    i.handoverId(),
                                    i.group(),
                                    i.sourceType(),
                                    i.sourceId(),
                                    i.residentId(),
                                    i.roomId(),
                                    text(p, "description", i.description()),
                                    text(p, "note", i.note()),
                                    i.resolved(),
                                    i.section(),
                                    i.statusAtFinalization(),
                                    i.noteAuthor());
                })
                .toList();
    }

    public List<Map<String, Object>> assessments(long id) {
        return merge(store.assessments(id), corrected(id).get("residentAssessments"), "residentId");
    }

    public List<Map<String, Object>> comments(long id) {
        var patches = patches(corrected(id).get("comments"), "id");
        return store.comments(id).stream()
                .map(original -> {
                    var result = new LinkedHashMap<>(original);
                    var p = patches.get(((Number) original.get("id")).longValue());
                    if (p != null) {
                        if (p.containsKey("content")) result.put("content", p.get("content"));
                        if (p.get("replies") instanceof List<?> replies) {
                            var old = (List<?>) original.get("replies");
                            var next = new ArrayList<Map<String, Object>>();
                            for (int i = 0; i < old.size(); i++) {
                                var reply = new LinkedHashMap<String, Object>();
                                ((Map<?, ?>) old.get(i)).forEach((k, v) -> reply.put(k.toString(), v));
                                if (i < replies.size()
                                        && replies.get(i) instanceof Map<?, ?> edit
                                        && edit.containsKey("content")) reply.put("content", edit.get("content"));
                                next.add(reply);
                            }
                            result.put("replies", next);
                        }
                    }
                    return (Map<String, Object>) result;
                })
                .toList();
    }

    private List<Map<String, Object>> merge(List<Map<String, Object>> original, Object edits, String key) {
        var rows = patches(edits, key);
        return original.stream()
                .map(r -> {
                    var out = new LinkedHashMap<>(r);
                    var p = rows.get(((Number) r.get(key)).longValue());
                    if (p != null && p.containsKey("content")) out.put("content", p.get("content"));
                    return (Map<String, Object>) out;
                })
                .toList();
    }

    static Map<Long, Map<?, ?>> patches(Object input, String key) {
        var result = new LinkedHashMap<Long, Map<?, ?>>();
        if (input instanceof List<?> list)
            for (var row : list)
                if (row instanceof Map<?, ?> m && m.get(key) instanceof Number n) result.put(n.longValue(), m);
        return result;
    }

    private static String text(Map<?, ?> m, String key, String fallback) {
        return m.containsKey(key) ? (String) m.get(key) : fallback;
    }
}
