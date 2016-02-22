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
package com.alibaba.dubbo.remoting.p2p.support;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.sunline.ltts.apm.api.registry.base.EURL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Server;
import com.alibaba.dubbo.remoting.Transporters;
import com.alibaba.dubbo.remoting.p2p.Group;
import com.alibaba.dubbo.remoting.p2p.Peer;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerDispatcher;

/**
 * AbstractGroup
 * 
 * @author william.liangf
 */
public abstract class AbstractGroup implements Group {

    // 日志输出
    protected static final Logger logger = LoggerFactory.getLogger(AbstractGroup.class);
    
    protected final EURL url;
    
    protected final Map<EURL, Server> servers = new ConcurrentHashMap<EURL, Server>();

    protected final Map<EURL, Client> clients = new ConcurrentHashMap<EURL, Client>();
    
    protected final ChannelHandlerDispatcher dispatcher = new ChannelHandlerDispatcher();

    public AbstractGroup(EURL url){
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
    }
    
    public EURL getUrl() {
        return url;
    }

    public void close() {
        for (EURL url : new ArrayList<EURL>(servers.keySet())) {
            try {
                leave(url);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }
        for (EURL url : new ArrayList<EURL>(clients.keySet())) {
            try {
                disconnect(url);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }
    }
    
    public Peer join(EURL url, ChannelHandler handler) throws RemotingException {
        Server server = servers.get(url);
        if (server == null) { // TODO 有并发间隙
            server = Transporters.bind(url, handler);
            servers.put(url, server);
            dispatcher.addChannelHandler(handler);
        }
        return new ServerPeer(server, clients, this);
    }

    public void leave(EURL url) throws RemotingException {
        Server server = servers.remove(url);
        if (server != null) {
            server.close();
        }
    }

    protected Client connect(EURL url) throws RemotingException {
        if (servers.containsKey(url)) {
            return null;
        }
        Client client = clients.get(url);
        if (client == null) { // TODO 有并发间隙
            client = Transporters.connect(url, dispatcher);
            clients.put(url, client);
        }
        return client;
    }

    protected void disconnect(EURL url) throws RemotingException {
        Client client = clients.remove(url);
        if (client != null) {
            client.close();
        }
    }

}