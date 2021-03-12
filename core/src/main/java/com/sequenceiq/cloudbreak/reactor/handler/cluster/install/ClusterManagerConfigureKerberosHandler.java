package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerConfigureKerberosFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerConfigureKerberosRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerConfigureKerberosSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterManagerConfigureKerberosHandler implements EventHandler<ClusterManagerConfigureKerberosRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerConfigureKerberosHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterManagerConfigureKerberosRequest.class);
    }

    @Override
    public void accept(Event<ClusterManagerConfigureKerberosRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.configureKerberos(stackId);
            response = new ClusterManagerConfigureKerberosSuccess(stackId);
        } catch (RuntimeException | CloudbreakException e) {
            LOGGER.error("ClusterManagerConfigureKerberosHandler step failed with the following message: {}", e.getMessage());
            response = new ClusterManagerConfigureKerberosFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
