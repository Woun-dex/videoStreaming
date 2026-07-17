package com.dev.videoStreaming.video.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.SetBucketPolicyArgs;

@Configuration
public class minioConfig {

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint("http://localhost:9000")
                .credentials("minioadmin", "minioadmin")
                .build();

        try {
            // Ensure bucket exists
            boolean found = client.bucketExists(BucketExistsArgs.builder().bucket("videos").build());
            if (!found) {
                client.makeBucket(MakeBucketArgs.builder().bucket("videos").build());
            }

            // Define a public read policy for the "videos" bucket
            String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": "s3:GetObject",
                      "Resource": "arn:aws:s3:::videos/*"
                    }
                  ]
                }
                """;

            // Apply the policy
            client.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket("videos")
                .config(policy)
                .build());
                
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize MinIO bucket", e);
        }

        return client;
    }
}
