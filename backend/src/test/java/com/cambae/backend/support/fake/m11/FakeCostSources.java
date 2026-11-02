package com.cambae.backend.support.fake.m11;

import com.cambae.backend.m11.api.events.CostSourceBilled;
import com.cambae.backend.m11.api.events.CostSourceChanged;
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
