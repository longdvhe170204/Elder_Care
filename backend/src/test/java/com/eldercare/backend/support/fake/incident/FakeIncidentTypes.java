package com.eldercare.backend.support.fake.incident;

import com.eldercare.backend.incident.api.IncidentTypeApi;
import java.util.HashMap;
import java.util.Map;

/** Bản giả tra id loại sự cố: mã chưa khai báo nhận id cố định theo thứ tự gặp. */
public class FakeIncidentTypes implements IncidentTypeApi {
    private final Map<String, Long> ids = new HashMap<>();

    public FakeIncidentTypes type(String code, long id) {
        ids.put(code, id);
        return this;
    }

    @Override
    public long idOf(String code) {
        return ids.computeIfAbsent(code, c -> 900L + ids.size());
    }
}
