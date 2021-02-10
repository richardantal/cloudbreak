package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup;

import com.sequenceiq.flow.core.FlowState;

public enum DatalakeRecoveryBringupState implements FlowState {
    INIT_STATE,
    RECOVERY_BRINGUP_STATE,
    RECOVERY_BRINGUP_FAILED_STATE,
    RECOVERY_BRINGUP_FINISHED_STATE,
    FINAL_STATE;
}
