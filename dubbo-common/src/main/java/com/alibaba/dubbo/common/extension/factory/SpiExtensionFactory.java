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
package com.alibaba.dubbo.common.extension.factory;

import com.alibaba.dubbo.common.extension.ExtensionFactory;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.extension.SPI;
//import cn.sunline.ltts.apm.api.registry.base.extension.SPI;

/**
 * SpiExtensionFactory
 * 
 * @author william.liangf
 */
public class SpiExtensionFactory implements ExtensionFactory {

    public <T> T getExtension(Class<T> type, String name) {
        if (type.isInterface() && (type.isAnnotationPresent(SPI.class) || type.isAnnotationPresent(cn.sunline.ltts.apm.api.registry.base.extension.SPI.class))) {
        	if(type.isAnnotationPresent(SPI.class)){
        		ExtensionLoader<T> loader = ExtensionLoader.getExtensionLoader(type);
        		if (loader.getSupportedExtensions().size() > 0) {
                    return loader.getAdaptiveExtension();
                }
        	}
        	/**
        	 * sunline-mod  PB 20150218  兼容使用注册中心时apm-api的SPI注册机制
        	 * */
        	else if(type.isAnnotationPresent(cn.sunline.ltts.apm.api.registry.base.extension.SPI.class)){
        		cn.sunline.ltts.apm.api.registry.base.extension.ExtensionLoader<T> loader = cn.sunline.ltts.apm.api.registry.base.extension.ExtensionLoader.getExtensionLoader(type);
        		if (loader.getSupportedExtensions().size() > 0) {
                    return loader.getAdaptiveExtension();
                }
        	}
            
            
        }
        return null;
    }

}
