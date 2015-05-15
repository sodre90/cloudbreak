package com.sequenceiq.it.cloudbreak;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class GcpTemplateCreationTest extends AbstractCloudbreakIntegrationTest {
    @Autowired
    private TemplateAdditionHelper additionHelper;

    private List<TemplateAddition> additions;

    @BeforeMethod
    @Parameters({ "templateAdditions" })
    public void setup(@Optional("master,1;slave_1,3") String templateAdditions) {
        additions = additionHelper.parseTemplateAdditions(templateAdditions);
    }

    @Test
    @Parameters({ "gcpName", "gcpInstanceType", "volumeType", "volumeCount", "volumeSize" })
    public void testGcpTemplateCreation(@Optional("it-gcp-template") String gcpName, @Optional("N1_STANDARD_2") String gcpInstanceType,
            @Optional("HDD") String volumeType, @Optional("1") String volumeCount, @Optional("30") String volumeSize) throws Exception {
        // GIVEN
        // WHEN
        // TODO: publicInAccount
        String id = getClient().postGccTemplate(gcpName, "GCP template for integration testing", gcpInstanceType, volumeType, volumeCount, volumeSize, false);
        // THEN
        Assert.assertNotNull(id);
        additionHelper.handleTemplateAdditions(getItContext(), id, additions);
    }
}