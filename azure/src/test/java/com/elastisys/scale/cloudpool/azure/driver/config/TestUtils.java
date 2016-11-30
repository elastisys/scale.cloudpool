package com.elastisys.scale.cloudpool.azure.driver.config;

import java.util.Arrays;
import java.util.Map;

import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;
import com.google.common.collect.ImmutableMap;

public class TestUtils {

    public static AzureAuth validAuth() {
        // Sample clientId/appId.
        String CLIENT_ID = "12345678-abcd-efff-bcbc-90bbcfdeeecf";
        // Sample domain/tenant.
        String DOMAIN = "87654321-abcd-efff-bcbc-90bbcfdeeecf";
        // Sample secret/password.
        String SECRET = "12121212-aaaa-bbbb-cccc-000000000000";
        return new AzureAuth(CLIENT_ID, DOMAIN, SECRET, "AZURE");
    }

    /**
     * Creates an {@link AzureAuth} with invalid Azure environment.
     *
     * @return
     */
    public static AzureAuth invalidAuth() {
        // Sample clientId/appId.
        String CLIENT_ID = "12345678-abcd-efff-bcbc-90bbcfdeeecf";
        // Sample domain/tenant.
        String DOMAIN = "87654321-abcd-efff-bcbc-90bbcfdeeecf";
        // Sample secret/password.
        String SECRET = "12121212-aaaa-bbbb-cccc-000000000000";

        return new AzureAuth(CLIENT_ID, DOMAIN, SECRET, "AZURE_INVALID_ENV");
    }

    public static TimeInterval illegalTimeInterval() {
        return JsonUtils.toObject(JsonUtils.parseJsonString("{\"time\": -1, \"unit\": \"minutes\"}"),
                TimeInterval.class);
    }

    /**
     * Creates a valid {@link AzureApiAccess} config.
     *
     * @return
     */
    public static AzureApiAccess validApiAccess() {
        // sample subscriptionId
        String subscriptionId = "11111111-2222-3333-4444-555555555555";
        return new AzureApiAccess(subscriptionId, validAuth());
    }

    /**
     * Creates an {@link AzureApiAccess} config with invalid {@link AzureAuth}.
     *
     * @return
     */
    public static AzureApiAccess invalidApiAccess() {
        // sample subscriptionId
        String subscriptionId = "11111111-2222-3333-4444-555555555555";
        return new AzureApiAccess(subscriptionId, invalidAuth());
    }

    public static CustomScriptExtension validCustomScript() {
        return new CustomScriptExtension(
                Arrays.asList(
                        "https://gist.githubusercontent.com/elastisys/09be421f09ae3646f1aadf4542f6b8f2/raw/e42334045905f908d781e78e03bb9412bf325da7/win-server-install-webserver.ps1"),
                "powershell.exe -ExecutionPolicy Unrestricted -File win-server-install-webserver.ps1");
    }

    /**
     * Creates an invalid {@link CustomScriptExtension} without an
     * encodedCommand.
     *
     * @return
     */
    public static CustomScriptExtension invalidCustomScript() {
        return JsonUtils.toObject(JsonUtils.parseJsonString("{\"fileUris\": [\"https://some/file.txt\"]}"),
                CustomScriptExtension.class);
    }

    public static LinuxSettings validLinuxSettings() {
        return new LinuxSettings("ubuntu", null, "secret", new CustomScriptExtension(null,
                "c2ggLWMgJ2FwdCB1cGRhdGUgLXF5ICYmIGFwdCBpbnN0YWxsIC1xeSBhcGFjaGUyJwo="));
    }

    /**
     * Creates an invalid {@link LinuxSettings} with a
     * {@link CustomScriptExtension} missing a command to execute.
     *
     * @return
     */
    public static LinuxSettings invalidLinuxSettings() {
        return new LinuxSettings("ubuntu", null, "secret", new CustomScriptExtension(null, null));
    }

    public static WindowsSettings validWindowsSettings() {
        return new WindowsSettings("administrator", "secret",
                new CustomScriptExtension(
                        Arrays.asList(
                                "https://gist.githubusercontent.com/elastisys/09be421f09ae3646f1aadf4542f6b8f2/raw/e42334045905f908d781e78e03bb9412bf325da7/win-server-install-webserver.ps1"),
                        "powershell.exe -ExecutionPolicy Unrestricted -File win-server-install-webserver.ps1"));
    }

    /**
     * Creates invalid {@link WindowsSettings} missing a admin password.
     *
     * @return
     */
    public static WindowsSettings invalidWindowsSettings() {
        String password = null;
        return new WindowsSettings("administrator", password,
                new CustomScriptExtension(
                        Arrays.asList(
                                "https://gist.githubusercontent.com/elastisys/09be421f09ae3646f1aadf4542f6b8f2/raw/e42334045905f908d781e78e03bb9412bf325da7/win-server-install-webserver.ps1"),
                        "powershell.exe -ExecutionPolicy Unrestricted -File win-server-install-webserver.ps1"));
    }

    public static NetworkSettings validNetworkSettings() {
        return new NetworkSettings("web-net", "default", true, Arrays.asList("ssh", "web"));
    }

    /**
     * Creates invalid {@link NetworkSettings} missing a subnet.
     *
     * @return
     */
    public static NetworkSettings invalidNetworkSettings() {
        String subnetName = null;
        return new NetworkSettings("web-net", subnetName, true, Arrays.asList("ssh", "web"));
    }

    public static Map<String, String> validTags() {
        return ImmutableMap.of("tier", "web", "region", "eu-west-1");
    }

}
