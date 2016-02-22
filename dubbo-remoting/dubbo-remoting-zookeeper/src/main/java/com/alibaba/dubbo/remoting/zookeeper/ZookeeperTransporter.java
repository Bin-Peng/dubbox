package com.alibaba.dubbo.remoting.zookeeper;

import com.alibaba.dubbo.common.Constants;
import cn.sunline.ltts.apm.api.registry.base.EURL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

@SPI("zkclient")
public interface ZookeeperTransporter {

	@Adaptive({Constants.CLIENT_KEY, Constants.TRANSPORTER_KEY})
	ZookeeperClient connect(EURL url);

}
