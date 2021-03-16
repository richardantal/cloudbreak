package com.sequenceiq.authorization.service.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
public class ResourceFilteringService {

    @Inject
    private GrpcUmsClient umsClient;

    public <R extends Resource, E> List<E> filter(
            Crn userCrn,
            AuthorizationResourceAction action,
            List<R> resources,
            Function<Predicate<String>, List<E>> resultMapper) {
        if (CollectionUtils.isEmpty(resources)) {
            return List.of();
        }
        Map<Optional<String>, List<R>> resourcesByParents = new LinkedHashMap<>();
        resources.forEach(resource -> resourcesByParents
                .computeIfAbsent(resource.getParentResourceCrn(), ignored -> new ArrayList<>())
                .add(resource));

        List<String> resourceCrns = new ArrayList<>();
        for (Map.Entry<Optional<String>, List<R>> entry : resourcesByParents.entrySet()) {
            Optional<String> parentResource = entry.getKey();
            List<R> subResources = entry.getValue();
            if (parentResource.isPresent()) {
                resourceCrns.add(parentResource.get());
            }
            resourceCrns.addAll(subResources
                    .stream()
                    .map(Resource::getResourceCrn)
                    .collect(Collectors.toList()));
        }

        Iterator<Boolean> resultIterator = umsClient.hasRightsOnResources(userCrn.toString(), userCrn.toString(), resourceCrns, action.getRight(),
                getRequestId())
                .iterator();
        Map<String, Boolean> resultMap = new HashMap<>();
        resourcesByParents.forEach((parentResource, subResources) -> {
            if (parentResource.isPresent() && resultIterator.next()) {
                for (Resource resource : subResources) {
                    resultIterator.next();
                    resultMap.put(resource.getResourceCrn(), Boolean.TRUE);
                }
            } else {
                for (Resource resource : subResources) {
                    resultMap.put(resource.getResourceCrn(), resultIterator.next());
                }
            }
        });
        return resultMapper.apply(resourceCrn -> resultMap.getOrDefault(resourceCrn, Boolean.FALSE));
    }

    private Optional<String> getRequestId() {
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        return Optional.of(requestId);
    }
}
