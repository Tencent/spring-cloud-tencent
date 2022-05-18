package com.tencent.cloud.polaris.registry.filter;


public interface RegisterFilterHandler {

	boolean beforeInvoke(RegisterFilterHandlerContext registerFilterHandlerContext);

	boolean afterInvoke(RegisterFilterHandlerContext registerFilterHandlerContext);

	boolean isSingleInstance();
}
