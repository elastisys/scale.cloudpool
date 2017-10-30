package com.elastisys.scale.cloudpool.azure.driver;

import static com.google.common.base.Preconditions.checkArgument;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.elastisys.scale.commons.util.time.UtcTime;
import com.microsoft.azure.management.batch.ProvisioningState;
import com.microsoft.azure.management.compute.InstanceViewStatus;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.StatusLevelTypes;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineInstanceView;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NicIPConfiguration;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

/**
 * Builder for constructing mock {@link VirtualMachine}s in tests.
 */
public class VmMockBuilder {
    public static final String DEFAULT_SUBSCRIPTION_ID = "123";
    public static final String DEFAULT_RESOURCE_GROUP = "my-rg";
    public static final Region DEFAULT_REGION = Region.EUROPE_NORTH;
    public static final VirtualMachineSizeTypes DEFAULT_VM_SIZE = VirtualMachineSizeTypes.STANDARD_DS1_V2;
    public static final String DEFAULT_PRIVATE_IP = "10.0.0.1";
    public static final ProvisioningState DEFAULT_PROVISIONING_STATE = ProvisioningState.SUCCEEDED;

    private String name;
    private String subscriptionId = DEFAULT_SUBSCRIPTION_ID;
    private String resourceGroup = DEFAULT_RESOURCE_GROUP;
    private Region region = DEFAULT_REGION;

    private VirtualMachineSizeTypes size = DEFAULT_VM_SIZE;
    private DateTime startTime = UtcTime.now();
    private String privateIp = DEFAULT_PRIVATE_IP;
    private String publicIp;

    private PowerState powerState;
    private ProvisioningState provisioningState = DEFAULT_PROVISIONING_STATE;

    private Map<String, String> tags = new HashMap<>();

    public static VmMockBuilder with() {
        return new VmMockBuilder();
    }

    public VmMockBuilder name(String name) {
        this.name = name;
        return this;
    }

    public VmMockBuilder subscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
        return this;
    }

    public VmMockBuilder resourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
        return this;
    }

    public VmMockBuilder region(Region region) {
        this.region = region;
        return this;
    }

    public VmMockBuilder size(VirtualMachineSizeTypes size) {
        this.size = size;
        return this;
    }

    public VmMockBuilder startTime(DateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public VmMockBuilder privateIp(String privateIp) {
        this.privateIp = privateIp;
        return this;
    }

    public VmMockBuilder publicIp(String publicIp) {
        this.publicIp = publicIp;
        return this;
    }

    public VmMockBuilder powerState(PowerState powerState) {
        this.powerState = powerState;
        return this;
    }

    public VmMockBuilder provisioningState(ProvisioningState provisioningState) {
        this.provisioningState = provisioningState;
        return this;
    }

    public VmMockBuilder tags(Map<String, String> tags) {
        this.tags = tags;
        return this;
    }

    public VirtualMachine build() {
        checkArgument(this.name != null, "vm mock: missing name");
        checkArgument(this.resourceGroup != null, "vm mock: missing resourceGroup");
        checkArgument(this.region != null, "vm mock: missing region");
        checkArgument(this.size != null, "vm mock: missing size");
        checkArgument(this.privateIp != null, "vm mock: missing privateIp");
        checkArgument(this.provisioningState != null, "vm mock: missing provisioningState");
        checkArgument(this.tags != null, "vm mock: missing tags");

        VirtualMachine vmMock = mock(VirtualMachine.class);

        when(vmMock.id()).thenReturn(id());
        when(vmMock.name()).thenReturn(this.name);
        when(vmMock.regionName()).thenReturn(this.region.toString());
        when(vmMock.region()).thenReturn(this.region);
        when(vmMock.size()).thenReturn(this.size);
        when(vmMock.resourceGroupName()).thenReturn(this.resourceGroup);
        NetworkInterface primaryNicMock = primaryNicMock();
        when(vmMock.getPrimaryNetworkInterface()).thenReturn(primaryNicMock);
        when(vmMock.powerState()).thenReturn(this.powerState);

        if (this.provisioningState == null) {
            when(vmMock.provisioningState()).thenReturn(null);
        } else {
            when(vmMock.provisioningState()).thenReturn(this.provisioningState.toString());
        }

        when(vmMock.tags()).thenReturn(this.tags);
        VirtualMachineInstanceView mockInstanceView = mockInstanceView();
        when(vmMock.instanceView()).thenReturn(mockInstanceView);

        return vmMock;
    }

    private String id() {
        return String.format("/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s",
                this.subscriptionId, this.resourceGroup, this.name);
    }

    private NetworkInterface primaryNicMock() {
        NetworkInterface nicMock = mock(NetworkInterface.class);

        when(nicMock.primaryPrivateIP()).thenReturn(this.privateIp);

        NicIPConfiguration primaryIpConfigMock = mock(NicIPConfiguration.class);
        when(nicMock.primaryIPConfiguration()).thenReturn(primaryIpConfigMock);

        if (this.publicIp != null) {
            PublicIPAddress publicIpAddrMock = mock(PublicIPAddress.class);
            when(primaryIpConfigMock.getPublicIPAddress()).thenReturn(publicIpAddrMock);
            when(publicIpAddrMock.ipAddress()).thenReturn(this.publicIp);
        }

        return nicMock;
    }

    private VirtualMachineInstanceView mockInstanceView() {
        VirtualMachineInstanceView instanceViewMock = mock(VirtualMachineInstanceView.class);

        List<InstanceViewStatus> statuses = new ArrayList<>();

        if (this.startTime != null) {
            InstanceViewStatus mockStatus = mock(InstanceViewStatus.class);
            when(mockStatus.code()).thenReturn("ProvisioningState/succeeded");
            when(mockStatus.displayStatus()).thenReturn("Provisioning succeeded");
            when(mockStatus.level()).thenReturn(StatusLevelTypes.INFO);
            when(mockStatus.time()).thenReturn(this.startTime);
            statuses.add(mockStatus);
        }
        when(instanceViewMock.statuses()).thenReturn(statuses);
        return instanceViewMock;

    }
}
