/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.app;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Overrides the {@link AmazonS3} client declared by the S3 artifact repository
 * extension, which builds its client without path style access. S3 compatible
 * stores such as MinIO are typically reachable under a single host only, so the
 * virtual host style addressing used by default ({@code bucket.host}) cannot be
 * resolved against them.
 *
 * <p>
 * The extension declares its own client as {@code @ConditionalOnMissingBean}, so
 * this bean simply takes precedence. Path style access stays off by default and
 * is enabled with {@code aws.s3.path-style-access=true}.
 */
@Configuration
@ConditionalOnProperty(prefix = "org.eclipse.hawkbit.artifact.repository.s3", name = "enabled", matchIfMissing = true)
public class S3ClientConfiguration {

    @Value("${aws.region:#{null}}")
    private String region;

    @Value("${aws.s3.endpoint:#{null}}")
    private String endpoint;

    @Value("${aws.s3.path-style-access:false}")
    private boolean pathStyleAccess;

    /**
     * @param credentialsProvider the credentials provider contributed by the extension
     * @param clientConfiguration the client configuration contributed by the extension
     * @return the {@link AmazonS3} client used by the artifact repository
     */
    @Bean
    public AmazonS3 amazonS3(final AWSCredentialsProvider credentialsProvider,
            final ClientConfiguration clientConfiguration) {
        final AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard()
                .withCredentials(credentialsProvider).withClientConfiguration(clientConfiguration)
                .withPathStyleAccessEnabled(pathStyleAccess);
        if (StringUtils.hasLength(endpoint)) {
            final String signingRegion = StringUtils.hasLength(region) ? region : "";
            s3ClientBuilder.withEndpointConfiguration(new EndpointConfiguration(endpoint, signingRegion));
        } else if (StringUtils.hasLength(region)) {
            s3ClientBuilder.withRegion(region);
        }
        return s3ClientBuilder.build();
    }
}
