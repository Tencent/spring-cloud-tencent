package com.tencent.cloud.polaris.registry.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultRegisterFilterHandler implements RegisterFilterHandler {
	private static final Logger log = LoggerFactory
			.getLogger(DefaultRegisterFilterHandler.class);

	@Override
	public boolean beforeInvoke(RegisterFilterHandlerContext registerFilterHandlerContext) {
		log.info("defaultRegisterFilterHandler beforeInvoke info");
		return true;
	}

	@Override
	public boolean afterInvoke(RegisterFilterHandlerContext registerFilterHandlerContext) {
		log.info("defaultRegisterFilterHandler beforeInvoke afterInvoke");
		return true;
	}

	@Override
	public boolean isSingleInstance() {
		return true;
	}
}
