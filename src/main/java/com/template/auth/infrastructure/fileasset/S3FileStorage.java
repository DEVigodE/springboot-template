package com.template.auth.infrastructure.fileasset;

import com.template.auth.application.fileasset.FileStoragePort;
import com.template.auth.config.AppProperties;
import java.io.InputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * FileStoragePort adapter backed by S3 (FILE_STORAGE=s3 + AWS_S3_BUCKET, and
 * optionally AWS_REGION). Credentials come from the SDK's default provider
 * chain (env vars, IAM role, ...) - never passed explicitly here.
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3FileStorage implements FileStoragePort {

    private final S3Client client;
    private final String bucket;

    public S3FileStorage(AppProperties properties) {
        AppProperties.Storage.S3 s3 = properties.storage().s3();
        if (s3.bucket().isBlank()) {
            throw new IllegalStateException("AWS_S3_BUCKET is required when FILE_STORAGE=s3");
        }
        S3ClientBuilder builder = S3Client.builder();
        if (!s3.region().isBlank()) {
            builder.region(Region.of(s3.region()));
        }
        this.client = builder.build();
        this.bucket = s3.bucket();
    }

    @Override
    public void save(String storageKey, byte[] content) {
        client.putObject(PutObjectRequest.builder().bucket(bucket).key(storageKey).build(),
                RequestBody.fromBytes(content));
    }

    @Override
    public InputStream open(String storageKey) {
        // GetObject fails on a missing key before any bytes stream - the same
        // "fail before headers are sent" guarantee LocalFileStorage checks for.
        return client.getObject(GetObjectRequest.builder().bucket(bucket).key(storageKey).build());
    }

    @Override
    public void delete(String storageKey) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build());
    }
}
