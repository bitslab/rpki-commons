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
package net.ripe.rpki.commons.crypto.cms.ghostbuster;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.property.Address;
import ezvcard.property.Email;
import ezvcard.property.FormattedName;
import ezvcard.property.Organization;
import ezvcard.property.Telephone;
import ezvcard.property.VCardProperty;
import net.ripe.rpki.commons.crypto.cms.RpkiSignedObjectInfo;
import net.ripe.rpki.commons.crypto.cms.RpkiSignedObjectParser;
import net.ripe.rpki.commons.validation.ValidationResult;
import net.ripe.rpki.commons.validation.ValidationString;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static net.ripe.rpki.commons.validation.ValidationString.GHOSTBUSTERS_RECORD_CONTENT_TYPE;
import static net.ripe.rpki.commons.validation.ValidationString.GHOSTBUSTERS_RECORD_SINGLE_VCARD;

public class GhostbustersCmsParser extends RpkiSignedObjectParser {

    private String vCardPayload;

    @Override
    public void parse(ValidationResult result, byte[] encoded) {
        super.parse(result, encoded);
        validateGhostbusters();
    }

    @Override
    public void decodeRawContent(InputStream content) throws IOException {
        vCardPayload = CharStreams.toString(new InputStreamReader(content, Charsets.UTF_8));
    }

    protected void validateGhostbusters() {
        ValidationResult validationResult = getValidationResult();

        if (!validationResult.rejectIfFalse(getContentType() != null, GHOSTBUSTERS_RECORD_CONTENT_TYPE)) {
            return;
        }
        if (!validationResult.rejectIfFalse(GhostbustersCms.CONTENT_TYPE.equals(getContentType()), GHOSTBUSTERS_RECORD_CONTENT_TYPE, getContentType().toString())) {
            return;
        }
        if (!validationResult.rejectIfNull(vCardPayload, GHOSTBUSTERS_RECORD_SINGLE_VCARD)) {
            return;
        }

        List<VCard> vCards = Ezvcard.parse(vCardPayload).all();
        validationResult.rejectIfFalse(vCards.size() == 1, GHOSTBUSTERS_RECORD_SINGLE_VCARD, String.valueOf(vCards.size()));
        if (validationResult.hasFailureForCurrentLocation()) {
            return;
        }

        VCard vCard = vCards.get(0);

        validationResult.rejectIfFalse(VCardVersion.V4_0 == vCard.getVersion(), ValidationString.GHOSTBUSTERS_RECORD_VCARD_VERSION, vCard.getVersion().getVersion());
        validationResult.rejectIfFalse(vCard.getFormattedName() != null && !Strings.isNullOrEmpty(vCard.getFormattedName().getValue()), ValidationString.GHOSTBUSTERS_RECORD_FN_PRESENT);
        validationResult.rejectIfTrue(
            vCard.getAddresses().isEmpty() &&
                vCard.getTelephoneNumbers().isEmpty() &&
                vCard.getEmails().isEmpty(),
            ValidationString.GHOSTBUSTERS_RECORD_ADR_TEL_OR_EMAIL_PRESENT
        );

        for (VCardProperty property: vCard) {
            validationResult.rejectIfFalse(
                property instanceof FormattedName ||
                    property instanceof Address ||
                    property instanceof Email ||
                    property instanceof Telephone ||
                    property instanceof Organization,
                ValidationString.GHOSTBUSTERS_RECORD_SUPPORTED_PROPERTY
            );
        }
    }

    public GhostbustersCms getGhostbustersCms() {
        if (!isSuccess()) {
            throw new IllegalArgumentException("Ghostbuster record validation failed: " + getValidationResult().getFailuresForCurrentLocation());
        }
        RpkiSignedObjectInfo cmsObjectData = new RpkiSignedObjectInfo(getEncoded(), getResourceCertificate(), getContentType(), getSigningTime());
        return new GhostbustersCms(cmsObjectData, vCardPayload);
    }

    public boolean isSuccess() {
        return !getValidationResult().hasFailureForCurrentLocation();
    }
}
