package com.alibaba.dubbo.remoting.zookeeper;

import java.util.List;

import cn.sunline.ltts.apm.api.registry.base.EURL;

public interface ZookeeperClient {

	void create(String path, boolean ephemeral);

	void delete(String path);

	List<String> getChildren(String path);

	List<String> addChildListener(String path, ChildListener listener);

	void removeChildListener(String path, ChildListener listener);

	void addStateListener(StateListener listener);
	
	void removeStateListener(StateListener listener);

	boolean isConnected();

	void close();

	EURL getUrl();

}
