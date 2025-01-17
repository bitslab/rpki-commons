package net.ripe.rpki.commons.crypto.cms.aspa;

import com.google.common.collect.ImmutableSortedSet;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.When;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import net.ripe.ipresource.Asn;
import net.ripe.ipresource.IpResourceSet;
import net.ripe.rpki.commons.crypto.ValidityPeriod;
import net.ripe.rpki.commons.crypto.rfc3779.AddressFamily;
import net.ripe.rpki.commons.crypto.util.KeyPairFactoryTest;
import net.ripe.rpki.commons.crypto.x509cert.X509CertificateInformationAccessDescriptor;
import net.ripe.rpki.commons.crypto.x509cert.X509ResourceCertificate;
import net.ripe.rpki.commons.crypto.x509cert.X509ResourceCertificateBuilder;
import net.ripe.rpki.commons.util.UTC;
import net.ripe.rpki.commons.validation.ValidationResult;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyPair;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static net.ripe.rpki.commons.crypto.x509cert.X509CertificateBuilderHelper.DEFAULT_SIGNATURE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;

@RunWith(JUnitQuickcheck.class)
public class AspaCmsTest {


    private static final KeyPair TEST_KEY_PAIR = KeyPairFactoryTest.TEST_KEY_PAIR;
    private static final X500Principal TEST_DN = new X500Principal("CN=issuer");
    private static final URI TEST_LOCATION = URI.create("rsync://certificate/repository/filename.asa");
    private static final URI TEST_CA_LOCATION = URI.create("rsync://certificate/repository/ca.cer");
    private static final BigInteger ROA_CERT_SERIAL = BigInteger.TEN;

    private static final ImmutableSortedSet<ProviderAS> PROVIDER_AS_SET = ImmutableSortedSet.<ProviderAS>naturalOrder()
        .add(new ProviderAS(Asn.parse("AS65001"), Optional.empty()))
        .add(new ProviderAS(Asn.parse("AS65002"), Optional.of(AddressFamily.IPV4)))
        .build();

    private static final Asn CUSTOMER_ASN = Asn.parse("AS65000");


    @Test
    public void should_parse_aspa_1() throws IOException {
        String path = "src/test/resources/conformance/root/aspa-bm.asa";
        AspaCms aspa = parseValidAspa(path);
        assertEquals(Asn.parse("AS65000"), aspa.getCustomerAsn());
        assertEquals(
            ImmutableSortedSet.<ProviderAS>naturalOrder()
                .add(new ProviderAS(Asn.parse("AS65001"), Optional.empty()))
                .add(new ProviderAS(Asn.parse("AS65002"), Optional.of(AddressFamily.IPV4)))
                .build(),
            aspa.getProviderASSet()
        );
    }

    @Test
    public void should_parse_aspa_2() throws IOException {
        String path = "src/test/resources/conformance/root/AS211321.asa";
        AspaCms aspa = parseValidAspa(path);
        assertEquals(Asn.parse("AS211321"), aspa.getCustomerAsn());
        assertEquals(
            ImmutableSortedSet.<ProviderAS>naturalOrder()
                .add(new ProviderAS(Asn.parse("AS65000"), Optional.empty()))
                .add(new ProviderAS(Asn.parse("AS65001"), Optional.of(AddressFamily.IPV4)))
                .add(new ProviderAS(Asn.parse("AS65002"), Optional.of(AddressFamily.IPV6)))
                .build(),
            aspa.getProviderASSet()
        );
    }

    private AspaCms parseValidAspa(String path) throws IOException {
        byte[] bytes = FileUtils.readFileToByteArray(new File(path));
        ValidationResult result = ValidationResult.withLocation(path);
        AspaCmsParser parser = new AspaCmsParser();
        parser.parse(result, bytes);

        assertFalse("" + result.getFailuresForAllLocations(), result.hasFailures());
        AspaCms aspa = parser.getAspa();
        return aspa;
    }

    @Test
    public void should_reject_creating_aspa_with_empty_provider_as_set() {
        assertThatThrownBy(() -> createAspa(CUSTOMER_ASN, ImmutableSortedSet.of()))
            .isInstanceOfSatisfying(
                IllegalArgumentException.class,
                (e) -> assertThat(e.getMessage()).isEqualTo("ProviderASSet must not be empty")
            );
    }

    @Property(trials = 100)
    public void should_generate_aspa(int customerAsId, @When(satisfies = "!#_.isEmpty") List<ProviderAS> providerAsIdSet) {
        Asn customerAsn = new Asn(Integer.toUnsignedLong(customerAsId));
        ImmutableSortedSet<ProviderAS> providerAsSet = providerAsIdSet.stream()
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
        AspaCms cms = createAspa(customerAsn, providerAsSet);
        assertEquals(0, cms.getVersion());
        assertEquals(customerAsn, cms.getCustomerAsn());
        assertEquals(providerAsSet, cms.getProviderASSet());
    }

    public static AspaCms createAspa() {
        return createAspa(CUSTOMER_ASN, PROVIDER_AS_SET);
    }

    public static AspaCms createAspa(Asn customerAsn, ImmutableSortedSet<ProviderAS> providerAsSet) {
        AspaCmsBuilder builder = new AspaCmsBuilder();
        builder.withCertificate(createCertificate(new IpResourceSet(customerAsn)));
        builder.withCustomerAsn(customerAsn);
        builder.withProviderASSet(
            providerAsSet
        );
        builder.withSignatureProvider(DEFAULT_SIGNATURE_PROVIDER);
        return builder.build(TEST_KEY_PAIR.getPrivate());
    }

    private static X509ResourceCertificate createCertificate(IpResourceSet resources) {
        X509ResourceCertificateBuilder builder = new X509ResourceCertificateBuilder();
        builder.withCa(false).withIssuerDN(TEST_DN).withSubjectDN(TEST_DN).withSerial(ROA_CERT_SERIAL);
        builder.withPublicKey(TEST_KEY_PAIR.getPublic());
        builder.withSigningKeyPair(TEST_KEY_PAIR);
        final DateTime now = UTC.dateTime();
        builder.withKeyUsage(KeyUsage.digitalSignature);
        builder.withValidityPeriod(new ValidityPeriod(now.minusMinutes(1), now.plusYears(1)));
        builder.withResources(resources);
        builder.withSubjectInformationAccess(new X509CertificateInformationAccessDescriptor(X509CertificateInformationAccessDescriptor.ID_AD_SIGNED_OBJECT, TEST_LOCATION));
        builder.withAuthorityInformationAccess(new X509CertificateInformationAccessDescriptor(X509CertificateInformationAccessDescriptor.ID_CA_CA_ISSUERS, TEST_CA_LOCATION));
        return builder.build();
    }

}
