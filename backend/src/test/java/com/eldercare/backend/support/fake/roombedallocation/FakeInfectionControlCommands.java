package com.eldercare.backend.support.fake.roombedallocation;

import com.eldercare.backend.roombedallocation.api.InfectionControlCommandApi;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Bản giả chung của 003 cho 007 (T053, T054): ghi lại lời gọi để test của 007 kiểm lệnh gỡ khoanh vùng, thêm vào danh
 * sách tiếp xúc có gọi 003; {@link #fail} cho lời gọi kế tiếp ném lỗi để kiểm lệnh 007 rollback.
 */
public class FakeInfectionControlCommands implements InfectionControlCommandApi {
    public record CordonLifted(Set<Long> floorIds, Set<Long> areaIds, Instant at) {}

    public record ContactListed(long residentId, Instant at) {}

    public final List<CordonLifted> cordonsLifted = new ArrayList<>();
    public final List<ContactListed> contactsListed = new ArrayList<>();
    private RuntimeException failure;

    public FakeInfectionControlCommands fail(RuntimeException error) {
        failure = error;
        return this;
    }

    public void reset() {
        cordonsLifted.clear();
        contactsListed.clear();
        failure = null;
    }

    private void maybeFail() {
        var error = failure;
        failure = null;
        if (error != null) throw error;
    }

    @Override
    public void cordonLifted(Set<Long> floorIds, Set<Long> areaIds, Instant at) {
        maybeFail();
        cordonsLifted.add(new CordonLifted(Set.copyOf(floorIds), Set.copyOf(areaIds), at));
    }

    @Override
    public void contactListed(long residentId, Instant at) {
        maybeFail();
        contactsListed.add(new ContactListed(residentId, at));
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        FakeInfectionControlCommands fakeInfectionControlCommands() {
            return new FakeInfectionControlCommands();
        }
    }
}
