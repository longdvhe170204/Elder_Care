package com.eldercare.backend.administration.shared;

import static com.eldercare.backend.administration.shared.SharedTestKinds.OTHER_MODULE_STATE;
import static com.eldercare.backend.administration.shared.SharedTestKinds.STATE;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.shared.SharedTestKinds.Attributes;
import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.support.TestUsers;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Hợp đồng của {@link ResidentModuleStateApi} (module-interfaces 000 mục 1.8), chạy trên bản thật và bản giả. */
public interface ResidentModuleStateApiContract {

    ResidentModuleStateApi states();

    <R> R tx(Supplier<R> work);

    @Test
    default void save_thenGet_replaces_andIsPerModule() {
        long resident = TestUsers.nextId();
        assertThat(tx(() -> states().get(STATE, resident))).isEmpty();

        tx(() -> {
            states().save(STATE, resident, new Attributes("đỏ", 1));
            return null;
        });
        tx(() -> {
            assertThat(states().lockForUpdate(STATE, resident)).contains(new Attributes("đỏ", 1));
            states().save(STATE, resident, new Attributes("xanh", 2));
            return null;
        });

        assertThat(tx(() -> states().get(STATE, resident))).contains(new Attributes("xanh", 2));
        assertThat(tx(() -> states().get(OTHER_MODULE_STATE, resident))).isEmpty();
    }
}
