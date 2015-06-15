package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.List;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class CloudFormationStackPollerObject extends StackContext {

    private AmazonCloudFormationClient cloudFormationClient;
    private StackStatus successStatus;
    private StackStatus errorStatus;
    private List<StackStatus> stackErrorStatuses;

    public CloudFormationStackPollerObject(AmazonCloudFormationClient cloudFormationClient, StackStatus successStatus, StackStatus errorStatus,
            List<StackStatus> stackErrorStatuses, Stack stack) {
        super(stack);
        this.cloudFormationClient = cloudFormationClient;
        this.successStatus = successStatus;
        this.errorStatus = errorStatus;
        this.stackErrorStatuses = stackErrorStatuses;
    }

    public AmazonCloudFormationClient getCloudFormationClient() {
        return cloudFormationClient;
    }

    public StackStatus getSuccessStatus() {
        return successStatus;
    }

    public StackStatus getErrorStatus() {
        return errorStatus;
    }

    public List<StackStatus> getStackErrorStatuses() {
        return stackErrorStatuses;
    }
}
