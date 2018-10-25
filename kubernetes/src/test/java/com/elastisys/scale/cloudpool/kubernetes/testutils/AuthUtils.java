package com.elastisys.scale.cloudpool.kubernetes.testutils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import com.elastisys.scale.commons.security.pem.PemUtils;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.elastisys.scale.commons.util.io.IoUtils;

public class AuthUtils {
    public static String loadString(String filePath) {
        return IoUtils.toString(new File(filePath), StandardCharsets.UTF_8);
    }

    public static Certificate loadCert(String filePath) throws CertificateException, IOException {
        return PemUtils.parseX509Cert(new File(filePath));
    }

    public static PrivateKey loadKey(String filePath) throws FileNotFoundException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeySpecException, IOException {
        return PemUtils.parseRsaPrivateKey(new File(filePath));
    }

    public static String loadBase64(String filePath) {
        return Base64Utils.toBase64(loadString(filePath));
    }
}
