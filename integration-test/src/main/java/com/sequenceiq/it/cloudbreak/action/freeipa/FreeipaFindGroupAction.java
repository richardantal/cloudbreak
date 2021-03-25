package com.sequenceiq.it.cloudbreak.action.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackForceDeleteAction;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class FreeipaFindGroupAction implements Action<FreeIpaTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackForceDeleteAction.class);

    @Override
    public FreeIpaTestDto action(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        com.sequenceiq.freeipa.client.FreeIpaClient ipaServerClient = testDto.createIpaServerClient();
        String group = "admins";
        if (ipaServerClient.groupFindAll().contains(group)) {
            LOGGER.info("Group {} found in Freeipa", group);
        } else {
            throw new TestFailException(String.format("Group %s cannot be found in freeipa", group));
        }
        return testDto;
    }
}
