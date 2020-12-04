/**
 * The BSD License
 *
 * Copyright (c) 2010-2020 RIPE NCC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of the RIPE NCC nor the names of its contributors may be
 *     used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.ripe.rpki.commons.provisioning.identity;

import net.ripe.rpki.commons.provisioning.x509.ProvisioningIdentityCertificate;
import net.ripe.rpki.commons.provisioning.x509.ProvisioningIdentityCertificateParser;
import net.ripe.rpki.commons.validation.ValidationResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;


import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Base64;
import java.util.Optional;


public abstract class IdentitySerializer<T> {

    public static final String XMLNS = "http://www.hactrn.net/uris/rpki/rpki-setup/";

    public abstract T deserialize(String xml);

    public abstract String serialize(T object);

    protected DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        final DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        documentFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        documentFactory.setNamespaceAware(true);

        final DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

        return documentBuilder;
    }

    protected Optional<String> getAttributeValue(final Node node, final String attr) {
        return Optional.ofNullable(node.getAttributes())
                .map(a -> a.getNamedItem(attr))
                .map(item->item.getTextContent());
    }

    protected Optional<Node> getElement(Document doc, String elementName) {
        final Node node = doc.getElementsByTagNameNS(XMLNS, elementName).item(0);
        return Optional.ofNullable(node);
    }

    protected Optional<String> getBpkiElementContent(final Document doc, final String nodeName) {
        return getElement(doc, nodeName).map(e -> e.getTextContent().replaceAll("\\s+", ""));
    }

    protected String serialize(final Document document) throws TransformerException {
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();

        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        final StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(sw));

        return sw.toString();
    }

    protected ProvisioningIdentityCertificate getProvisioningIdentityCertificate(final String bpkiTa) {
        final ProvisioningIdentityCertificateParser parser = new ProvisioningIdentityCertificateParser();
        parser.parse(ValidationResult.withLocation("unknown.cer"), Base64.getDecoder().decode(bpkiTa));
        return parser.getCertificate();
    }

    public static class IdentitySerializerException extends RuntimeException {
        public IdentitySerializerException(Exception e) {
            super(e);
        }

        public IdentitySerializerException(final String message) {
            super(message);
        }

        public IdentitySerializerException(final String message, final Exception e) {
            super(message, e);
        }
    }
}
