package com.alibaba.dubbo.config.spring.registry;

import java.util.ArrayList;
import java.util.List;

import cn.sunline.ltts.apm.api.registry.base.EURL;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
public class MockRegistry implements Registry {

    private EURL url;

    private List<EURL> registered = new ArrayList<EURL>();

    private List<EURL> subscribered = new ArrayList<EURL>();

    public List<EURL> getRegistered() {
        return registered;
    }

    public List<EURL> getSubscribered() {
        return subscribered;
    }

    public MockRegistry(EURL url) {
        if (url == null) {
            throw new NullPointerException();
        }
        this.url = url;
    }

    public EURL getUrl() {
        return url;
    }

    public boolean isAvailable() {
        return true;
    }

    public void destroy() {

    }

    public void register(EURL url) {
        registered.add(url);
    }

    public void unregister(EURL url) {
        registered.remove(url);
    }

    public void subscribe(EURL url, NotifyListener listener) {
        subscribered.add(url);
    }

    public void unsubscribe(EURL url, NotifyListener listener) {
        subscribered.remove(url);
    }

    public List<EURL> lookup(EURL url) {
        return null;
    }
}
