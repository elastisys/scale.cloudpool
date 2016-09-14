package com.elastisys.scale.cloudpool.kubernetes.lab;

import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.apache.http.client.methods.HttpGet;

import com.elastisys.scale.commons.net.http.Http;
import com.elastisys.scale.commons.net.http.HttpRequestResponse;
import com.elastisys.scale.commons.security.pem.PemUtils;

public class Main {

    public static void main(String[] args) throws Exception {
        File certFile = new File("/tmp/ssl/admin.pem");
        File keyFile = new File("/tmp/ssl/admin-key.pem");

        PrivateKey privateKey = PemUtils.parseRsaPrivateKey(keyFile);

        // parse cert
        X509Certificate cert = PemUtils.parseX509Cert(certFile);

        System.out.println("cert: " + cert);
        System.out.println("key: " + privateKey);

        KeyStore keyStore = PemUtils.keyStoreFromCertAndKey(cert, privateKey, "secret");

        Http http = Http.builder().clientCertAuth(keyStore, "secret").verifyHostname(false).verifyHostCert(false)
                .build();
        HttpRequestResponse response = http.execute(
                new HttpGet("https://172.17.4.101:443/api/v1/namespaces/default/replicationcontrollers/nginx"));

        System.out.println(response);
        System.out.println(response.getResponseBody());
    }
}
