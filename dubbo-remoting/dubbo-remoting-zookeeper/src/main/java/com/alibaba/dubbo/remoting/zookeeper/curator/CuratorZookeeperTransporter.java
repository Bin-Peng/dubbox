package com.alibaba.dubbo.remoting.zookeeper.curator;

import cn.sunline.ltts.apm.api.registry.base.EURL;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperClient;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperTransporter;

public class CuratorZookeeperTransporter implements ZookeeperTransporter {

	public ZookeeperClient connect(EURL url) {
		return new CuratorZookeeperClient(url);
	}

}
