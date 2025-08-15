/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.cloud.tsf.demo.consumer.controller;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.tsf.faulttolerance.annotation.TsfFaultTolerance;
import org.springframework.cloud.tsf.faulttolerance.model.TsfFaultToleranceStragety;
import org.springframework.tsf.core.TsfContext;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class SdkBaseTest {

	private static final Logger LOG = LoggerFactory.getLogger(SdkBaseTest.class);

	@Autowired
	private RestTemplate restTemplate;

	private final AtomicInteger failOverCount = new AtomicInteger(0);

	@RequestMapping(value = "/echo-once/{str}", method = RequestMethod.GET)
	public String restOnceProvider(@PathVariable String str, @RequestParam(required = false) String tagName, @RequestParam(required = false) String tagValue) {
		if (!StringUtils.isEmpty(tagName)) {
			TsfContext.putTag(tagName, tagValue);
		}
		LOG.info("start call echo-once");
		String result = restTemplate.getForObject("http://provider-demo/echo/" + str, String.class);
		LOG.info("end call echo-once, the result is : " + result);
		return result;
	}

	@TsfFaultTolerance(strategy = TsfFaultToleranceStragety.FAIL_FAST, fallbackMethod = "restProviderFailfast")
	@RequestMapping(value = "/failfast/{str}", method = RequestMethod.GET)
	public String failFast(@PathVariable String str) {
		LOG.info("start call failfast:" + str);
		String result = restTemplate.getForObject("http://provider-demo/error/" + str, String.class);
		LOG.info("end call failfast-no-:" + str);
		return result;
	}

	public String restProviderFailfast(String str) {
		String result = "failfast-recall:" + str;
		LOG.info(result);
		return result;
	}


	@TsfFaultTolerance(strategy = TsfFaultToleranceStragety.FAIL_FAST, fallbackMethod = "restProviderFallback")
	@RequestMapping(value = "/fallback/{str}", method = RequestMethod.GET)
	public String fallBack(@PathVariable String str) {
		LOG.info("start call fallback:" + str);
		String result = restTemplate.getForObject("http://provider-demo/error/" + str, String.class);
		LOG.info("end call fallback-no:" + result);
		return result;
	}

	public String restProviderFallback(String str) {
		String result = "fallback-recall:" + str;
		LOG.info(result);
		return result;
	}

	// The call failed. Fault tolerance retry twice. A total of three calls
	@TsfFaultTolerance(strategy = TsfFaultToleranceStragety.FAIL_OVER, maxAttempts = 2, fallbackMethod = "restProviderFailover")
	@RequestMapping(value = "/failover/{str}", method = RequestMethod.GET)
	public String failOver(@PathVariable String str) {
		LOG.info("start call failover:" + str);
		String result = restTemplate.getForObject("http://provider-demo/error/" + str, String.class);
		LOG.info("end call failover-no:" + str);
		return result;
	}

	public String restProviderFailover(String str) {
		String result = "failover-recall:" + str;
		LOG.info(result);
		return result;
	}

	// Ignore the RuntimeException exception, do not trigger fault tolerance, and retry will fail
	@TsfFaultTolerance(strategy = TsfFaultToleranceStragety.FAIL_OVER, maxAttempts = 2, ignoreExceptions = {RuntimeException.class}, fallbackMethod = "restProviderFailoverException")
	@RequestMapping(value = "/failover-exception/{str}", method = RequestMethod.GET)
	public String failOverException(@PathVariable String str) throws InterruptedException {
		String result = "";
		LOG.info("start call failover-exception:" + str);
		result = restTemplate.getForObject("http://provider-demo/error/" + str, String.class);
		LOG.info("end call failover with exception:" + str);
		return result;
	}

	public String restProviderFailoverException(String str) {
		String result = "failover-exception-recall:" + str;
		LOG.info(result);
		return result;
	}


	@TsfFaultTolerance(strategy = TsfFaultToleranceStragety.FAIL_OVER, maxAttempts = 2, ignoreExceptions = {RuntimeException.class},
			raisedExceptions = {Exception.class}, fallbackMethod = "restProviderFallback")
	@RequestMapping(value = "/raisedException/{exceptionType}", method = RequestMethod.GET)
	public String raisedException(@PathVariable String exceptionType) throws IOException, RuntimeException, TimeoutException {
		String result = "";
//		LOG.info("start call raisedException:" + exceptionType);
		switch (exceptionType) {
			case "RuntimeException":
				LOG.info("Test failOver for raised RuntimeException");
				throw new RuntimeException("NO");

			case "TimeOutException":
				LOG.info("Test failOver for raised TimeOutException");
				if (failOverCount.incrementAndGet() % 3 == 0) {
					return "failOver success,failOverCount=" + failOverCount.get();
				}
				throw new TimeoutException("NO");
			default:
				LOG.info("Test failOver for raised otherException");
				if (failOverCount.incrementAndGet() % 3 == 0) {
					return "failOver success,failOverCount=" + failOverCount.get();
				}
				throw new IOException("NO");
		}

	}
}
