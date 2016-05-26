package com.sequenceiq.cloudbreak.core.flow2.cluster.sync;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncResult;

@Configuration
public class ClusterSyncActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSyncActions.class);

    @Bean(name = "CLUSTER_SYNC_STATE")
    public Action syncCluster() {
        return new AbstractClusterSyncAction<StackStatusUpdateContext>(StackStatusUpdateContext.class) {
            @Override
            protected void doExecute(ClusterSyncContext context, StackStatusUpdateContext payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterSyncContext context) {
                return new ClusterSyncRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_SYNC_FINISHED_STATE")
    public Action finishSyncCluster() {
        return new AbstractClusterSyncAction<ClusterSyncResult>(ClusterSyncResult.class) {
            @Override
            protected void doExecute(ClusterSyncContext context, ClusterSyncResult payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterSyncContext context) {
                return new StackEvent(ClusterSyncEvent.FINALIZED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_SYNC_FAILED_STATE")
    public Action clusterSyncFailedAction() {
        return new AbstractStackFailureAction<ClusterSyncState, ClusterSyncEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.warn("Error during executing cluster sync.", payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterSyncEvent.FAIL_HANDLED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }
}