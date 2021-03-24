package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.common.api.type.LoadBalancerType;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public final class AzureLoadBalancer {
    private static final String LOAD_BALANCER_NAME_PREFIX = "LoadBalancer";

    private final List<AzureLoadBalancingRule> rules;

    private final Set<AzureLoadBalancerProbe> probes;

    private final String name;

    private final LoadBalancerType type;

    private final String instanceGroupName;

    public AzureLoadBalancer(CloudLoadBalancer cloudLoadBalancer) {
        rules = cloudLoadBalancer.getPortToTargetGroupMapping()
                .keySet()
                .stream()
                .map(AzureLoadBalancingRule::new)
                .collect(toList());

        // we want to derive the set of probes from the rules used in the load balancer
        probes = rules.stream()
                .map(AzureLoadBalancingRule::getProbe)
                .collect(toSet());

        this.name = getLoadBalancerName(cloudLoadBalancer.getType());
        this.type = cloudLoadBalancer.getType();
        // we validate there's only one target instance group when the
        // load balancer object is first initiatlized, so get the first
        // and only element in the set
        this.instanceGroupName = cloudLoadBalancer.getPortToTargetGroupMapping().values().stream()
            .map(groups -> groups.iterator().next().getName())
            .findFirst().get();
    }

    public static String getLoadBalancerName(LoadBalancerType type) {
        return LOAD_BALANCER_NAME_PREFIX + type.toString();
    }

    public Collection<AzureLoadBalancingRule> getRules() {
        return rules;
    }

    public Collection<AzureLoadBalancerProbe> getProbes() {
        return probes;
    }

    public String getName() {
        return name;
    }

    public LoadBalancerType getType() {
        return type;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }
}
