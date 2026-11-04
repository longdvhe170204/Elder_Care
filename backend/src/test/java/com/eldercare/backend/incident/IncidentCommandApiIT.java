package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.api.IncidentCommandApi;
import com.eldercare.backend.incident.internal.incident.service.DefaultIncidentCommandApi;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;

/** Bản thật {@code IncidentCommandApi} chỉ chạy trong transaction của lệnh gọi (module-interfaces 007 mục 1.6). */
class IncidentCommandApiIT extends MySqlIntegrationTestBase {
    @Autowired
    IncidentCommandApi commands;

    @Autowired
    com.eldercare.backend.incident.api.AlertCommandApi alertCommands;

    @Test
    void FR_026a_realBeanRequiresCallerTransaction() {
        assertThat(org.springframework.aop.support.AopUtils.getTargetClass(commands))
                .isEqualTo(DefaultIncidentCommandApi.class);
        var user = Mockito.mock(CurrentUser.class);

        assertThatThrownBy(() -> commands.acknowledge(1L, user)).isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> commands.addProgression(1L, "ACTION_TAKEN", "x", user))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> commands.record(null, user)).isInstanceOf(IllegalTransactionStateException.class);
        assertThat(org.springframework.aop.support.AopUtils.getTargetClass(alertCommands))
                .isEqualTo(com.eldercare.backend.incident.internal.alert.service.DefaultAlertCommandApi.class);
        assertThatThrownBy(() -> alertCommands.acknowledge(1L, user))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
