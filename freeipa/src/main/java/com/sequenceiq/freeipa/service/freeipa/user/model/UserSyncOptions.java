package com.sequenceiq.freeipa.service.freeipa.user.model;

public class UserSyncOptions {

    private final boolean fmsToFreeIpaBatchCallEnabled;

    private final boolean credentialsUpdateOptimizationEnabled;

    public UserSyncOptions(boolean fmsToFreeIpaBatchCallEnabled, boolean credentialsUpdateOptimizationEnabled) {
        this.fmsToFreeIpaBatchCallEnabled = fmsToFreeIpaBatchCallEnabled;
        this.credentialsUpdateOptimizationEnabled = credentialsUpdateOptimizationEnabled;
    }

    public boolean isFmsToFreeIpaBatchCallEnabled() {
        return fmsToFreeIpaBatchCallEnabled;
    }

    public boolean isCredentialsUpdateOptimizationEnabled() {
        return credentialsUpdateOptimizationEnabled;
    }
}
