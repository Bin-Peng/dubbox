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
package com.alibaba.dubbo.remoting.transport.dispatcher.message;

import cn.sunline.ltts.apm.api.registry.base.EURL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Dispatcher;

/**
 * 只有message receive使用线程池.
 * 
 * @author chao.liuc
 */
public class MessageOnlyDispatcher implements Dispatcher {

    public static final String NAME = "message";

    public ChannelHandler dispatch(ChannelHandler handler, EURL url) {
        return new MessageOnlyChannelHandler(handler, url);
    }

}