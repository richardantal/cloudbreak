package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.StartClusterManagerManagementServicesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.StartClusterManagerManagementServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.StartClusterManagerManagementServicesSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class StartClusterManagerManagementServicesHandler implements EventHandler<StartClusterManagerManagementServicesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartClusterManagerManagementServicesHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StartClusterManagerManagementServicesRequest.class);
    }

    @Override
    public void accept(Event<StartClusterManagerManagementServicesRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.startManagementServices(stackId);
            response = new StartClusterManagerManagementServicesSuccess(stackId);
        } catch (RuntimeException e) {
            LOGGER.error("StartClusterManagerManagementServicesHandler step failed with the following message: {}", e.getMessage());
            response = new StartClusterManagerManagementServicesFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
