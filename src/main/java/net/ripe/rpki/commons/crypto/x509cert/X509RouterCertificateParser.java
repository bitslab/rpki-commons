/**
 * The BSD License
 *
 * Copyright (c) 2010-2012 RIPE NCC
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
package net.ripe.rpki.commons.crypto.x509cert;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import static net.ripe.rpki.commons.validation.ValidationString.AS_RESOURCE_PRESENT;
import static net.ripe.rpki.commons.validation.ValidationString.BGPSEC_EXT_PRESENT;
import static net.ripe.rpki.commons.validation.ValidationString.CERT_NO_SUBJECT_PK_INFO;
import static net.ripe.rpki.commons.validation.ValidationString.CERT_SIA_IS_PRESENT;
import static net.ripe.rpki.commons.validation.ValidationString.IP_RESOURCE_PRESENT;

public class X509RouterCertificateParser extends X509CertificateParser<X509RouterCertificate> {

    @Override
    public X509RouterCertificate getCertificate() {
        if (!isSuccess()) {
            throw new IllegalArgumentException("Router certificate validation failed");
        }
        return new X509RouterCertificate(getX509Certificate());
    }


    @Override
    protected void doTypeSpecificValidation() {
        result.rejectIfFalse(isBgpSecExtensionPresent(), BGPSEC_EXT_PRESENT);

        final X509CertificateInformationAccessDescriptor[] sia = getCertificate().getSubjectInformationAccess();
        result.rejectIfTrue(sia != null && sia.length > 0, CERT_SIA_IS_PRESENT);

        result.rejectIfTrue(isIpResourceExtensionPresent(), IP_RESOURCE_PRESENT);
        result.rejectIfFalse(isAsResourceExtensionPresent(), AS_RESOURCE_PRESENT);

        final SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(getX509Certificate().getPublicKey());
        result.rejectIfTrue(subjectPublicKeyInfo == null, CERT_NO_SUBJECT_PK_INFO);
    }
}
