package com.eldercare.backend.resident;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * 001 T004 (contracts/api-001.yaml, tag {@code files}): {@code POST /files} nhận PDF, JPEG, PNG tối đa 10 MB và trả
 * {@code fileId}; {@code .xlsx} người dùng tải lên bị từ chối (chỉ tệp hệ thống tạo mới có); {@code GET /files/{id}}
 * trả nội dung cho người tải lên khi tệp chưa gắn, người khác 403, không có tệp 404.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import({FakeFileConfig.class, FileEndpointsIT.Users.class})
class FileEndpointsIT extends MySqlIntegrationTestBase {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3};

    @TestConfiguration
    static class Users {

        @Bean
        SwitchableUser currentUser() {
            return new SwitchableUser();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    SwitchableUser user;

    @Autowired
    JsonMapper json;

    @Test
    void upload_returns201WithFileId_andUploaderCanDownload() throws Exception {
        user.set(FakePermissions.user(41, Role.OFFICE_STAFF));

        MvcResult created = mvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "ban-scan.png", FileTypes.PNG, PNG)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileId").isNumber())
                .andReturn();
        long id = json.readTree(created.getResponse().getContentAsString())
                .get("fileId")
                .asLong();

        mvc.perform(get("/api/v1/files/" + id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", FileTypes.PNG))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("ban-scan.png")))
                .andExpect(content().bytes(PNG));

        user.set(FakePermissions.user(42, Role.OFFICE_STAFF));
        mvc.perform(get("/api/v1/files/" + id)).andExpect(status().isForbidden());
    }

    @Test
    void upload_xlsxFromUser_is422() throws Exception {
        user.set(FakePermissions.user(41, Role.OFFICE_STAFF));
        byte[] zip = {'P', 'K', 3, 4, 0, 0};

        mvc.perform(multipart("/api/v1/files").file(new MockMultipartFile("file", "a.xlsx", FileTypes.XLSX, zip)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("FILE_TYPE_NOT_ALLOWED"));
    }

    @Test
    void upload_contentNotMatchingDeclaredType_is422() throws Exception {
        user.set(FakePermissions.user(41, Role.OFFICE_STAFF));

        mvc.perform(multipart("/api/v1/files").file(new MockMultipartFile("file", "a.pdf", FileTypes.PDF, PNG)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("FILE_TYPE_NOT_ALLOWED"));
    }

    @Test
    void download_unknownFile_is404() throws Exception {
        user.set(FakePermissions.user(41, Role.OFFICE_STAFF));

        mvc.perform(get("/api/v1/files/987654321")).andExpect(status().isNotFound());
    }
}
