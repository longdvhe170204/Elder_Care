package com.eldercare.backend.support.fake.shared;

import static com.eldercare.backend.support.fake.shared.FakeContent.valid;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Bản giả {@link ResidentModuleStateApi} giữ dữ liệu trong bộ nhớ (module-interfaces 000 mục 1.8). */
public class FakeResidentModuleStateApi implements ResidentModuleStateApi {

    private final Map<String, Object> states = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(SharedKind<T> kind, long residentId) {
        return Optional.ofNullable((T) states.get(key(kind, residentId)));
    }

    @Override
    public <T> Optional<T> lockForUpdate(SharedKind<T> kind, long residentId) {
        return get(kind, residentId);
    }

    @Override
    public <T> void save(SharedKind<T> kind, long residentId, T data) {
        states.put(key(kind, residentId), valid(data));
    }

    private static String key(SharedKind<?> kind, long residentId) {
        return kind.module() + ":" + residentId;
    }
}
