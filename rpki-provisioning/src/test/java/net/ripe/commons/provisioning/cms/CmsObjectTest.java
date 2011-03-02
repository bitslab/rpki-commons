package net.ripe.commons.provisioning.cms;


import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.security.cert.X509Certificate;

import net.ripe.commons.provisioning.cms.CmsObject;

import org.bouncycastle.cms.CMSSignedGenerator;
import org.junit.Before;
import org.junit.Test;

public class CmsObjectTest {

    private CmsObject subject;


    @Before
    public void setUp() {
        subject = new CmsObject(new byte[] {'f', 'o', 'o'}, null);
    }

    @Test
    public void shouldDefineSyntaxVersion() {
        assertEquals(CmsObject.VERSION, 3);
    }

    @Test
    public void shouldDefineDigestAlgorithm() {
        assertEquals(CmsObject.DIGEST_ALGORITHM_OID, CMSSignedGenerator.DIGEST_SHA256);
    }

    @Test
    public void shouldDefineContentType() {
        assertEquals(CmsObject.CONTENT_TYPE, "1.2.840.113549.1.9.16.1.28");
    }

    @Test
    public void shouldCompareOnlyTheContentForEquality() {
        X509Certificate certificate1 = createMock(X509Certificate.class);
        X509Certificate certificate2 = createMock(X509Certificate.class);

        byte[] encodedContent = new byte[] {'f', 'o', 'o'};

        CmsObject cms1 = new CmsObject(encodedContent, certificate1);
        CmsObject cms2 = new CmsObject(encodedContent, certificate2);

        assertEquals(cms1, cms2);
    }

    @Test
    public void shouldUseOnlyTheContentForHashcode() {
        X509Certificate certificate1 = createMock(X509Certificate.class);
        X509Certificate certificate2 = createMock(X509Certificate.class);

        byte[] encodedContent = new byte[] {'f', 'o', 'o'};

        CmsObject cms1 = new CmsObject(encodedContent, certificate1);
        CmsObject cms2 = new CmsObject(encodedContent, certificate2);

        assertEquals(cms1.hashCode(), cms2.hashCode());
    }

}