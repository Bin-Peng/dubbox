package com.alibaba.dubbo.common.extensionloader.adaptive;

import cn.sunline.ltts.apm.api.registry.base.EURL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * @author ding.lid
 */
@SPI
public interface HasAdaptiveExt {
    @Adaptive
    String echo(EURL url, String s);
}
