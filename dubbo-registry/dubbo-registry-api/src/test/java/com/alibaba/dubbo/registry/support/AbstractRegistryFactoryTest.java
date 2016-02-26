/*
 * Copyright 1999-2011 Alibaba Group.
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
package com.alibaba.dubbo.registry.support;

import java.util.List;

import org.junit.Test;

import com.alibaba.dubbo.common.utils.NetUtils;
//import com.alibaba.dubbo.registry.NotifyListener;
//import com.alibaba.dubbo.registry.Registry;
//import com.alibaba.dubbo.registry.RegistryFactory;

import cn.sunline.ltts.apm.api.registry.base.EURL;
import cn.sunline.ltts.apm.api.registry.base.NotifyListener;
import cn.sunline.ltts.apm.api.registry.base.Registry;
import cn.sunline.ltts.apm.api.registry.base.RegistryFactory;
import cn.sunline.ltts.apm.api.registry.base.support.AbstractRegistryFactory;
import junit.framework.Assert;
/**
 * AbstractRegistryFactoryTest
 * 
 * @author william.liangf
 */
public class AbstractRegistryFactoryTest {
    
    private RegistryFactory registryFactory = new AbstractRegistryFactory() {
        
        @Override
        protected Registry createRegistry(final EURL url) {
            return new Registry() {

                public EURL getEurl() {
                    return url;
                }

                public boolean isAvailable() {
                    return false;
                }

                public void destroy() {
                }

                public void register(EURL url) {
                }

                public void unregister(EURL url) {
                }

                public void subscribe(EURL url, NotifyListener listener) {
                }

                public void unsubscribe(EURL url, NotifyListener listener) {
                }

                public List<EURL> lookup(EURL url) {
                    return null;
                }

				

                
            };
        }
    };
    
    @Test
    public void testRegistryFactoryCache() throws Exception {
        EURL url = EURL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostAddress() + ":2233");
        Registry registry1 = registryFactory.getRegistry(url);
        Registry registry2 = registryFactory.getRegistry(url);
        Assert.assertEquals(registry1, registry2);
    }
    
    @Test
    public void testRegistryFactoryIpCache() throws Exception {
        Registry registry1 = registryFactory.getRegistry(EURL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":2233"));
        Registry registry2 = registryFactory.getRegistry(EURL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostAddress() + ":2233"));
        Assert.assertEquals(registry1, registry2);
    }

    @Test
    public void testRegistryFactoryGroupCache() throws Exception {
        Registry registry1 = registryFactory.getRegistry(EURL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":2233?group=aaa"));
        Registry registry2 = registryFactory.getRegistry(EURL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":2233?group=bbb"));
        Assert.assertNotSame(registry1, registry2);
    }

}