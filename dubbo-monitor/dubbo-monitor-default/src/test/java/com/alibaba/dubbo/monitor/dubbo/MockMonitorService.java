/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.monitor.dubbo;

import java.util.Arrays;
import java.util.List;

import cn.sunline.ltts.apm.api.registry.base.EURL;
import com.alibaba.dubbo.monitor.MonitorService;

/**
 * MockMonitorService
 * 
 * @author william.liangf
 */
public class MockMonitorService implements MonitorService {
    
    private EURL statistics;

    public void collect(EURL statistics) {
        this.statistics = statistics;
    }

    public EURL getStatistics() {
        return statistics;
    }

	public List<EURL> lookup(EURL query) {
		return Arrays.asList(statistics);
	}

}
