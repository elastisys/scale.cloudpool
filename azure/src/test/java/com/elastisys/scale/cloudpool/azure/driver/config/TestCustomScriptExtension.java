package com.elastisys.scale.cloudpool.azure.driver.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.elastisys.scale.commons.util.base64.Base64Utils;

/**
 * Exercises {@link CustomScriptExtension}.
 */
public class TestCustomScriptExtension {

    /** Sample fileUris */
    private static final List<String> FILE_URIS = Arrays.asList(
            "https://gist.githubusercontent.com/elastisys/09be421f09ae3646f1aadf4542f6b8f2/raw/e42334045905f908d781e78e03bb9412bf325da7/win-server-install-webserver.ps1");
    /** Sample encodedCommand */
    private static final String ENCODED_COMMAND = Base64Utils
            .toBase64("powershell.exe -ExecutionPolicy Unrestricted -File win-server-install-webserver.ps1");

    /**
     * Only commandToExecute is mandatory.
     */
    @Test
    public void onDefaults() {
        List<String> fileUris = null;
        CustomScriptExtension scriptExt = new CustomScriptExtension(fileUris, ENCODED_COMMAND);
        scriptExt.validate();

        assertThat(scriptExt.getEncodedCommand(), is(ENCODED_COMMAND));
        // check defaults
        assertThat(scriptExt.getFileUris(), is(Collections.emptyList()));
    }

    /**
     * Should be possible to specify files to download.
     */
    @Test
    public void complete() {
        CustomScriptExtension scriptExt = new CustomScriptExtension(FILE_URIS, ENCODED_COMMAND);
        scriptExt.validate();

        assertThat(scriptExt.getFileUris(), is(FILE_URIS));
        assertThat(scriptExt.getEncodedCommand(), is(ENCODED_COMMAND));
    }

    /**
     * each fileUri must be a URI.
     */
    @Test
    public void onIllegalFileUri() {
        List<String> fileUris = Arrays.asList("a/b/c");

        try {
            new CustomScriptExtension(fileUris, ENCODED_COMMAND).validate();
            fail("should fail");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
            assertTrue(e.getMessage().contains("fileUris"));
        }
    }

    /**
     * encodedCommand is required
     */
    @Test
    public void onMissingEncodedCommand() {
        try {
            String encodedCommand = null;
            new CustomScriptExtension(FILE_URIS, encodedCommand).validate();
            fail("should fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("encodedCommand"));
        }
    }
}
