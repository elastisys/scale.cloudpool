package com.elastisys.scale.cloudpool.multipool.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

import org.junit.Before;
import org.junit.Test;

import com.elastisys.scale.cloudpool.api.CloudPool;
import com.elastisys.scale.cloudpool.api.types.CloudPoolStatus;
import com.elastisys.scale.cloudpool.api.types.MembershipStatus;
import com.elastisys.scale.cloudpool.api.types.ServiceState;
import com.elastisys.scale.cloudpool.multipool.api.CloudPoolInstance;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.util.file.FileUtils;
import com.google.common.base.Optional;
import com.google.common.io.Files;
import com.google.gson.JsonObject;

/**
 * Exercise {@link DiskBackedCloudPoolInstance}.
 *
 */
public class TestDiskBackedCloudPoolInstance {

    private static final File stateDir = new File("target/multipool/cloudpools/my-pool");
    private static final File INSTANCE_STATUS_FILE = new File(stateDir, DiskBackedCloudPoolInstance.STATUS_FILE);
    private static final File INSTANCE_CONFIG_FILE = new File(stateDir, DiskBackedCloudPoolInstance.CONFIG_FILE);
    private CloudPool mockedCloudPool;

    /** Object under test. */
    private DiskBackedCloudPoolInstance cloudPoolInstance;

    @Before
    public void beforeTestMethod() throws IOException {
        this.mockedCloudPool = mock(CloudPool.class);
        FileUtils.deleteRecursively(stateDir);
        this.cloudPoolInstance = new DiskBackedCloudPoolInstance(this.mockedCloudPool, stateDir);
    }

    /**
     * A {@link CloudPool} must be given.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createWithNullCloudPool() throws IOException {
        CloudPool nullCloudPool = null;
        new DiskBackedCloudPoolInstance(nullCloudPool, stateDir);
    }

    /**
     * A state directory must be given.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createWithNullStateDir() throws IOException {
        File nullStateDir = null;
        new DiskBackedCloudPoolInstance(this.mockedCloudPool, nullStateDir);
    }

    /**
     * If the state directory doesn't exist, it should be created.
     */
    @Test
    public void shouldCreateStateDirOnCreation() throws IOException {
        FileUtils.deleteRecursively(stateDir);
        assertThat(stateDir.exists(), is(false));

        new DiskBackedCloudPoolInstance(this.mockedCloudPool, stateDir);
        assertThat(stateDir.isDirectory(), is(true));
    }

    /**
     * If the state dir cannot be written to, the constructor should fail.
     */
    @Test(expected = AccessDeniedException.class)
    public void shouldFailIfStateDirRequiresPrivileges() throws IOException {
        File notWritableDir = new File("/root/mypool");
        new DiskBackedCloudPoolInstance(this.mockedCloudPool, notWritableDir);
    }

    /**
     * When {@link CloudPool#configure(JsonObject)} is called, the state of the
     * {@link CloudPoolInstance} should be saved.
     */
    @Test
    public void saveOnConfigure() {
        JsonObject config = JsonUtils.parseJsonString("{\"a\": \"b\"}").getAsJsonObject();
        CloudPoolStatus status = new CloudPoolStatus(false, true);

        when(this.mockedCloudPool.getConfiguration()).thenReturn(Optional.of(config));
        when(this.mockedCloudPool.getStatus()).thenReturn(status);

        assertThat(INSTANCE_CONFIG_FILE.exists(), is(false));
        assertThat(INSTANCE_STATUS_FILE.exists(), is(false));

        this.cloudPoolInstance.configure(config);

        // verify that state was saved
        assertThat(INSTANCE_CONFIG_FILE.exists(), is(true));
        assertThat(INSTANCE_STATUS_FILE.exists(), is(true));

        assertThat(JsonUtils.parseJsonFile(INSTANCE_CONFIG_FILE), is(config));
        assertThat(JsonUtils.parseJsonFile(INSTANCE_STATUS_FILE), is(JsonUtils.toJson(status)));
    }

