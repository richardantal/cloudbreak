package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerSetupMonitoringFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerSetupMonitoringRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerSetupMonitoringSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterManagerSetupMonitoringHandler implements EventHandler<ClusterManagerSetupMonitoringRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerSetupMonitoringHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterManagerSetupMonitoringRequest.class);
    }

    @Override
    public void accept(Event<ClusterManagerSetupMonitoringRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.setupMonitoring(stackId);
            response = new ClusterManagerSetupMonitoringSuccess(stackId);
        } catch (RuntimeException | CloudbreakException e) {
            LOGGER.error("ClusterManagerSetupMonitoringHandler step failed with the following message: {}", e.getMessage());
            response = new ClusterManagerSetupMonitoringFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
