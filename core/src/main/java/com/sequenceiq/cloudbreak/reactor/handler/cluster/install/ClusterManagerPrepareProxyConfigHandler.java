package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerPrepareProxyConfigFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerPrepareProxyConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerPrepareProxyConfigSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterManagerPrepareProxyConfigHandler implements EventHandler<ClusterManagerPrepareProxyConfigRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerPrepareProxyConfigHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterManagerPrepareProxyConfigRequest.class);
    }

    @Override
    public void accept(Event<ClusterManagerPrepareProxyConfigRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.prepareProxyConfig(stackId);
            response = new ClusterManagerPrepareProxyConfigSuccess(stackId);
        } catch (RuntimeException e) {
            LOGGER.error("ClusterManagerPrepareProxyConfigHandler step failed with the following message: {}", e.getMessage());
            response = new ClusterManagerPrepareProxyConfigFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
