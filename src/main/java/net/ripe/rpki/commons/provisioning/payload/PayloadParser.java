package net.ripe.rpki.commons.provisioning.payload;

import net.ripe.rpki.commons.provisioning.payload.error.RequestNotPerformedResponsePayload;
import net.ripe.rpki.commons.provisioning.payload.error.RequestNotPerformedResponsePayloadSerializer;
import net.ripe.rpki.commons.provisioning.payload.issue.request.CertificateIssuanceRequestPayload;
import net.ripe.rpki.commons.provisioning.payload.issue.request.CertificateIssuanceRequestPayloadSerializer;
import net.ripe.rpki.commons.provisioning.payload.issue.response.CertificateIssuanceResponsePayload;
import net.ripe.rpki.commons.provisioning.payload.issue.response.CertificateIssuanceResponsePayloadSerializer;
import net.ripe.rpki.commons.provisioning.payload.list.request.ResourceClassListQueryPayload;
import net.ripe.rpki.commons.provisioning.payload.list.request.ResourceClassListQueryPayloadSerializer;
import net.ripe.rpki.commons.provisioning.payload.list.response.ResourceClassListResponsePayload;
import net.ripe.rpki.commons.provisioning.payload.list.response.ResourceClassListResponsePayloadSerializer;
import net.ripe.rpki.commons.provisioning.payload.revocation.request.CertificateRevocationRequestPayload;
import net.ripe.rpki.commons.provisioning.payload.revocation.request.CertificateRevocationRequestPayloadSerializer;
import net.ripe.rpki.commons.provisioning.payload.revocation.response.CertificateRevocationResponsePayload;
import net.ripe.rpki.commons.provisioning.payload.revocation.response.CertificateRevocationResponsePayloadSerializer;
import net.ripe.rpki.commons.validation.ValidationResult;
import net.ripe.rpki.commons.validation.ValidationString;
import net.ripe.rpki.commons.xml.XmlSerializer;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.ripe.rpki.commons.validation.ValidationString.VALID_PAYLOAD_TYPE;


public final class PayloadParser {

    private static final Pattern TYPE_PATTERN = Pattern.compile(".*<message[^>]*type=['\"]([a-z|\\_]*)['\"].*", Pattern.DOTALL);

    private static final XmlSerializer<ResourceClassListResponsePayload> LIST_RESPONSE_SERIALIZER = new ResourceClassListResponsePayloadSerializer();
    private static final XmlSerializer<ResourceClassListQueryPayload> LIST_SERIALIZER = new ResourceClassListQueryPayloadSerializer();
    private static final XmlSerializer<CertificateIssuanceRequestPayload> ISSUE_SERIALIZER = new CertificateIssuanceRequestPayloadSerializer();
    private static final XmlSerializer<CertificateIssuanceResponsePayload> ISSUE_RESPONSE_SERIALIZER = new CertificateIssuanceResponsePayloadSerializer();
    private static final XmlSerializer<CertificateRevocationRequestPayload> REVOKE_SERIALIZER = new CertificateRevocationRequestPayloadSerializer();
    private static final XmlSerializer<CertificateRevocationResponsePayload> REVOKE_RESPONSE_SERIALIZER = new CertificateRevocationResponsePayloadSerializer();
    private static final XmlSerializer<RequestNotPerformedResponsePayload> ERROR_RESPONSE_SERIALIZER = new RequestNotPerformedResponsePayloadSerializer();

    private static final Map<PayloadMessageType, XmlSerializer<? extends AbstractProvisioningPayload>> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put(PayloadMessageType.list, LIST_SERIALIZER);
        TYPE_MAP.put(PayloadMessageType.list_response, LIST_RESPONSE_SERIALIZER);
        TYPE_MAP.put(PayloadMessageType.issue, ISSUE_SERIALIZER);
        TYPE_MAP.put(PayloadMessageType.issue_response, ISSUE_RESPONSE_SERIALIZER);
        TYPE_MAP.put(PayloadMessageType.revoke, REVOKE_SERIALIZER);
        TYPE_MAP.put(PayloadMessageType.revoke_response, REVOKE_RESPONSE_SERIALIZER);
        TYPE_MAP.put(PayloadMessageType.error_response, ERROR_RESPONSE_SERIALIZER);
    }

    private PayloadParser() {
    }

    public static AbstractProvisioningPayload parse(String payloadXml, ValidationResult validationResult) {
        Matcher matcher = TYPE_PATTERN.matcher(payloadXml);
        validationResult.rejectIfFalse(matcher.matches(), ValidationString.FOUND_PAYLOAD_TYPE);
        if (validationResult.hasFailures()) {
            return null;
        }

        String type = matcher.group(1);
        validationResult.rejectIfFalse(PayloadMessageType.containsAsEnum(type), VALID_PAYLOAD_TYPE);
        if (validationResult.hasFailures()) {
            return null;
        }

        PayloadMessageType messageType = PayloadMessageType.valueOf(type);
        XmlSerializer<? extends AbstractProvisioningPayload> serializer = TYPE_MAP.get(messageType);
        AbstractProvisioningPayload payload = serializer.deserialize(payloadXml);
        validationResult.rejectIfFalse(AbstractProvisioningPayload.SUPPORTED_VERSION.equals(payload.getVersion()), ValidationString.VALID_PAYLOAD_VERSION);
        if (validationResult.hasFailures()) {
            return null;
        }

        return payload;
    }

    public static String serialize(AbstractProvisioningPayload payload) {
        PayloadMessageType type = payload.getType();
        switch (type) {
            case list:
                return LIST_SERIALIZER.serialize((ResourceClassListQueryPayload) payload);
            case list_response:
                return LIST_RESPONSE_SERIALIZER.serialize((ResourceClassListResponsePayload) payload);
            case issue:
                return ISSUE_SERIALIZER.serialize((CertificateIssuanceRequestPayload) payload);
            case issue_response:
                return ISSUE_RESPONSE_SERIALIZER.serialize((CertificateIssuanceResponsePayload) payload);
            case revoke:
                return REVOKE_SERIALIZER.serialize((CertificateRevocationRequestPayload) payload);
            case revoke_response:
                return REVOKE_RESPONSE_SERIALIZER.serialize((CertificateRevocationResponsePayload) payload);
            case error_response:
                return ERROR_RESPONSE_SERIALIZER.serialize((RequestNotPerformedResponsePayload) payload);
            default:
                throw new NotImplementedException("Don't have serializer for PayloadMessageType: " + type);
        }
    }
}