    /**
     * When {@link CloudPool#start()} is called, the state of the
     * {@link CloudPoolInstance} should be saved.
     */
    @Test
    public void saveOnStart() throws IOException {
        JsonObject config = JsonUtils.parseJsonString("{\"a\": \"b\"}").getAsJsonObject();
        CloudPoolStatus status = new CloudPoolStatus(true, true);

        when(this.mockedCloudPool.getConfiguration()).thenReturn(Optional.of(config));
        when(this.mockedCloudPool.getStatus()).thenReturn(status);

        // write some existing content and make sure it gets overwritten
        Files.write("{}".getBytes(), INSTANCE_CONFIG_FILE);
        Files.write("{}".getBytes(), INSTANCE_STATUS_FILE);

        this.cloudPoolInstance.start();

        // verify that state was saved
        assertThat(INSTANCE_CONFIG_FILE.exists(), is(true));
        assertThat(INSTANCE_STATUS_FILE.exists(), is(true));

        assertThat(JsonUtils.parseJsonFile(INSTANCE_CONFIG_FILE), is(config));
        assertThat(JsonUtils.parseJsonFile(INSTANCE_STATUS_FILE), is(JsonUtils.toJson(status)));
    }

    /**
     * When {@link CloudPool#stop()} is called, the state of the
     * {@link CloudPoolInstance} should be saved.
     */
    @Test
    public void saveOnstop() throws IOException {
        JsonObject config = JsonUtils.parseJsonString("{\"a\": \"b\"}").getAsJsonObject();
        CloudPoolStatus status = new CloudPoolStatus(false, true);

        when(this.mockedCloudPool.getConfiguration()).thenReturn(Optional.of(config));
        when(this.mockedCloudPool.getStatus()).thenReturn(status);

        // write some existing content and make sure it gets overwritten
        Files.write("{}".getBytes(), INSTANCE_CONFIG_FILE);
        Files.write("{}".getBytes(), INSTANCE_STATUS_FILE);

        this.cloudPoolInstance.stop();

        // verify that state was saved
        assertThat(INSTANCE_CONFIG_FILE.exists(), is(true));
        assertThat(INSTANCE_STATUS_FILE.exists(), is(true));

        assertThat(JsonUtils.parseJsonFile(INSTANCE_CONFIG_FILE), is(config));
        assertThat(JsonUtils.parseJsonFile(INSTANCE_STATUS_FILE), is(JsonUtils.toJson(status)));
    }

    /**
     * When {@link DiskBackedCloudPoolInstance#restore()} is called, the
     * configuration and started status should be read from disk and set on the
     * {@link CloudPool}.
     */
    @Test
    public void restore() throws IOException {
        JsonObject config = JsonUtils.parseJsonString("{\"a\": \"b\"}").getAsJsonObject();
        CloudPoolStatus status = new CloudPoolStatus(true, true);

        // write some existing content and make sure it gets overwritten
        Files.write(JsonUtils.toString(config).getBytes(), INSTANCE_CONFIG_FILE);
        Files.write(JsonUtils.toString(JsonUtils.toJson(status)).getBytes(), INSTANCE_STATUS_FILE);

        this.cloudPoolInstance.restore();

        // verify that cloudpool had its config restored
        verify(this.mockedCloudPool).configure(config);
        // verify that cloudpool had its started state restored
        verify(this.mockedCloudPool).start();
    }

    /**
     * When the saved started state is <code>false</code>, the recovered
     * {@link CloudPool} should be in a stopped state.
     */
    @Test
    public void restoreStopped() throws IOException {
        JsonObject config = JsonUtils.parseJsonString("{\"a\": \"b\"}").getAsJsonObject();
        CloudPoolStatus status = new CloudPoolStatus(false, true);

        // write some existing content and make sure it gets overwritten
        Files.write(JsonUtils.toString(config).getBytes(), INSTANCE_CONFIG_FILE);
        Files.write(JsonUtils.toString(JsonUtils.toJson(status)).getBytes(), INSTANCE_STATUS_FILE);

        this.cloudPoolInstance.restore();

        // verify that cloudpool had its started state restored
        verify(this.mockedCloudPool).stop();
    }

    /**
     * It should be possible to call restore when the state dir is empty. This
     * should be a no-op.
     */
    @Test
    public void restoreFromEmptyStateDir() throws IOException {
        INSTANCE_CONFIG_FILE.delete();
        INSTANCE_STATUS_FILE.delete();

        this.cloudPoolInstance.restore();

        // no attempt should be made to set config or start/stop
        verifyZeroInteractions(this.mockedCloudPool);
    }

    @Test
    public void delegateConfigure() {
        JsonObject config = JsonUtils.parseJsonString("{\"a\": \"b\"}").getAsJsonObject();
        this.cloudPoolInstance.configure(config);
        verify(this.mockedCloudPool).configure(config);
    }

    /**
     * {@link CloudPool} methods called on {@link DiskBackedCloudPoolInstance}
     * should be delegated to the wrapped {@link CloudPool}.
     */
    @Test
    public void delegateGetConfiguration() {
        this.cloudPoolInstance.getConfiguration();
        verify(this.mockedCloudPool).getConfiguration();
    }

