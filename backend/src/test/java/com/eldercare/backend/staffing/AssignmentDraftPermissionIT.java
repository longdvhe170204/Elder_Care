package com.eldercare.backend.staffing;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Cô lập khóa thao tác; nguồn và luật phân công vẫn chạy thật như IT HTTP. */
class AssignmentDraftPermissionIT extends AssignmentHttpTestBase {
    @MockitoBean
    PermissionApi permissions;

    @Test
    void FR_025_updateDoesNotRequireCreateForPreviewOrSave() throws Exception {
        when(permissions.hasPermission(any(), any(), any())).thenAnswer(call -> {
            PermissionKey key = call.getArgument(1);
            return key.operation() == Operation.CREATE
                    ? PermissionResult.denied(FailedLayer.ROLE)
                    : new PermissionResult(true, Optional.empty(), Optional.empty(), Optional.empty());
        });
        String path = "/api/v1/assignments/" + assignment;
        mvc.perform(post(path + "/draft-update-preview")
                        .header("Authorization", bearer)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body())))
                .andExpect(status().isOk());
        mvc.perform(post(path + "/update-draft")
                        .header("Authorization", bearer)
                        .header("If-Match", "\"0\"")
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body())))
                .andExpect(status().isOk());
        verify(permissions, never()).hasPermission(any(), argThat(k -> k.operation() == Operation.CREATE), any());
    }
}
