package com.elastisys.scale.cloudpool.commons.basepool.config;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPoolException;
import com.elastisys.scale.cloudpool.commons.scaledown.VictimSelectionPolicy;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.elastisys.scale.commons.net.alerter.http.HttpAlerterConfig;
import com.elastisys.scale.commons.net.alerter.http.HttpAuthConfig;
import com.elastisys.scale.commons.net.alerter.multiplexing.AlertersConfig;
import com.elastisys.scale.commons.net.alerter.smtp.SmtpAlerterConfig;
import com.elastisys.scale.commons.net.smtp.SmtpClientAuthentication;
import com.elastisys.scale.commons.net.smtp.SmtpClientConfig;
import com.elastisys.scale.commons.net.ssl.BasicCredentials;
import com.elastisys.scale.commons.util.base64.Base64Utils;
import com.google.gson.JsonObject;

/**
 * Tests validation of {@link BaseCloudPoolConfig}s. This involves validating
 * {@link BaseCloudPoolConfig}s with missing fields, which may for example occur
 * when deserializing from JSON.
 */
public class TestBaseCloudPoolConfigValidation {

    @Test
    public void minimalConfig() throws CloudPoolException {
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), null, null, null).validate();
    }

    @Test
    public void withAlertConfig() throws CloudPoolException {
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alertsConfig(), null, null)
                .validate();
    }

    @Test
    public void withPoolUpdate() throws CloudPoolException {
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alertsConfig(), null,
                poolUpdate()).validate();
    }

    @Test
    public void withPoolFetch() throws CloudPoolException {
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alertsConfig(), poolFetch(),
                poolUpdate()).validate();
    }

    // illegal config: missing /cloudPool
    @Test(expected = IllegalArgumentException.class)
    public void missingCloudPool() throws CloudPoolException {
        new BaseCloudPoolConfig(null, scaleOutConfig(), scaleInConfig(), alertsConfig(), null, null).validate();
    }

    // illegal config: missing /cloudPool/name
    @Test(expected = IllegalArgumentException.class)
    public void missingCloudPoolName() throws CloudPoolException {
        CloudPoolConfig cloudPoolConfig = cloudPoolConfig();
        setPrivateField(cloudPoolConfig, "name", null);
        new BaseCloudPoolConfig(cloudPoolConfig, scaleOutConfig(), scaleInConfig(), null, null, null).validate();
    }

    // illegal config: missing /cloudPool/driverConfig
    @Test(expected = IllegalArgumentException.class)
    public void missingCloudPoolDriverConfig() throws CloudPoolException {
        CloudPoolConfig cloudPoolConfig = cloudPoolConfig();
        setPrivateField(cloudPoolConfig, "driverConfig", null);
        new BaseCloudPoolConfig(cloudPoolConfig, scaleOutConfig(), scaleInConfig(), null, null, null).validate();
    }

    // illegal config: missing /scaleOutConfig
    @Test(expected = IllegalArgumentException.class)
    public void missingScaleOutConfig() throws CloudPoolException {
        new BaseCloudPoolConfig(cloudPoolConfig(), null, scaleInConfig(), alertsConfig(), null, null).validate();
    }

    // illegal config: missing /scaleOutConfig/size
    @Test(expected = IllegalArgumentException.class)
    public void missingScaleOutConfigSize() throws CloudPoolException {
        ScaleOutConfig scaleOutConfig = scaleOutConfig();
        setPrivateField(scaleOutConfig, "size", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig, scaleInConfig(), alertsConfig(), null, null)
                .validate();
    }

    // illegal config: missing /scaleOutConfig/image
    @Test(expected = IllegalArgumentException.class)
    public void missingScaleOutConfigImage() throws CloudPoolException {
        ScaleOutConfig scaleOutConfig = scaleOutConfig();
        setPrivateField(scaleOutConfig, "image", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig, scaleInConfig(), alertsConfig(), null, null)
                .validate();
    }

    // it is okay for config to miss /scaleOutConfig/keyPair
    @Test
    public void missingScaleOutConfigKeyPair() throws CloudPoolException {
        ScaleOutConfig scaleOutConfig = scaleOutConfig();
        setPrivateField(scaleOutConfig, "keyPair", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig, scaleInConfig(), alertsConfig(), null, null)
                .validate();
    }

    // it is okay for config to miss /scaleOutConfig/securityGroups
    @Test
    public void missingScaleOutConfigSecurityGroups() throws CloudPoolException {
        ScaleOutConfig scaleOutConfig = scaleOutConfig();
        setPrivateField(scaleOutConfig, "securityGroups", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig, scaleInConfig(), alertsConfig(), null, null)
                .validate();
    }

    // it is okay for config to miss /scaleOutConfig/encodedUserData
    @Test
    public void missingScaleOutConfigEncodedUserData() throws CloudPoolException {
        ScaleOutConfig scaleOutConfig = scaleOutConfig();
        setPrivateField(scaleOutConfig, "encodedUserData", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig, scaleInConfig(), alertsConfig(), null, null)
                .validate();
    }

    // illegal config: missing /alerts/smtp[0]/subject
    @Test(expected = IllegalArgumentException.class)
    public void missingSmtpSubject() throws CloudPoolException {
        AlertersConfig alerts = alertsConfig();
        setPrivateField(alerts.getSmtpAlerters().get(0), "subject", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alerts, null, null).validate();
    }

    // illegal config: missing /alerts/smtp[0]/recipients
    @Test(expected = IllegalArgumentException.class)
    public void missingSmtpRecipients() throws CloudPoolException {
        AlertersConfig alerts = alertsConfig();
        setPrivateField(alerts.getSmtpAlerters().get(0), "recipients", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alerts, null, null).validate();
    }

    // illegal config: missing /alerts/smtp[0]/sender
    @Test(expected = IllegalArgumentException.class)
    public void missingSmtpSender() throws CloudPoolException {
        AlertersConfig alerts = alertsConfig();
        setPrivateField(alerts.getSmtpAlerters().get(0), "sender", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alerts, null, null).validate();
    }

    // illegal config: missing /alerts/smtp[0]/smtpClientConfig
    @Test(expected = IllegalArgumentException.class)
    public void missingSmtpClientConfig() throws CloudPoolException {
        AlertersConfig alerts = alertsConfig();
        setPrivateField(alerts.getSmtpAlerters().get(0), "smtpClientConfig", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alerts, null, null).validate();
    }

    // illegal config: missing /alerts/smtp[0]/smtpClientConfig/smtpHost
    @Test(expected = IllegalArgumentException.class)
    public void missingSmtpHost() throws CloudPoolException {
        AlertersConfig alerts = alertsConfig();
        setPrivateField(alerts.getSmtpAlerters().get(0).getSmtpClientConfig(), "smtpHost", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alerts, null, null).validate();
    }

    // illegal config: missing
    // /alerts/smtp[0]/smtpClientConfig/authentication/userName
    @Test(expected = IllegalArgumentException.class)
    public void missingSmtpAuthenticationUsername() throws CloudPoolException {
        AlertersConfig alerts = alertsConfig();
        SmtpClientAuthentication authentication = alerts.getSmtpAlerters().get(0).getSmtpClientConfig()
                .getAuthentication();
        setPrivateField(authentication, "username", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alerts, null, null).validate();
    }

    // illegal config: missing
    // /alerts/smtp[0]/smtpClientConfig/authentication/password
    @Test(expected = IllegalArgumentException.class)
    public void missingSmtpAuthenticationPassword() throws CloudPoolException {
        AlertersConfig alerts = alertsConfig();
        SmtpClientAuthentication authentication = alerts.getSmtpAlerters().get(0).getSmtpClientConfig()
                .getAuthentication();
        setPrivateField(authentication, "password", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alerts, null, null).validate();
    }

    // illegal config: missing /alerts/http[0]/destinationUrls
    @Test(expected = IllegalArgumentException.class)
    public void missingHttpDestinationUrls() throws CloudPoolException {
        AlertersConfig alerts = alertsConfig();
        HttpAlerterConfig httpAlerter = alerts.getHttpAlerters().get(0);
        setPrivateField(httpAlerter, "destinationUrls", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alerts, null, null).validate();
    }

    // illegal config: missing /alerts/http[0]/auth/basicCredentials/username
    @Test(expected = IllegalArgumentException.class)
    public void missingHttpBasicAuthUsername() throws CloudPoolException {
        AlertersConfig alerts = alertsConfig();
        HttpAlerterConfig httpAlerter = alerts.getHttpAlerters().get(0);
        setPrivateField(httpAlerter.getAuth().getBasicCredentials().get(), "username", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alerts, null, null).validate();
    }

    // illegal config: missing /alerts/http[0]/auth/basicCredentials/password
    @Test(expected = IllegalArgumentException.class)
    public void missingHttpBasicAuthPassword() throws CloudPoolException {
        AlertersConfig alerts = alertsConfig();
        HttpAlerterConfig httpAlerter = alerts.getHttpAlerters().get(0);
        setPrivateField(httpAlerter.getAuth().getBasicCredentials().get(), "password", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alerts, null, null).validate();
    }

    // illegal config: missing /poolFetch/retries
    @Test(expected = IllegalArgumentException.class)
    public void missingPoolFetchRetries() {
        PoolFetchConfig poolFetch = poolFetch();
        setPrivateField(poolFetch, "retries", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alertsConfig(), poolFetch, null)
                .validate();
    }

    // illegal config: missing /poolFetch/refreshInterval
    @Test(expected = IllegalArgumentException.class)
    public void missingPoolFetchRefreshInterval() {
        PoolFetchConfig poolFetch = poolFetch();
        setPrivateField(poolFetch, "refreshInterval", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alertsConfig(), poolFetch, null)
                .validate();
    }

    // illegal config: missing /poolFetch/reachabilityTimeout
    @Test(expected = IllegalArgumentException.class)
    public void missingPoolFetchReachabilityTimeout() {
        PoolFetchConfig poolFetch = poolFetch();
        setPrivateField(poolFetch, "reachabilityTimeout", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alertsConfig(), poolFetch, null)
                .validate();
    }

    // illegal config: missing /poolUpdate/updateInterval
    @Test(expected = IllegalArgumentException.class)
    public void missingPoolUpdateInterval() {
        PoolUpdateConfig poolUpdate = poolUpdate();
        setPrivateField(poolUpdate, "updateInterval", null);
        new BaseCloudPoolConfig(cloudPoolConfig(), scaleOutConfig(), scaleInConfig(), alertsConfig(), null, poolUpdate)
                .validate();
    }

    private AlertersConfig alertsConfig() {
        List<SmtpAlerterConfig> emailAlerters = Arrays.asList(emailAlerterConfig("user@elastisys.com", "ERROR|FATAL"));
        List<HttpAlerterConfig> httpAlerters = Arrays.asList(httpAlerterConfig("https://host", "ERROR"));
        return new AlertersConfig(emailAlerters, httpAlerters);
    }

    private HttpAlerterConfig httpAlerterConfig(String url, String severityFilter) {
        return new HttpAlerterConfig(Arrays.asList(url), severityFilter,
                new HttpAuthConfig(new BasicCredentials("user", "pass"), null));
    }

    private SmtpAlerterConfig emailAlerterConfig(String recipient, String severityFilter) {
        return new SmtpAlerterConfig(Arrays.asList(recipient), "sender@elastisys.com", "subject", severityFilter,
                smtpClientConfig());
    }

    private SmtpClientConfig smtpClientConfig() {
        return new SmtpClientConfig("some.mail.host", 587, smtpAuth(), true);
    }

    private SmtpClientAuthentication smtpAuth() {
        return new SmtpClientAuthentication("userName", "password");
    }

    private CloudPoolConfig cloudPoolConfig() {
        return new CloudPoolConfig("MyScalingGroup", cloudCredentialsConfig());
    }

    private ScaleOutConfig scaleOutConfig() {
        List<String> bootScript = Arrays.asList("#!/bin/bash", "apt-get update -qy && apt-get isntall apache2 -qy");
        String encodedUserData = Base64Utils.toBase64(bootScript);
        return new ScaleOutConfig("size", "image", "keyPair", Arrays.asList("securityGroup"), encodedUserData);
    }

    private ScaleInConfig scaleInConfig() {
        return new ScaleInConfig(VictimSelectionPolicy.CLOSEST_TO_INSTANCE_HOUR, 300);
    }

    private PoolFetchConfig poolFetch() {
        RetriesConfig retriesConfig = new RetriesConfig(3, new TimeInterval(2L, TimeUnit.SECONDS));
        TimeInterval refreshInterval = new TimeInterval(20L, TimeUnit.SECONDS);
        TimeInterval reachabilityTimeout = new TimeInterval(2L, TimeUnit.MINUTES);
        return new PoolFetchConfig(retriesConfig, refreshInterval, reachabilityTimeout);
    }

    private PoolUpdateConfig poolUpdate() {
        return new PoolUpdateConfig(new TimeInterval(1L, TimeUnit.MINUTES));
    }

    private JsonObject cloudCredentialsConfig() {
        return JsonUtils.parseJsonString("{\"userName\": \"johndoe\", " + "\"region\": \"us-east-1\"}")
                .getAsJsonObject();
    }

    /**
     * Sets a private field on an instance object.
     *
     * @param object
     *            The object.
     * @param privateFieldName
     *            The name of the private field to set.
     * @param valueToSet
     *            The value to set.
     * @throws Exception
     */
    private void setPrivateField(Object object, String privateFieldName, Object valueToSet) throws RuntimeException {
        try {
            Field field = object.getClass().getDeclaredField(privateFieldName);
            field.setAccessible(true);
            field.set(object, valueToSet);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("could not set private field '%s' on object: %s", privateFieldName, e.getMessage()));
        }
    }
}
