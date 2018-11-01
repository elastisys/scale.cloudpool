package com.elastisys.scale.cloudpool.kubernetes.config.kubeconfig;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.elastisys.scale.commons.util.io.IoUtils;

/**
 * Exercise {@link Cluster} and its validation.
 */
public class TestCluster {
    /** Path to CA cert. */
    public static final String CA_CERT_PATH = "src/test/resources/ssl/ca.pem";

    /**
     * It should be possible to specify a ca cert as a file system path.
     */
    @Test
    public void caCertFromFile() {
        Cluster cluster = clusterCaCertPath("https://apiserver", CA_CERT_PATH);
        cluster.validate();

        assertThat(cluster.getServer(), is("https://apiserver"));
        assertThat(cluster.getCertificateAuthorityPath(), is(CA_CERT_PATH));
        assertThat(cluster.getCertificateAuthorityData(), is(nullValue()));
        assertThat(cluster.getInsecureSkipTlsVerify(), is(false));
    }

    /**
     * It should be possible to specify a ca cert as base64-encoded data.
     */
    @Test
    public void caCertFromData() {
        Cluster cluster = clusterCaCertData("https://apiserver",
                IoUtils.toString("ssl/ca.pem.base64", StandardCharsets.UTF_8));
        cluster.validate();

        assertThat(cluster.getServer(), is("https://apiserver"));
        assertThat(cluster.getCertificateAuthorityPath(), is(nullValue()));
        assertThat(cluster.getCertificateAuthorityData(),
                is(IoUtils.toString("ssl/ca.pem.base64", StandardCharsets.UTF_8)));
        assertThat(cluster.getInsecureSkipTlsVerify(), is(false));
    }

    /**
     * It should be possible to skip server cert verification altogether via
     * insecure-skip-tls-verify.
     */
    @Test
    public void insecureSkipTlsVerify() {
        Cluster cluster = clusterSkipTlsVerify("https://apiserver");
        cluster.validate();

        assertThat(cluster.getServer(), is("https://apiserver"));
        assertThat(cluster.getCertificateAuthorityPath(), is(nullValue()));
        assertThat(cluster.getCertificateAuthorityData(), is(nullValue()));
        assertThat(cluster.getInsecureSkipTlsVerify(), is(true));
    }

    /**
     * Specifying both a CA cert and insecure-skip-tls-verify is ambiguous.
     */
    @Test
    public void caCertIncompatibleWithInsecureSkipTlsVerify() {
        try {
            Cluster cluster = clusterCaCertDataAndInsecureSkipTlsVerify("https://apiserver",
                    IoUtils.toString("ssl/ca.pem.base64", StandardCharsets.UTF_8));
            cluster.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("ambiguous input"));
        }

        try {
            Cluster cluster = clusterCaCertPathAndInsecureSkipTlsVerify("https://apiserver", CA_CERT_PATH);
            cluster.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("ambiguous input"));
        }
    }

    /**
     * Specifying both a CA cert and insecure-skip-tls-verify is ambiguous.
     */
    @Test
    public void caCert() {
        try {
            Cluster cluster = clusterCaCertDataAndInsecureSkipTlsVerify("https://apiserver",
                    IoUtils.toString("ssl/ca.pem.base64", StandardCharsets.UTF_8));
            cluster.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("ambiguous input"));
        }

        try {
            Cluster cluster = clusterCaCertPathAndInsecureSkipTlsVerify("https://apiserver", CA_CERT_PATH);
            cluster.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("ambiguous input"));
        }
    }

    /**
     * {@link Cluster} should validate that the specified certificate-authority
     * path exists.
     */
    @Test
    public void onBadCertPath() {
        Cluster cluster = clusterCaCertPath("https://apiserver", "bad/path.pem");
        try {
            cluster.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("No such file or directory"));
        }
    }

    /**
     * {@link Cluster} should validate that the specified certificate-authority
     * path is a valid cert.
     */
    @Test
    public void onBadCertFileContent() {
        Cluster cluster = clusterCaCertPath("https://apiserver", "src/test/resources/ssl/auth-token");
        try {
            cluster.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("failed to load ca cert"));
        }
    }

    /**
     * {@link Cluster} should validate that the specified
     * certificate-authority-data is a valid cert.
     */
    @Test
    public void onBadCertData() {
        String base64CaCertData = IoUtils.toString("ssl/ca.pem", StandardCharsets.UTF_8);
        base64CaCertData = base64CaCertData.substring(1, base64CaCertData.length());
        Cluster cluster = clusterCaCertData("https://apiserver", base64CaCertData);
        try {
            cluster.validate();
            fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("failed to load ca cert"));
        }
    }

    public static Cluster clusterCaCertPath(String apiServerUrl, String caCertPath) {
        Cluster cluster = new Cluster();
        cluster.server = apiServerUrl;
        cluster.certificateAuthorityPath = caCertPath;
        return cluster;
    }

    public static Cluster clusterCaCertData(String apiServerUrl, String base64CaCertData) {
        Cluster cluster = new Cluster();
        cluster.server = apiServerUrl;
        cluster.certificateAuthorityData = base64CaCertData;
        return cluster;
    }

    public static Cluster clusterSkipTlsVerify(String apiServerUrl) {
        Cluster cluster = new Cluster();
        cluster.server = apiServerUrl;
        cluster.insecureSkipTlsVerify = true;
        return cluster;
    }

    public static Cluster clusterCaCertDataAndInsecureSkipTlsVerify(String apiServerUrl, String base64CaCertData) {
        Cluster cluster = clusterCaCertData(apiServerUrl, base64CaCertData);
        cluster.insecureSkipTlsVerify = true;
        return cluster;
    }

    public static Cluster clusterCaCertPathAndInsecureSkipTlsVerify(String apiServerUrl, String caCertPath) {
        Cluster cluster = clusterCaCertPath(apiServerUrl, caCertPath);
        cluster.insecureSkipTlsVerify = true;
        return cluster;
    }

}
