package com.elastisys.scale.cloudpool.azure.driver.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.elastisys.scale.commons.json.JsonUtils;

/**
 * Configuration that specifies a (set of) custom script(s) to run when a VM is
 * booted.
 * <p/>
 * See <a href="https://github.com/Azure/custom-script-extension-linux">custom
 * script extensions for linux</a> for details.
 *
 * @see LinuxSettings
 * @see WindowsSettings
 */
public class CustomScriptExtension {

    /**
     * A set of file URIs to download before executing the script. May be null.
     */
    private final List<String> fileUris;
    /**
     * A base64-encoded command to execute. Required. Such a command can, for
     * example, be generated via a call similar to:
     * {@code echo "sh -c 'apt update -qy && apt install -qy apache2'" | base64 -w0}.
     */
    private final String encodedCommand;

    /**
     * Creates a {@link CustomScriptExtension}.
     *
     * @param fileUris
     *            A set of file URIs to download before executing the script.
     *            May be null.
     * @param encodedCommand
     *            A base64-encoded command to execute. Required.
     */
    public CustomScriptExtension(List<String> fileUris, String encodedCommand) {
        this.fileUris = fileUris;
        this.encodedCommand = encodedCommand;
    }

    /**
     * A set of file URIs to download before executing the script.
     *
     * @return
     */
    public List<String> getFileUris() {
        return Optional.ofNullable(this.fileUris).orElse(Collections.emptyList());
    }

    /**
     * A base64-encoded command to execute.
     *
     * @return
     */
    public String getEncodedCommand() {
        return this.encodedCommand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileUris(), this.encodedCommand);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CustomScriptExtension) {
            CustomScriptExtension that = (CustomScriptExtension) obj;
            return Objects.equals(getFileUris(), that.getFileUris())
                    && Objects.equals(this.encodedCommand, that.encodedCommand);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyString(JsonUtils.toJson(this));
    }

    /**
     * Validates fields.
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        for (String fileUri : getFileUris()) {
            try {
                URI.create(fileUri).toURL();
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        String.format("customScript: fileUris: illegal file URI: %s: %s", fileUri, e.getMessage()), e);
            }
        }
        checkArgument(this.encodedCommand != null, "customScript: no encodedCommand given");
    }
}
