package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.UpdateClusterConfigFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.UpdateClusterConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.UpdateClusterConfigSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpdateClusterConfigHandler implements EventHandler<UpdateClusterConfigRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateClusterConfigHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateClusterConfigRequest.class);
    }

    @Override
    public void accept(Event<UpdateClusterConfigRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.updateConfig(stackId);
            response = new UpdateClusterConfigSuccess(stackId);
        } catch (RuntimeException e) {
            LOGGER.error("UpdateClusterConfigHandler step failed with the following message: {}", e.getMessage());
            response = new UpdateClusterConfigFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
