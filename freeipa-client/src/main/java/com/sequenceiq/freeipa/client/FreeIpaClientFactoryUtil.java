package com.sequenceiq.freeipa.client;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;

import io.opentracing.Tracer;

public class FreeIpaClientFactoryUtil {

    public static final String ADMIN_USER = "admin";

    public static HttpClientConfig getHttpClientConfig(String apiAddress) {
        return new HttpClientConfig(apiAddress);
    }

    public static FreeIpaClientBuilder getDirectFreeipaClientBuilder(String adminPassword, HttpClientConfig httpClientConfig,
            int gatewayPort, String fqdn, Tracer tracer) throws Exception {
        return new FreeIpaClientBuilder(ADMIN_USER, adminPassword, httpClientConfig, gatewayPort, fqdn, tracer);
    }
}
