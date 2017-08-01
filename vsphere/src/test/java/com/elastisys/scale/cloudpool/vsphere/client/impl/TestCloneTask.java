package com.elastisys.scale.cloudpool.vsphere.client.impl;

import com.elastisys.scale.cloudpool.vsphere.tag.Tag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.ScalingTag;
import com.elastisys.scale.cloudpool.vsphere.tag.impl.VsphereTag;
import com.elastisys.scale.cloudpool.vsphere.tagger.Tagger;
import com.google.common.collect.Lists;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.mo.Task;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class TestCloneTask {

    Tagger tagger;
    Task task;
    TaskInfo taskInfo;

    @Before
    public void setUp() throws Exception {
        tagger = mock(Tagger.class);
        task = mock(Task.class);
        taskInfo = mock(TaskInfo.class);
        when(task.getTaskInfo()).thenReturn(taskInfo);
    }

    @Test
    public void testWithSuccessfulTask() throws Exception {
        List<Tag> tags = Lists.newArrayList(new VsphereTag(ScalingTag.CLOUD_POOL, "TestTag"));
        when(task.waitForTask()).thenReturn("success");
        CloneTask cloneTask = new CloneTask(tagger, task, tags);
        cloneTask.call();
        verify(task, times(1)).getTaskInfo();
        verify(taskInfo, times(1)).getResult();
    }

    @Test
    public void testWithFailedTask() throws Exception {
        List<Tag> tags = Lists.newArrayList();
        when(task.waitForTask()).thenReturn("error");
        CloneTask cloneTask = new CloneTask(tagger, task, tags);
        cloneTask.call();
        verify(task, times(0)).getTaskInfo();
    }
}
