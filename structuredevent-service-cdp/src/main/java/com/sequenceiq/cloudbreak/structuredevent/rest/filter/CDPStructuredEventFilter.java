package com.sequenceiq.cloudbreak.structuredevent.rest.filter;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.REST;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.message.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPBaseRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPDefaultStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.service.lookup.CDPAccountAwareRepositoryLookupService;
import com.sequenceiq.flow.ha.NodeConfig;

@Component
public class CDPStructuredEventFilter implements WriterInterceptor, ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPStructuredEventFilter.class);

    private static final String LOGGING_ENABLED_PROPERTY = "structuredevent.loggingEnabled";

    private static final String LOGGINGSTREAM_PROPERTY = "structuredevent.entityLogger";

    private static final String REST_PARAMS = "REST_PARAMS";

    private static final String REQUEST_TIME = "REQUEST_TIME";

    private static final String REQUEST_DETAILS = "REQUEST_DETAIS";

    private static final String RESPONSE_DETAILS = "RESPONSE_DETAIS";

    private static final int MAX_CONTENT_LENGTH = 65535;

    private static final String ID = "id";

    private static final String CRN = "crn";

    private final List<String> skippedHeadersList = Lists.newArrayList("authorization");

    private final Map<String, AccountAwareResourceRepository<?, ?>> pathRepositoryMap = new HashMap<>();

    private final Pattern extractIdRestParamFromResponsePattern = Pattern.compile("\"" + ID + "\":([0-9]*)");

    private final Pattern extractCrnRestParamFromResponsePattern = Pattern.compile("\"" + CRN + "\":\"([0-9a-zA-Z:-]*)\"");

    @Value("${info.app.version:}")
    private String cbVersion;

    @Value("${structuredevent.rest.contentlogging:false}")
    private Boolean contentLogging;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private NodeConfig nodeConfig;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private CDPBaseRestRequestThreadLocalService cloudbreakRestRequestThreadLocalService;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private CDPDefaultStructuredEventClient structuredEventClient;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private List<CDPRestUrlParser> cdpRestUrlParsers;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private ApplicationContext applicationContext;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private ListableBeanFactory listableBeanFactory;

    //Do not remove the @Autowired annotation Jersey is able to inject dependencies that are instantiated by Spring this way only!
    @Autowired
    private CDPAccountAwareRepositoryLookupService repositoryLookupService;

    @PostConstruct
    public void initializePathRepositoryMap() {
        Map<String, Object> accountEntityTypes = listableBeanFactory.getBeansWithAnnotation(AccountEntityType.class);
        for (Object accountEntityType : accountEntityTypes.values()) {
            Path pathAnnotation = AnnotationUtils.findAnnotation(accountEntityType.getClass().getSuperclass(), Path.class);
            AccountEntityType entityTypeAnnotation = AnnotationUtils.findAnnotation(accountEntityType.getClass(), AccountEntityType.class);
            if (pathAnnotation != null) {
                String pathValue = pathAnnotation.value();
                Class<?> entityClass = entityTypeAnnotation.value();
                AccountAwareResourceRepository<?, ?> repository = repositoryLookupService.getRepositoryForEntity(entityClass);
                pathRepositoryMap.put(pathValue, repository);
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        boolean loggingEnabled = isLoggingEnabled(requestContext);
        requestContext.setProperty(LOGGING_ENABLED_PROPERTY, loggingEnabled);
        if (loggingEnabled) {
            requestContext.setProperty(REQUEST_TIME, System.currentTimeMillis());
            StringBuilder body = new StringBuilder();
            requestContext.setEntityStream(logInboundEntity(body, requestContext.getEntityStream(), MessageUtils.getCharset(requestContext.getMediaType())));
            requestContext.setProperty(REST_PARAMS, getRequestUrlParameters(requestContext));
            requestContext.setProperty(REQUEST_DETAILS, createRequestDetails(requestContext, body.toString()));
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (BooleanUtils.isTrue((Boolean) requestContext.getProperty(LOGGING_ENABLED_PROPERTY))) {
            RestResponseDetails restResponse = createResponseDetails(responseContext);
            if (responseContext.hasEntity()) {
                OutputStream stream = new LoggingStream(responseContext.getEntityStream());
                responseContext.setEntityStream(stream);
                requestContext.setProperty(LOGGINGSTREAM_PROPERTY, stream);
                requestContext.setProperty(RESPONSE_DETAILS, restResponse);
            } else {
                Long requestTime = (Long) requestContext.getProperty(REQUEST_TIME);
                RestRequestDetails restRequest = (RestRequestDetails) requestContext.getProperty(REQUEST_DETAILS);
                Map<String, String> restParams = (Map<String, String>) requestContext.getProperty(REST_PARAMS);
                sendStructuredEvent(restRequest, restResponse, restParams, requestTime, "");
            }
        }
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        context.proceed();
        if (BooleanUtils.isTrue((Boolean) context.getProperty(LOGGING_ENABLED_PROPERTY))) {
            Long requestTime = (Long) context.getProperty(REQUEST_TIME);
            RestRequestDetails restRequest = (RestRequestDetails) context.getProperty(REQUEST_DETAILS);
            RestResponseDetails restResponse = (RestResponseDetails) context.getProperty(RESPONSE_DETAILS);
            String responseBody = ((LoggingStream) context.getProperty(LOGGINGSTREAM_PROPERTY)).getStringBuilder(
                    MessageUtils.getCharset(context.getMediaType())).toString();
            Map<String, String> restParams = (Map<String, String>) context.getProperty(REST_PARAMS);
            if (restParams == null) {
                restParams = new HashMap<>();
            }
            extendRestParamsFromResponse(restParams, responseBody);
            sendStructuredEvent(restRequest, restResponse, restParams, requestTime, responseBody);
        }
    }

    private void sendStructuredEvent(RestRequestDetails restRequest, RestResponseDetails restResponse, Map<String, String> restParams, Long requestTime,
            String responseBody) {
        restResponse.setBody(responseBody);
        RestCallDetails restCall = new RestCallDetails();
        restCall.setRestRequest(restRequest);
        restCall.setRestResponse(restResponse);
        restCall.setDuration(System.currentTimeMillis() - requestTime);
        CloudbreakUser cloudbreakUser = cloudbreakRestRequestThreadLocalService.getCloudbreakUser();
        if (cloudbreakUser == null) {
            String serviceId = authenticatedUserService.getServiceAccountId();
            cloudbreakUser = new CloudbreakUser(serviceId, serviceId, serviceId, serviceId, serviceId);
        }
        structuredEventClient.sendStructuredEvent(new CDPStructuredRestCallEvent(createCDPOperationDetails(restParams, requestTime, cloudbreakUser),
                restCall));
    }

    private Map<String, String> getRequestUrlParameters(ContainerRequestContext requestContext) {
        Map<String, String> params = Maps.newHashMap();
        for (CDPRestUrlParser cdpRestUrlParser : cdpRestUrlParsers) {
            if (cdpRestUrlParser.fillParams(requestContext, params)) {
                //TODO
//                String workspaceId = params.get(CDPRestUrlParser.WORKSPACE_ID);
//                if (isResourceIdIsAbsentOrNull(params) && NumberUtils.isCreatable(workspaceId)) {
//                    putResourceIdFromRepository(requestContext, params, legacyRestUrlParser, workspaceId);
//                }
                break;
            }
        }
        return params;
    }

    private void putResourceIdFromRepository(ContainerRequestContext requestContext, Map<String, String> params,
            CDPRestUrlParser cdpRestUrlParser, String accountId) {
        for (Entry<String, AccountAwareResourceRepository<?, ?>> pathRepositoryEntry : pathRepositoryMap.entrySet()) {
            String pathWithWorkspaceId = pathRepositoryEntry.getKey().replaceFirst("\\{.*\\}", accountId);
            String requestUrl = cdpRestUrlParser.getUrl(requestContext);
            if (('/' + requestUrl).startsWith(pathWithWorkspaceId)) {
                AccountAwareResourceRepository<?, ?> resourceRepository = pathRepositoryEntry.getValue();
                String resourceName = params.get(CDPRestUrlParser.RESOURCE_NAME);
                if (resourceName != null) {
                    resourceRepository.findByNameAndAccountId(resourceName, accountId)
                            .ifPresent(resource -> params.put(CDPRestUrlParser.RESOURCE_ID, Long.toString(resource.getId())));
                }
                break;
            }
        }
    }

    protected void extendRestParamsFromResponse(Map<String, String> params, CharSequence responseBody) {
        boolean resourceIdIsAbsentOrNull = isResourceIdIsAbsentOrNull(params);
        boolean resourceCrnIsAbsentOrNull = isResourceCrnIsAbsentOrNull(params);
        if (responseBody != null && (resourceIdIsAbsentOrNull || resourceCrnIsAbsentOrNull)) {
            Set<String> resourcesParamsToCollect = new HashSet<>();
            decorateSetWithValueIfNecessary(resourceIdIsAbsentOrNull, ID, resourcesParamsToCollect);
            decorateSetWithValueIfNecessary(resourceCrnIsAbsentOrNull, CRN, resourcesParamsToCollect);
            Map<String, String> resourceParams = extractResourceValueFromJson(responseBody, resourcesParamsToCollect);
            extractResourceParamWithPattern(responseBody, resourceIdIsAbsentOrNull, resourceParams, ID, extractIdRestParamFromResponsePattern);
            extractResourceParamWithPattern(responseBody, resourceCrnIsAbsentOrNull, resourceParams, CRN, extractCrnRestParamFromResponsePattern);
            addExtractedValuesToParameters(params, resourceParams, ID, CDPRestUrlParser.RESOURCE_ID);
            addExtractedValuesToParameters(params, resourceParams, CRN, CDPRestUrlParser.RESOURCE_CRN);
        }
    }

    private void addExtractedValuesToParameters(Map<String, String> params, Map<String, String> resourceParams, String id, String resourceId) {
        if (resourceParams.containsKey(id)) {
            params.put(resourceId, resourceParams.get(id));
        }
    }

    private void extractResourceParamWithPattern(CharSequence responseBody, boolean resourceIdIsAbsentOrNull, Map<String, String> resourceParams, String key,
            Pattern pattern) {
        if (resourceIdIsAbsentOrNull && !resourceParams.containsKey(key)) {
            Matcher matcher = pattern.matcher(responseBody);
            if (matcher.find() && matcher.groupCount() >= 1) {
                resourceParams.put(key, matcher.group(1));
            }
        }
    }

    private void decorateSetWithValueIfNecessary(boolean decorate, String value, Set<String> set) {
        if (decorate) {
            set.add(value);
        }
    }

    private Map<String, String> extractResourceValueFromJson(CharSequence responseBody, Set<String> pathes) {
        Map<String, String> resourceMap = new HashMap<>(pathes.size());
        try {
            if (JsonUtil.isValid(responseBody.toString())) {
                JsonNode jsonNode = JsonUtil.readTree(responseBody.toString());
                for (String path : pathes) {
                    JsonNode idNode = jsonNode.path(path);
                    if (idNode.isMissingNode()) {
                        LOGGER.debug("Response was a JSON but no " + path + " available");
                    } else {
                        resourceMap.put(path, idNode.asText());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Json parsing failed for ", e);
        }
        return resourceMap;
    }

    private boolean isResourceIdIsAbsentOrNull(Map<String, String> params) {
        return isParameterAbsentOrNull(params, CDPRestUrlParser.RESOURCE_ID);
    }

    private boolean isParameterAbsentOrNull(Map<String, String> params, String parameter) {
        return !params.containsKey(parameter) || params.get(parameter) == null;
    }

    private boolean isResourceCrnIsAbsentOrNull(Map<String, String> params) {
        return isParameterAbsentOrNull(params, CDPRestUrlParser.RESOURCE_CRN);
    }

    private boolean isLoggingEnabled(ContainerRequestContext requestContext) {
        return !"GET".equals(requestContext.getMethod());
    }

    private CDPOperationDetails createCDPOperationDetails(Map<String, String> restParams, Long requestTime, CloudbreakUser cloudbreakUser) {
        String resourceType = null;
        String resourceId = null;
        String resourceName = null;
        String resourceCrn = null;
        String resourceEvent = null;
        if (restParams != null) {
            resourceType = restParams.get(CDPRestUrlParser.RESOURCE_TYPE);
            resourceId = restParams.get(CDPRestUrlParser.RESOURCE_ID);
            resourceName = restParams.get(CDPRestUrlParser.RESOURCE_NAME);
            resourceCrn = restParams.get(CDPRestUrlParser.RESOURCE_CRN);
            resourceEvent = restParams.get(CDPRestUrlParser.RESOURCE_EVENT);
        }
        return new CDPOperationDetails(requestTime,
                REST,
                resourceType,
                StringUtils.isNotEmpty(resourceId) ? Long.valueOf(resourceId) : null,
                resourceName,
                nodeConfig.getId(),
                cbVersion,
                cloudbreakUser.getTenant(),
                resourceCrn,
                cloudbreakUser.getUserCrn(),
                null,
                resourceEvent);

    }

    private RestRequestDetails createRequestDetails(ContainerRequestContext requestContext, String body) {
        LOGGER.debug("Request body length: {}", body.length());
        RestRequestDetails restRequest = new RestRequestDetails();
        restRequest.setRequestUri(requestContext.getUriInfo().getRequestUri().toString());
        restRequest.setBody(body);
        restRequest.setCookies(requestContext.getCookies().entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().toString())));
        restRequest.setHeaders(requestContext.getHeaders().entrySet().stream().filter(e -> !skippedHeadersList.contains(e.getKey())).collect(
                Collectors.toMap(Entry::getKey, e -> StringUtils.join(e.getValue(), ","))));
        MediaType mediaType = requestContext.getMediaType();
        restRequest.setMediaType(mediaType != null ? mediaType.toString() : "");
        restRequest.setMethod(requestContext.getMethod());
        return restRequest;
    }

    private RestResponseDetails createResponseDetails(ContainerResponseContext responseContext) {
        RestResponseDetails restResponse = new RestResponseDetails();
        restResponse.setStatusCode(responseContext.getStatus());
        restResponse.setStatusText(responseContext.getStatusInfo().toEnum().name());
        restResponse.setCookies(responseContext.getCookies().entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().toString())));
        restResponse.setHeaders(responseContext.getHeaders().entrySet().stream().filter(e -> !skippedHeadersList.contains(e.getKey())).collect(
                Collectors.toMap(Entry::getKey, e -> StringUtils.join(e.getValue(), ","))));
        MediaType mediaType = responseContext.getMediaType();
        restResponse.setMediaType(mediaType != null ? mediaType.toString() : "");
        return restResponse;
    }

    private InputStream logInboundEntity(StringBuilder content, InputStream stream, Charset charset) throws IOException {
        if (contentLogging) {
            if (!stream.markSupported()) {
                stream = new BufferedInputStream(stream);
            }
            stream.mark(MAX_CONTENT_LENGTH + 1);
            String entityString = IOUtils.toString(stream, charset);
            if (entityString.length() > MAX_CONTENT_LENGTH) {
                entityString = entityString.substring(0, MAX_CONTENT_LENGTH) + "...more...";
            }
            content.append(entityString);
            content.append('\n');
            stream.reset();
        }
        return stream;
    }

    private class LoggingStream extends FilterOutputStream {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        LoggingStream(OutputStream inner) {
            super(inner);
        }

        StringBuffer getStringBuilder(Charset charset) {
            StringBuffer b = new StringBuffer();
            if (contentLogging) {
                byte[] entity = baos.toByteArray();
                b.append(new String(entity, 0, Math.min(entity.length, MAX_CONTENT_LENGTH), charset));
                if (entity.length > MAX_CONTENT_LENGTH) {
                    b.append("...more...");
                }
                b.append('\n');
            }
            return b;
        }

        @Override
        public void write(int i) throws IOException {
            if (contentLogging && baos.size() <= MAX_CONTENT_LENGTH) {
                baos.write(i);
            }
            out.write(i);
        }
    }
}