package com.alibaba.dubbo.remoting.zookeeper.zkclient;

import cn.sunline.ltts.apm.api.registry.base.EURL;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperClient;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperTransporter;

public class ZkclientZookeeperTransporter implements ZookeeperTransporter {

	public ZookeeperClient connect(EURL url) {
		return new ZkclientZookeeperClient(url);
	}

}
