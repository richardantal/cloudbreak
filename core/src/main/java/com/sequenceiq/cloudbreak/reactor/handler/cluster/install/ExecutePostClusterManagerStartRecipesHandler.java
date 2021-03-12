package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostClusterManagerStartRecipesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostClusterManagerStartRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostClusterManagerStartRecipesSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ExecutePostClusterManagerStartRecipesHandler implements EventHandler<ExecutePostClusterManagerStartRecipesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutePostClusterManagerStartRecipesHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExecutePostClusterManagerStartRecipesRequest.class);
    }

    @Override
    public void accept(Event<ExecutePostClusterManagerStartRecipesRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.executePostClusterManagerStartRecipes(stackId);
            response = new ExecutePostClusterManagerStartRecipesSuccess(stackId);
        } catch (RuntimeException | CloudbreakException e) {
            LOGGER.error("ExecutePostClusterManagerStartRecipesHandler step failed with the following message: {}", e.getMessage());
            response = new ExecutePostClusterManagerStartRecipesFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
