package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.polling.SimpleStatusCheckerTask;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.experience.ExperienceCluster;
import com.sequenceiq.environment.experience.ExperienceConnectorService;

public class ExperienceDeletionRetrievalTask extends SimpleStatusCheckerTask<ExperiencePollerObject> {

    public static final int EXPERIENCE_RETRYING_INTERVAL = 5000;

    public static final int EXPERIENCE_RETRYING_COUNT = 900;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperienceDeletionRetrievalTask.class);

    private static final String DELETE_FAILED_STATUS = "DELETE_FAILED";

    private final ExperienceConnectorService experienceConnectorService;

    public ExperienceDeletionRetrievalTask(ExperienceConnectorService experienceConnectorService) {
        this.experienceConnectorService = experienceConnectorService;
    }

    @Override
    public boolean checkStatus(ExperiencePollerObject pollerObject) {
        EnvironmentExperienceDto dto = buildDto(pollerObject);
        int quantity = experienceConnectorService.getConnectedExperienceCount(dto);
        if (quantity == 0) {
            LOGGER.info("No active experience has been found for the environment (name: {}, crn: {})", pollerObject.getEnvironmentName(),
                    pollerObject.getEnvironmentCrn());
            return true;
        }
        LOGGER.info(quantity + " experience has found for the environment (name: {}, crn: {})",
                pollerObject.getEnvironmentName(), pollerObject.getEnvironmentCrn());
        return false;
    }

    @Override
    public void handleTimeout(ExperiencePollerObject experiencePollerObject) {
        LOGGER.debug("Timeout handler passthrough, {}", experiencePollerObject);
    }

    @Override
    public void handleException(Exception e) {
        LOGGER.debug("Exception handler passthrough", e);
    }

    @Override
    public String successMessage(ExperiencePollerObject experiencePollerObject) {
        return "Experience deletion was successful!";
    }

    @Override
    public boolean exitPolling(ExperiencePollerObject experiencePollerObject) {
        EnvironmentExperienceDto dto = buildDto(experiencePollerObject);
        Set<ExperienceCluster> connectedExperiences = experienceConnectorService.getConnectedExperiences(dto);
        return connectedExperiences.stream().anyMatch(c -> DELETE_FAILED_STATUS.equals(c.getStatus()));
    }

    @Override
    public boolean initialExitCheck(ExperiencePollerObject experiencePollerObject) {
        return false;
    }

    private EnvironmentExperienceDto buildDto(ExperiencePollerObject pollerObject) {
        return new EnvironmentExperienceDto.Builder()
                .withName(pollerObject.getEnvironmentName())
                .withCrn(pollerObject.getEnvironmentCrn())
                .withAccountId(pollerObject.getAccountId()).build();
    }

}
