package net.ripe.commons.provisioning.payload.list.request;

import net.ripe.certification.client.xml.XStreamXmlSerializer;
import net.ripe.commons.provisioning.payload.RelaxNgSchemaValidator;
import net.ripe.commons.provisioning.payload.list.request.ResourceClassListQueryPayload;
import net.ripe.commons.provisioning.payload.list.request.ResourceClassListQueryPayloadBuilder;
import net.ripe.commons.provisioning.payload.list.request.ResourceClassListQueryPayloadSerializerBuilder;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResourceClassListQueryPayloadBuilderTest {

    private static final XStreamXmlSerializer<ResourceClassListQueryPayload> SERIALIZER = new ResourceClassListQueryPayloadSerializerBuilder().build();
    private ResourceClassListQueryPayload payload;

    @Before
    public void given() {
        ResourceClassListQueryPayloadBuilder builder = new ResourceClassListQueryPayloadBuilder();
        payload = builder.build();
    }
    
    @Test
    public void shouldCreateParsableProvisioningObject() throws IOException {
        assertEquals("sender", payload.getSender());
        assertEquals("recipient", payload.getRecipient());
    }

    // http://tools.ietf.org/html/draft-ietf-sidr-rescerts-provisioning-09#section-3.3.1
    @Test
    public void shouldCreateXmlConformDraft() {
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
                "<message xmlns=\"http://www.apnic.net/specs/rescerts/up-down/\" version=\"1\" sender=\"sender\" recipient=\"recipient\" type=\"list\"/>";

        String actualXml = SERIALIZER.serialize(payload);
        assertEquals(expectedXml, actualXml);
    }

    @Test
    public void shouldProduceSchemaValidatedXml() throws SAXException, IOException {
        String actualXml = SERIALIZER.serialize(payload);
        assertTrue(RelaxNgSchemaValidator.validateAgainstRelaxNg(actualXml));
    }
}
