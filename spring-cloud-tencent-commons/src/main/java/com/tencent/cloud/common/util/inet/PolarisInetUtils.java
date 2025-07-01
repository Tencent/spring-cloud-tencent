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

package com.tencent.cloud.common.util.inet;

import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.tencent.cloud.common.util.AddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;


/**
 * Extend from {@link InetUtils}.
 *
 * @author Haotian Zhang
 */
public class PolarisInetUtils implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(PolarisInetUtils.class);
	// TODO: maybe shutdown the thread pool if it isn't being used?
	private final ExecutorService executorService;
	private final InetUtilsProperties properties;

	public PolarisInetUtils(final InetUtilsProperties properties) {
		this.properties = properties;
		this.executorService = Executors
				.newSingleThreadExecutor(r -> {
					Thread thread = new Thread(r);
					thread.setName(InetUtilsProperties.PREFIX);
					thread.setDaemon(true);
					return thread;
				});
	}

	public static String getIpString(boolean _ipv6) {
		InetAddress result = null;
		try {
			int lowest = Integer.MAX_VALUE;
			for (Enumeration<NetworkInterface> nics = NetworkInterface
					.getNetworkInterfaces(); nics.hasMoreElements(); ) {
				NetworkInterface ifc = nics.nextElement();
				if (ifc.isUp()) {
					logger.trace("Testing interface: " + ifc.getDisplayName());
					if (ifc.getIndex() < lowest || result == null) {
						lowest = ifc.getIndex();
					}
					else if (result != null) {
						continue;
					}
					for (Enumeration<InetAddress> addrs = ifc
							.getInetAddresses(); addrs.hasMoreElements(); ) {
						InetAddress address = addrs.nextElement();
						if (_ipv6) {
							if (address instanceof Inet6Address
									&& !address.isLinkLocalAddress()
									&& !address.isLoopbackAddress()
							) {
								logger.trace("Found non-loopback interface: "
										+ ifc.getDisplayName());
								result = address;
							}
						}
						else {
							if (address instanceof Inet4Address
									&& !address.isLoopbackAddress()
							) {
								logger.trace("Found non-loopback interface: "
										+ ifc.getDisplayName());
								result = address;
							}
						}
					}
				}
			}
		}
		catch (IOException ex) {
			logger.error("Cannot get first non-loopback address", ex);
		}

		if (result == null) {
			return null;
		}

		if (result.getHostAddress().contains("%")) {
			return result.getHostAddress().split("%")[0];
		}
		else {
			return result.getHostAddress();
		}
	}

	@Override
	public void close() {
		executorService.shutdown();
	}

	public InetUtils.HostInfo findFirstNonLoopbackHostInfo() {
		InetAddress address = findFirstNonLoopbackAddress();
		if (address != null) {
			return convertAddress(address);
		}
		InetUtils.HostInfo hostInfo = new InetUtils.HostInfo();
		hostInfo.setHostname(this.properties.getDefaultHostname());
		hostInfo.setIpAddress(this.properties.getDefaultIpAddress());
		return hostInfo;
	}

	public InetAddress findFirstNonLoopbackAddress() {
		boolean preferIpv6 = AddressUtils.preferIpv6();
		InetAddress result = findFirstNonLoopbackAddressByIpType(preferIpv6);
		logger.debug("ipv6 before, preferIpv6:{}, result:{}", preferIpv6, result);
		if (result == null) {
			result = findFirstNonLoopbackAddressByIpType(!preferIpv6);
		}
		logger.debug("ipv6 after, preferIpv6:{}, result:{}", preferIpv6, result);

		if (result != null) {
			return result;
		}

		try {
			return InetAddress.getLocalHost();
		}
		catch (UnknownHostException e) {
			logger.warn("Unable to retrieve localhost");
		}

		return null;
	}

	/** for testing. */
	boolean isPreferredAddress(InetAddress address) {

		if (this.properties.isUseOnlySiteLocalInterfaces()) {
			final boolean siteLocalAddress = address.isSiteLocalAddress();
			if (!siteLocalAddress) {
				logger.trace("Ignoring address: " + address.getHostAddress());
			}
			return siteLocalAddress;
		}
		final List<String> preferredNetworks = this.properties.getPreferredNetworks();
		if (preferredNetworks.isEmpty()) {
			return true;
		}
		for (String regex : preferredNetworks) {
			final String hostAddress = address.getHostAddress();
			if (hostAddress.matches(regex) || hostAddress.startsWith(regex)) {
				return true;
			}
		}
		logger.trace("Ignoring address: " + address.getHostAddress());
		return false;
	}

	/** for testing. */
	boolean ignoreInterface(String interfaceName) {
		for (String regex : this.properties.getIgnoredInterfaces()) {
			if (interfaceName.matches(regex)) {
				logger.trace("Ignoring interface: " + interfaceName);
				return true;
			}
		}
		return false;
	}

	public InetUtils.HostInfo convertAddress(final InetAddress address) {
		InetUtils.HostInfo hostInfo = new InetUtils.HostInfo();
		Future<String> result = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return address.getHostName();
			}
		});

		String hostname;
		try {
			hostname = result.get(this.properties.getTimeoutSeconds(), TimeUnit.SECONDS);
		}
		catch (Exception e) {
			logger.info("Cannot determine local hostname");
			hostname = "localhost";
		}
		if (hostname.contains("%")) {
			hostInfo.setHostname(hostname.split("%")[0]);
		}
		else {
			hostInfo.setHostname(hostname);
		}
		if (address.getHostAddress().contains("%")) {
			hostInfo.setIpAddress(address.getHostAddress().split("%")[0]);
		}
		else {
			hostInfo.setIpAddress(address.getHostAddress());
		}
		return hostInfo;
	}

	public String findIpInterface() {
		InetAddress address = findFirstNonLoopbackAddress();
		if (address.getHostAddress().contains("%")) {
			return address.getHostAddress().split("%")[1];
		}
		return "";
	}

	public String findIpAddress() {
		InetAddress address = findFirstNonLoopbackAddress();
		return address.getHostAddress().split("%")[0];
	}

	/**
	 *
	 * @return  "[ipv6]"
	 */
	public String findIpAddressWithBracket() {
		InetAddress address = findFirstNonLoopbackAddress();
		if (address.getHostAddress().contains("%")) {
			return address.getHostAddress().split("%")[0];
		}
		return address.getHostAddress();
	}

	/**
	 * @return ipv6%eth0
	 *
	 */
	public String findIpAddressAndInterface() {
		InetAddress address = findFirstNonLoopbackAddress();
		return address.getHostAddress();
	}

	public InetAddress findFirstNonLoopbackAddressByIpType(boolean _ipv6) {
		InetAddress result = null;
		try {
			int lowest = Integer.MAX_VALUE;
			for (Enumeration<NetworkInterface> nics = NetworkInterface
					.getNetworkInterfaces(); nics.hasMoreElements(); ) {
				NetworkInterface ifc = nics.nextElement();
				if (ifc.isUp()) {
					logger.trace("Testing interface: " + ifc.getDisplayName());
					if (ifc.getIndex() < lowest || result == null) {
						lowest = ifc.getIndex();
					}
					else if (result != null) {
						continue;
					}

					// @formatter:off
					if (!ignoreInterface(ifc.getDisplayName())) {
						for (Enumeration<InetAddress> addrs = ifc
								.getInetAddresses(); addrs.hasMoreElements();) {
							InetAddress address = addrs.nextElement();
							if (_ipv6) {
								if (address instanceof Inet6Address
										&& !address.isLinkLocalAddress()
										&& !address.isLoopbackAddress()
										&& isPreferredAddress(address)) {
									logger.trace("Found non-loopback interface: "
											+ ifc.getDisplayName());
									result = address;
								}
							}
							else {
								if (address instanceof Inet4Address
										&& !address.isLoopbackAddress()
										&& isPreferredAddress(address)) {
									logger.trace("Found non-loopback interface: "
											+ ifc.getDisplayName());
									result = address;
								}
							}
						}
					}
					// @formatter:on
				}
			}
		}
		catch (IOException ex) {
			logger.error("Cannot get first non-loopback address", ex);
		}

		if (result != null) {
			return result;
		}


		return null;
	}
}
