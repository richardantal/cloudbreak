package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkArgument;
import static com.sequenceiq.cloudbreak.auth.altus.Crn.ResourceType.MACHINE_USER;
import static com.sequenceiq.cloudbreak.auth.altus.Crn.ResourceType.USER;
import static com.sequenceiq.cloudbreak.auth.altus.Crn.Service.IAM;

import java.util.Objects;

public class Crns {

    private Crns() {
    }

    public static Crn ofUser(String value) {
        Crn crn = Crn.fromString(value);
        checkArgument(!Objects.isNull(crn)
                && IAM.equals(crn.getService())
                && (USER.equals(crn.getResourceType())
                || MACHINE_USER.equals(crn.getResourceType())), value + " is not a valid user crn");
        return crn;
    }
}
