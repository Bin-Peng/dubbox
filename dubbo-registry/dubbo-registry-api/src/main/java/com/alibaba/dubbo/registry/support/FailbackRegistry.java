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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.alibaba.dubbo.common.Constants;
import cn.sunline.ltts.apm.api.registry.base.EURL;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.registry.NotifyListener;

/**
 * FailbackRegistry. (SPI, Prototype, ThreadSafe)
 * 
 * @author william.liangf
 */
public abstract class FailbackRegistry extends AbstractRegistry {

    // 定时任务执行器
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("DubboRegistryFailedRetryTimer", true));

    // 失败重试定时器，定时检查是否有请求失败，如有，无限次重试
    private final ScheduledFuture<?> retryFuture;

    private final Set<EURL> failedRegistered = new ConcurrentHashSet<EURL>();

    private final Set<EURL> failedUnregistered = new ConcurrentHashSet<EURL>();

    private final ConcurrentMap<EURL, Set<NotifyListener>> failedSubscribed = new ConcurrentHashMap<EURL, Set<NotifyListener>>();

    private final ConcurrentMap<EURL, Set<NotifyListener>> failedUnsubscribed = new ConcurrentHashMap<EURL, Set<NotifyListener>>();

    private final ConcurrentMap<EURL, Map<NotifyListener, List<EURL>>> failedNotified = new ConcurrentHashMap<EURL, Map<NotifyListener, List<EURL>>>();

    public FailbackRegistry(EURL url) {
        super(url);
        int retryPeriod = url.getParameter(Constants.REGISTRY_RETRY_PERIOD_KEY, Constants.DEFAULT_REGISTRY_RETRY_PERIOD);
        this.retryFuture = retryExecutor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                // 检测并连接注册中心
                try {
                    retry();
                } catch (Throwable t) { // 防御性容错
                    logger.error("Unexpected error occur at failed retry, cause: " + t.getMessage(), t);
                }
            }
        }, retryPeriod, retryPeriod, TimeUnit.MILLISECONDS);
    }

    public Future<?> getRetryFuture() {
        return retryFuture;
    }

    public Set<EURL> getFailedRegistered() {
        return failedRegistered;
    }

    public Set<EURL> getFailedUnregistered() {
        return failedUnregistered;
    }

    public Map<EURL, Set<NotifyListener>> getFailedSubscribed() {
        return failedSubscribed;
    }

    public Map<EURL, Set<NotifyListener>> getFailedUnsubscribed() {
        return failedUnsubscribed;
    }

    public Map<EURL, Map<NotifyListener, List<EURL>>> getFailedNotified() {
        return failedNotified;
    }

    private void addFailedSubscribed(EURL url, NotifyListener listener) {
        Set<NotifyListener> listeners = failedSubscribed.get(url);
        if (listeners == null) {
            failedSubscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = failedSubscribed.get(url);
        }
        listeners.add(listener);
    }

    private void removeFailedSubscribed(EURL url, NotifyListener listener) {
        Set<NotifyListener> listeners = failedSubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
        listeners = failedUnsubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
        Map<NotifyListener, List<EURL>> notified = failedNotified.get(url);
        if (notified != null) {
            notified.remove(listener);
        }
    }

    @Override
    public void register(EURL url) {
        super.register(url);
        failedRegistered.remove(url);
        failedUnregistered.remove(url);
        try {
            // 向服务器端发送注册请求
            doRegister(url);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true)
                    && ! Constants.CONSUMER_PROTOCOL.equals(url.getProtocol());
            boolean skipFailback = t instanceof SkipFailbackWrapperException;
            if (check || skipFailback) {
                if(skipFailback) {
                    t = t.getCause();
                }
                throw new IllegalStateException("Failed to register " + url + " to registry " + getUrl().getAddress() + ", cause: " + t.getMessage(), t);
            } else {
                logger.error("Failed to register " + url + ", waiting for retry, cause: " + t.getMessage(), t);
            }

            // 将失败的注册请求记录到失败列表，定时重试
            failedRegistered.add(url);
        }
    }

    @Override
    public void unregister(EURL url) {
        super.unregister(url);
        failedRegistered.remove(url);
        failedUnregistered.remove(url);
        try {
            // 向服务器端发送取消注册请求
            doUnregister(url);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true)
                    && ! Constants.CONSUMER_PROTOCOL.equals(url.getProtocol());
            boolean skipFailback = t instanceof SkipFailbackWrapperException;
            if (check || skipFailback) {
                if(skipFailback) {
                    t = t.getCause();
                }
                throw new IllegalStateException("Failed to unregister " + url + " to registry " + getUrl().getAddress() + ", cause: " + t.getMessage(), t);
            } else {
                logger.error("Failed to uregister " + url + ", waiting for retry, cause: " + t.getMessage(), t);
            }

            // 将失败的取消注册请求记录到失败列表，定时重试
            failedUnregistered.add(url);
        }
    }

    @Override
    public void subscribe(EURL url, NotifyListener listener) {
        super.subscribe(url, listener);
        removeFailedSubscribed(url, listener);
        try {
            // 向服务器端发送订阅请求
            doSubscribe(url, listener);
        } catch (Exception e) {
            Throwable t = e;

            List<EURL> urls = getCacheUrls(url);
            if (urls != null && urls.size() > 0) {
                notify(url, listener, urls);
                logger.error("Failed to subscribe " + url + ", Using cached list: " + urls + " from cache file: " + getUrl().getParameter(Constants.FILE_KEY, System.getProperty("user.home") + "/dubbo-registry-" + url.getHost() + ".cache") + ", cause: " + t.getMessage(), t);
            } else {
                // 如果开启了启动时检测，则直接抛出异常
                boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                        && url.getParameter(Constants.CHECK_KEY, true);
                boolean skipFailback = t instanceof SkipFailbackWrapperException;
                if (check || skipFailback) {
                    if(skipFailback) {
                        t = t.getCause();
                    }
                    throw new IllegalStateException("Failed to subscribe " + url + ", cause: " + t.getMessage(), t);
                } else {
                    logger.error("Failed to subscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
                }
            }

            // 将失败的订阅请求记录到失败列表，定时重试
            addFailedSubscribed(url, listener);
        }
    }

    @Override
    public void unsubscribe(EURL url, NotifyListener listener) {
        super.unsubscribe(url, listener);
        removeFailedSubscribed(url, listener);
        try {
            // 向服务器端发送取消订阅请求
            doUnsubscribe(url, listener);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true);
            boolean skipFailback = t instanceof SkipFailbackWrapperException;
            if (check || skipFailback) {
                if(skipFailback) {
                    t = t.getCause();
                }
                throw new IllegalStateException("Failed to unsubscribe " + url + " to registry " + getUrl().getAddress() + ", cause: " + t.getMessage(), t);
            } else {
                logger.error("Failed to unsubscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
            }

            // 将失败的取消订阅请求记录到失败列表，定时重试
            Set<NotifyListener> listeners = failedUnsubscribed.get(url);
            if (listeners == null) {
                failedUnsubscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
                listeners = failedUnsubscribed.get(url);
            }
            listeners.add(listener);
        }
    }

    @Override
    protected void notify(EURL url, NotifyListener listener, List<EURL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        try {
        	doNotify(url, listener, urls);
        } catch (Exception t) {
            // 将失败的通知请求记录到失败列表，定时重试
            Map<NotifyListener, List<EURL>> listeners = failedNotified.get(url);
            if (listeners == null) {
                failedNotified.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, List<EURL>>());
                listeners = failedNotified.get(url);
            }
            listeners.put(listener, urls);
            logger.error("Failed to notify for subscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
        }
    }
    
    protected void doNotify(EURL url, NotifyListener listener, List<EURL> urls) {
    	super.notify(url, listener, urls);
    }
    
    @Override
    protected void recover() throws Exception {
        // register
        Set<EURL> recoverRegistered = new HashSet<EURL>(getRegistered());
        if (! recoverRegistered.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover register url " + recoverRegistered);
            }
            for (EURL url : recoverRegistered) {
                failedRegistered.add(url);
            }
        }
        // subscribe
        Map<EURL, Set<NotifyListener>> recoverSubscribed = new HashMap<EURL, Set<NotifyListener>>(getSubscribed());
        if (! recoverSubscribed.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover subscribe url " + recoverSubscribed.keySet());
            }
            for (Map.Entry<EURL, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                EURL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    addFailedSubscribed(url, listener);
                }
            }
        }
    }

    // 重试失败的动作
    protected void retry() {
        if (! failedRegistered.isEmpty()) {
            Set<EURL> failed = new HashSet<EURL>(failedRegistered);
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry register " + failed);
                }
                try {
                    for (EURL url : failed) {
                        try {
                            doRegister(url);
                            failedRegistered.remove(url);
                        } catch (Throwable t) { // 忽略所有异常，等待下次重试
                            logger.warn("Failed to retry register " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry register " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        if(! failedUnregistered.isEmpty()) {
            Set<EURL> failed = new HashSet<EURL>(failedUnregistered);
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry unregister " + failed);
                }
                try {
                    for (EURL url : failed) {
                        try {
                            doUnregister(url);
                            failedUnregistered.remove(url);
                        } catch (Throwable t) { // 忽略所有异常，等待下次重试
                            logger.warn("Failed to retry unregister  " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry unregister  " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        if (! failedSubscribed.isEmpty()) {
            Map<EURL, Set<NotifyListener>> failed = new HashMap<EURL, Set<NotifyListener>>(failedSubscribed);
            for (Map.Entry<EURL, Set<NotifyListener>> entry : new HashMap<EURL, Set<NotifyListener>>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry subscribe " + failed);
                }
                try {
                    for (Map.Entry<EURL, Set<NotifyListener>> entry : failed.entrySet()) {
                        EURL url = entry.getKey();
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            try {
                                doSubscribe(url, listener);
                                listeners.remove(listener);
                            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                                logger.warn("Failed to retry subscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry subscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        if (! failedUnsubscribed.isEmpty()) {
            Map<EURL, Set<NotifyListener>> failed = new HashMap<EURL, Set<NotifyListener>>(failedUnsubscribed);
            for (Map.Entry<EURL, Set<NotifyListener>> entry : new HashMap<EURL, Set<NotifyListener>>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry unsubscribe " + failed);
                }
                try {
                    for (Map.Entry<EURL, Set<NotifyListener>> entry : failed.entrySet()) {
                        EURL url = entry.getKey();
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            try {
                                doUnsubscribe(url, listener);
                                listeners.remove(listener);
                            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                                logger.warn("Failed to retry unsubscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry unsubscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        if (! failedNotified.isEmpty()) {
            Map<EURL, Map<NotifyListener, List<EURL>>> failed = new HashMap<EURL, Map<NotifyListener, List<EURL>>>(failedNotified);
            for (Map.Entry<EURL, Map<NotifyListener, List<EURL>>> entry : new HashMap<EURL, Map<NotifyListener, List<EURL>>>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry notify " + failed);
                }
                try {
                    for (Map<NotifyListener, List<EURL>> values : failed.values()) {
                        for (Map.Entry<NotifyListener, List<EURL>> entry : values.entrySet()) {
                            try {
                                NotifyListener listener = entry.getKey();
                                List<EURL> urls = entry.getValue();
                                listener.notify(urls);
                                values.remove(listener);
                            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                                logger.warn("Failed to retry notify " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry notify " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            retryFuture.cancel(true);
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    // ==== 模板方法 ====

    protected abstract void doRegister(EURL url);

    protected abstract void doUnregister(EURL url);

    protected abstract void doSubscribe(EURL url, NotifyListener listener);

    protected abstract void doUnsubscribe(EURL url, NotifyListener listener);

}