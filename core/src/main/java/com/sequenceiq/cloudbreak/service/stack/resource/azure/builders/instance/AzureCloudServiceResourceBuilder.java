package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.instance;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureCloudServiceDeleteTask;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureCloudServiceDeleteTaskContext;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureCreateResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourcePollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;

import groovyx.net.http.HttpResponseDecorator;

@Component
@Order(1)
public class AzureCloudServiceResourceBuilder extends AzureSimpleInstanceResourceBuilder {

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private AzureCloudServiceDeleteTask azureCloudServiceDeleteTask;
    @Autowired
    private PollingService<AzureCloudServiceDeleteTaskContext> azureCloudServiceRemoveReadyPollerObjectPollingService;
    @Autowired
    private AzureStackUtil azureStackUtil;
    @Autowired
    private AzureCreateResourceStatusCheckerTask azureCreateResourceStatusCheckerTask;
    @Autowired
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;
    @Autowired
    private CloudbreakEventService eventService;

    @Override
    public Boolean create(final CreateResourceRequest createResourceRequest, String region) throws Exception {
        AzureCloudServiceCreateRequest aCSCR = (AzureCloudServiceCreateRequest) createResourceRequest;
        try {
            HttpResponseDecorator cloudServiceResponse = (HttpResponseDecorator) aCSCR.getAzureClient().createCloudService(aCSCR.getProps());
            AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(aCSCR.getAzureClient(), aCSCR.getStack(), cloudServiceResponse);
            azureResourcePollerObjectPollingService.pollWithTimeout(azureCreateResourceStatusCheckerTask, azureResourcePollerObject,
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS, MAX_FAILURE_COUNT);
        } catch (Exception ex) {
            throw checkException(ex);
        }
        return true;
    }

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject deleteContextObject, String region) throws Exception {
        Stack stack = stackRepository.findById(deleteContextObject.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureCloudServiceDeleteTaskContext azureCloudServiceDeleteTaskContext = new AzureCloudServiceDeleteTaskContext(resource.getResourceName(),
                stack, azureStackUtil.createAzureClient(credential));
        azureCloudServiceRemoveReadyPollerObjectPollingService
                .pollWithTimeout(azureCloudServiceDeleteTask, azureCloudServiceDeleteTaskContext, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS, MAX_FAILURE_COUNT);
        return true;
    }

    @Override
    public List<Resource> buildResources(AzureProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        String vmName = getVmName(provisionContextObject.filterResourcesByType(ResourceType.AZURE_NETWORK).get(0).getResourceName(), index);
        String newVmName = vmName + String.valueOf(new Date().getTime());
        if (newVmName.length() > MAX_NAME_LENGTH) {
            newVmName = newVmName.substring(newVmName.length() - MAX_NAME_LENGTH, newVmName.length());
        }
        return Arrays.asList(new Resource(resourceType(), newVmName, stack, instanceGroup.orNull().getGroupName()));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(AzureProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        AzureTemplate azureTemplate = (AzureTemplate) instanceGroup.orNull().getTemplate();
        String vmName = buildResources.get(0).getResourceName();
        Map<String, String> props = new HashMap<>();
        props.put(NAME, vmName);
        props.put(DESCRIPTION, azureTemplate.getDescription());
        props.put(AFFINITYGROUP, provisionContextObject.getAffinityGroupName());
        return new AzureCloudServiceCreateRequest(props, resources, buildResources, stack, instanceGroup.orNull());
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_CLOUD_SERVICE;
    }

    public class AzureCloudServiceCreateRequest extends CreateResourceRequest {
        private Map<String, String> props = new HashMap<>();
        private List<Resource> resources;
        private Stack stack;
        private InstanceGroup instanceGroup;

        public AzureCloudServiceCreateRequest(Map<String, String> props, List<Resource> resources, List<Resource> buildNames,
                Stack stack, InstanceGroup instanceGroup) {
            super(buildNames);
            this.props = props;
            this.resources = resources;
            this.stack = stack;
            this.instanceGroup = instanceGroup;
        }

        public Stack getStack() {
            return stack;
        }

        public Map<String, String> getProps() {
            return props;
        }

        public AzureClient getAzureClient() {
            return azureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        }

        public List<Resource> getResources() {
            return resources;
        }

        public InstanceGroup getInstanceGroup() {
            return instanceGroup;
        }
    }
}