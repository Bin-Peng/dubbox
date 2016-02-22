package com.alibaba.dubbo.config.spring.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cn.sunline.ltts.apm.api.registry.base.EURL;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
public class MockRegistryFactory implements RegistryFactory {

    private static final Map<EURL, Registry> registries = new HashMap<EURL, Registry>();

    public Registry getRegistry(EURL url) {
        MockRegistry registry = new MockRegistry(url);
        registries.put(url, registry);
        return registry;
    }

    public static Collection<Registry> getCachedRegistry() {
        return registries.values();
    }

    public static void cleanCachedRegistry() {
        registries.clear();
    }
}
