package com.cambae.backend.m01.web;

import com.cambae.backend.common.file.FileContent;
import com.cambae.backend.common.file.FileService;
import com.cambae.backend.common.file.FileTypes;
import com.cambae.backend.m15.account.api.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Tải tệp lên và xuống (001 T004; contracts/api-001.yaml, tag {@code files}). Đặt ở M01 vì cần người dùng hiện tại của
 * M15, mà {@code common} không gọi ngược module; logic và quyền tải ở {@link FileService}. Tệp vừa tải lên chưa có chủ
 * sở hữu: chỉ người tải lên tải lại được tới khi lệnh nghiệp vụ gắn tệp vào bản ghi.
 */
@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "files")
class FileController {

    private final FileService files;
    private final CurrentUser currentUser;

    FileController(FileService files, CurrentUser currentUser) {
        this.files = files;
        this.currentUser = currentUser;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Map<String, Long>> upload(@RequestParam("file") MultipartFile file) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // POST /files chỉ nhận PDF, JPEG, PNG; FileApi.upload nhận thêm .xlsx cho tệp hệ thống tạo
        FileTypes.check(file.getContentType(), content, FileTypes.USER_UPLOAD);
        long id = files.upload(file.getOriginalFilename(), file.getContentType(), content, currentUser.accountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("fileId", id));
    }

    @GetMapping("/{id}")
    ResponseEntity<byte[]> download(@PathVariable long id) {
        FileContent file = files.download(id, currentUser.accountId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(file.originalName())
                                .build()
                                .toString())
                .body(file.content());
    }

    /** Tệp quá giới hạn của multipart: cùng mã lỗi với kiểm kích thước trong {@link FileTypes#check}. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ProblemDetail> tooLarge() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, "Tệp quá 10 MB");
        problem.setTitle(HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase());
        problem.setProperty("errorCode", "FILE_TOO_LARGE");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problem);
    }
}
