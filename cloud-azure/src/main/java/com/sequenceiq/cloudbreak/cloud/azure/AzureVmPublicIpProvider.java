package com.sequenceiq.cloudbreak.cloud.azure;

import org.springframework.stereotype.Component;

import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.PublicIPAddress;

@Component
class AzureVmPublicIpProvider {

    String getPublicIp(NetworkInterface networkInterface) {
        PublicIPAddress publicIpAddress = networkInterface.primaryIPConfiguration().getPublicIPAddress();

        if (publicIpAddress != null && publicIpAddress.ipAddress() != null) {
            return publicIpAddress.ipAddress();
        } else {
            return null;
        }
    }
}
