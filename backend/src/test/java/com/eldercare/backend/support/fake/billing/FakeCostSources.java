package com.eldercare.backend.support.fake.billing;

import com.eldercare.backend.billing.api.events.CostSourceBilled;
import com.eldercare.backend.billing.api.events.CostSourceChanged;
import org.springframework.context.ApplicationEventPublisher;

/** T002: fixture phát nguồn do chủ sự kiện giữ; chỉ dùng trong test, không thay producer thật. */
public class FakeCostSources {
    private final ApplicationEventPublisher publisher;

    public FakeCostSources(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void billed(CostSourceBilled event) {
        publisher.publishEvent(event);
    }

    public void changed(CostSourceChanged event) {
        publisher.publishEvent(event);
    }
}
