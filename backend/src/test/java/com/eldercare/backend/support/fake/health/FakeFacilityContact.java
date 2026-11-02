package com.eldercare.backend.support.fake.health;

import com.eldercare.backend.health.api.FacilityContactApi;
import com.eldercare.backend.health.api.dto.FacilityContactView;

/** Số liên hệ do test cấu hình; mặc định chưa có số, không bịa số liên hệ của viện. */
public class FakeFacilityContact implements FacilityContactApi {
    private FacilityContactView current = new FacilityContactView(null, null, 0);

    public void source(FacilityContactView value) {
        current = value;
    }

    @Override
    public FacilityContactView current() {
        return current;
    }
}
