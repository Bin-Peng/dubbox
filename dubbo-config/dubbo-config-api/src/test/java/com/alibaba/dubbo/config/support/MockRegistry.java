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
package com.alibaba.dubbo.config.support;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.common.Constants;
import cn.sunline.ltts.apm.api.registry.base.EURL;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;

/**
 * TODO Comment of MockRegistry
 * @author haomin.liuhm
 *
 */
public class MockRegistry implements Registry {

    static EURL subscribedUrl = new EURL("null", "0.0.0.0", 0);
    
    public static EURL getSubscribedUrl(){
        return subscribedUrl;
    }
    
    /* 
     * @see com.alibaba.dubbo.common.Node#getUrl()
     */
    public EURL getUrl() {
        return null;
    }

    /* 
     * @see com.alibaba.dubbo.common.Node#isAvailable()
     */
    public boolean isAvailable() {
        return true;
    }

    /* 
     * @see com.alibaba.dubbo.common.Node#destroy()
     */
    public void destroy() {
        
    }

    /* 
     * @see com.alibaba.dubbo.registry.RegistryService#register(com.alibaba.dubbo.common.URL)
     */
    public void register(EURL url) {
        
    }

    /* 
     * @see com.alibaba.dubbo.registry.RegistryService#unregister(com.alibaba.dubbo.common.URL)
     */
    public void unregister(EURL url) {
        
    }

    /* 
     * @see com.alibaba.dubbo.registry.RegistryService#subscribe(com.alibaba.dubbo.common.URL, com.alibaba.dubbo.registry.NotifyListener)
     */
    public void subscribe(EURL url, NotifyListener listener) {
        this.subscribedUrl = url;
        List<EURL> urls = new ArrayList<EURL>();
        
        urls.add(url.setProtocol("mockprotocol")
                    .removeParameter(Constants.CATEGORY_KEY)
                    .addParameter(Constants.METHODS_KEY, "sayHello"));
        
        listener.notify(urls);
    }

    /* 
     * @see com.alibaba.dubbo.registry.RegistryService#unsubscribe(com.alibaba.dubbo.common.URL, com.alibaba.dubbo.registry.NotifyListener)
     */
    public void unsubscribe(EURL url, NotifyListener listener) {
        
    }

    /* 
     * @see com.alibaba.dubbo.registry.RegistryService#lookup(com.alibaba.dubbo.common.URL)
     */
    public List<EURL> lookup(EURL url) {
        return null;
    }

}