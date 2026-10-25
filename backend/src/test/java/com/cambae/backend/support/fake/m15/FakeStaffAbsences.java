package com.cambae.backend.support.fake.m15;

import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.spi.StaffAbsenceApi;
import java.util.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

/** Một bản giả của SPI do 002 khai báo; chỉ nạp khi test chủ động import Config. */
public class FakeStaffAbsences implements StaffAbsenceApi {
    private record Key(long account, long absence) {}

    private final Set<Key> granted = new HashSet<>();

    public void allow(long account, long absence) {
        granted.add(new Key(account, absence));
    }

    public void clear() {
        granted.clear();
    }

    public boolean canViewReason(CurrentUser user, long absenceId) {
        return granted.contains(new Key(user.accountId(), absenceId));
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @Bean
        @Primary
        FakeStaffAbsences fakeStaffAbsences() {
            return new FakeStaffAbsences();
        }
    }
}
