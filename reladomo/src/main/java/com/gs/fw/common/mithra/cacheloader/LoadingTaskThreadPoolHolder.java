/*
 Copyright 2016 Goldman Sachs.
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package com.gs.fw.common.mithra.cacheloader;


import java.util.List;

import com.gs.collections.impl.list.mutable.FastList;
import com.gs.fw.common.mithra.database.SyslogChecker;

public class LoadingTaskThreadPoolHolder
{
    private final IOTaskThreadPoolWithCpuTaskConveyor ioTaskThreadPoolWithCpuTaskConveyor;
    private final List<DependentKeyIndex> dependentKeyIndices = FastList.newList();
    private final SyslogChecker syslogChecker;

    public LoadingTaskThreadPoolHolder(ExternalQueueThreadExecutor.ExceptionHandler exceptionHandler, String name, int threadCount, SyslogChecker syslogChecker)
    {
        this.syslogChecker = syslogChecker;
        ioTaskThreadPoolWithCpuTaskConveyor = new IOTaskThreadPoolWithCpuTaskConveyor(new DependentTaskBuilder(), exceptionHandler, name, threadCount);
    }

    private class DependentTaskBuilder implements IOTaskThreadPoolWithCpuTaskConveyor.Builder
    {
        //always called by the same single conveyor thread
        @Override
        public Runnable build()
        {
            long maxSize = 0;
            DependentKeyIndex largestIndex = null;

            for (int i = 0; i < dependentKeyIndices.size(); i++)
            {
                DependentKeyIndex each = dependentKeyIndices.get(i);
                if (each.size() > maxSize)
                {
                    maxSize = each.size();
                    largestIndex = each;
                }
            }

            return maxSize == 0 ? null : largestIndex.createTaskRunner(LoadingTaskRunner.State.QUEUED);
        }

    }

    public void addDependentKeyIndex(DependentKeyIndex dependentKeyIndex)
    {
        dependentKeyIndices.add(dependentKeyIndex);
    }

    public SyslogChecker getSyslogChecker()
    {
        return syslogChecker;
    }

    public void updateMonitor(LoadingTaskThreadPoolMonitor monitor)
    {
        long indexSize = 0L;
        long maxSize = 0L;
        long producedKeys = 0L;
        for (int i = 0; i < dependentKeyIndices.size(); i++)
        {
            long size = dependentKeyIndices.get(i).size();
            indexSize += size;
            if (size > maxSize) maxSize = size;
        }

        this.ioTaskThreadPoolWithCpuTaskConveyor.updateMonitor(monitor);
        monitor.setDependentKeyIndices(dependentKeyIndices.size());
        monitor.setIndexSize(indexSize);
        monitor.setMaxSize(maxSize);
        monitor.setProducedKeys(producedKeys);
    }

    public IOTaskThreadPoolWithCpuTaskConveyor getThreadPool()
    {
        return this.ioTaskThreadPoolWithCpuTaskConveyor;
    }
}
