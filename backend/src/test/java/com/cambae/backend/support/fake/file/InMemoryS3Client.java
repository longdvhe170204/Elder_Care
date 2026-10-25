package com.cambae.backend.support.fake.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Bản giả {@link S3Client} lưu trong bộ nhớ (001 T003; research H-13): test không cần tài khoản AWS, không chạy
 * LocalStack hay MinIO (000 R-14). Chỉ cài ba thao tác mà {@code FileService} dùng.
 */
public class InMemoryS3Client implements S3Client {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();
    private volatile boolean failPuts;
    private volatile boolean failDeletes;

    public boolean contains(String key) {
        return objects.containsKey(key);
    }

    public int count() {
        return objects.size();
    }

    /** Các lần {@code putObject} sau ném lỗi của SDK, như lúc S3 không với tới được. */
    public void failPuts(boolean fail) {
        this.failPuts = fail;
    }

    /** Các lần {@code deleteObject} sau ném lỗi của SDK. */
    public void failDeletes(boolean fail) {
        this.failDeletes = fail;
    }

    @Override
    public PutObjectResponse putObject(PutObjectRequest request, RequestBody body) {
        if (failPuts) {
            throw SdkClientException.create("S3 không với tới được");
        }
        try (var in = body.contentStreamProvider().newStream()) {
            objects.put(request.key(), in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return PutObjectResponse.builder().build();
    }

    @Override
    public ResponseBytes<GetObjectResponse> getObjectAsBytes(GetObjectRequest request) {
        byte[] bytes = objects.get(request.key());
        if (bytes == null) {
            throw NoSuchKeyException.builder()
                    .message("Không có đối tượng " + request.key())
                    .build();
        }
        return ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes);
    }

    @Override
    public DeleteObjectResponse deleteObject(DeleteObjectRequest request) {
        if (failDeletes) {
            throw SdkClientException.create("S3 không với tới được");
        }
        objects.remove(request.key());
        return DeleteObjectResponse.builder().build();
    }

    @Override
    public String serviceName() {
        return "s3";
    }

    @Override
    public void close() {}
}