    /**
     * {@link CloudPool} methods called on {@link DiskBackedCloudPoolInstance}
     * should be delegated to the wrapped {@link CloudPool}.
     */
    @Test
    public void delegateStart() {
        this.cloudPoolInstance.start();
        verify(this.mockedCloudPool).start();
    }

    /**
     * {@link CloudPool} methods called on {@link DiskBackedCloudPoolInstance}
     * should be delegated to the wrapped {@link CloudPool}.
     */
    @Test
    public void delegateStop() {
        this.cloudPoolInstance.stop();
        verify(this.mockedCloudPool).stop();
    }

    /**
     * {@link CloudPool} methods called on {@link DiskBackedCloudPoolInstance}
     * should be delegated to the wrapped {@link CloudPool}.
     */
    @Test
    public void delegateGetStatus() {
        this.cloudPoolInstance.getStatus();
        verify(this.mockedCloudPool).getStatus();
        verifyNoMoreInteractions(this.mockedCloudPool);
    }

    /**
     * {@link CloudPool} methods called on {@link DiskBackedCloudPoolInstance}
     * should be delegated to the wrapped {@link CloudPool}.
     */
    @Test
    public void delegateGetMachinePool() {
        this.cloudPoolInstance.getMachinePool();
        verify(this.mockedCloudPool).getMachinePool();
        verifyNoMoreInteractions(this.mockedCloudPool);
    }

    /**
     * {@link CloudPool} methods called on {@link DiskBackedCloudPoolInstance}
     * should be delegated to the wrapped {@link CloudPool}.
     */
    @Test
    public void delegateGetPoolSize() {
        this.cloudPoolInstance.getPoolSize();
        verify(this.mockedCloudPool).getPoolSize();
        verifyNoMoreInteractions(this.mockedCloudPool);
    }

    /**
     * {@link CloudPool} methods called on {@link DiskBackedCloudPoolInstance}
     * should be delegated to the wrapped {@link CloudPool}.
     */
    @Test
    public void delegateSetDesiredSize() {
        this.cloudPoolInstance.setDesiredSize(10);
        verify(this.mockedCloudPool).setDesiredSize(10);
        verifyNoMoreInteractions(this.mockedCloudPool);
    }

    /**
     * {@link CloudPool} methods called on {@link DiskBackedCloudPoolInstance}
     * should be delegated to the wrapped {@link CloudPool}.
     */
    @Test
    public void delegateTerminateMachine() {
        this.cloudPoolInstance.terminateMachine("i-1", true);
        verify(this.mockedCloudPool).terminateMachine("i-1", true);
        verifyNoMoreInteractions(this.mockedCloudPool);
    }

    /**
     * {@link CloudPool} methods called on {@link DiskBackedCloudPoolInstance}
     * should be delegated to the wrapped {@link CloudPool}.
     */
    @Test
    public void delegateSetServiceState() {
        this.cloudPoolInstance.setServiceState("i-1", ServiceState.BOOTING);
        verify(this.mockedCloudPool).setServiceState("i-1", ServiceState.BOOTING);
        verifyNoMoreInteractions(this.mockedCloudPool);
    }

    /**
     * {@link CloudPool} methods called on {@link DiskBackedCloudPoolInstance}
     * should be delegated to the wrapped {@link CloudPool}.
     */
    @Test
    public void delegateSetMembershipStatus() {
        this.cloudPoolInstance.setMembershipStatus("i-1", MembershipStatus.blessed());
        verify(this.mockedCloudPool).setMembershipStatus("i-1", MembershipStatus.blessed());
        verifyNoMoreInteractions(this.mockedCloudPool);
    }

    /**
     * {@link CloudPool} methods called on {@link DiskBackedCloudPoolInstance}
     * should be delegated to the wrapped {@link CloudPool}.
     */
    @Test
    public void delegateAttachMachine() {
        this.cloudPoolInstance.attachMachine("i-1");
        verify(this.mockedCloudPool).attachMachine("i-1");
        verifyNoMoreInteractions(this.mockedCloudPool);
    }

    /**
     * {@link CloudPool} methods called on {@link DiskBackedCloudPoolInstance}
     * should be delegated to the wrapped {@link CloudPool}.
     */
    @Test
    public void delegateDetachMachine() {
        this.cloudPoolInstance.detachMachine("i-1", false);
        verify(this.mockedCloudPool).detachMachine("i-1", false);
        verifyNoMoreInteractions(this.mockedCloudPool);
    }

}
