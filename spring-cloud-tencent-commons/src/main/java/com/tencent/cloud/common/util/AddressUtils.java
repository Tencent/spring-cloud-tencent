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

package com.tencent.cloud.common.util;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * the utils of parse address.
 *
 * @author lepdou 2022-03-29
 */
public final class AddressUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(AddressUtils.class);

	private static final String ADDRESS_SEPARATOR = ",";

	private final static Boolean hasIpv6Address;

	static {
		hasIpv6Address = hasIpv6Address();
	}

	private AddressUtils() {
	}

	public static List<String> parseAddressList(String addressInfo) {
		if (StringUtils.isBlank(addressInfo)) {
			return Collections.emptyList();
		}
		List<String> addressList = new ArrayList<>();
		String[] addresses = addressInfo.split(ADDRESS_SEPARATOR);
		for (String address : addresses) {
			URI uri = URI.create(address.trim());
			addressList.add(uri.getAuthority());
		}
		return addressList;
	}

	public static List<String> parseHostPortList(String addressInfo) {
		if (StringUtils.isBlank(addressInfo)) {
			return Collections.emptyList();
		}
		String[] addresses = addressInfo.split(ADDRESS_SEPARATOR);
		List<String> addressList = new ArrayList<>();
		for (String address : addresses) {
			addressList.add(address.trim());
		}
		return addressList;
	}

	public static boolean accessible(String ip, int port, int timeout) {
		Socket socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(InetAddress.getByName(ip), port), timeout);
		}
		catch (IOException e) {
			return false;
		}
		finally {
			try {
				socket.close();
			}
			catch (IOException e) {
				LOGGER.error("Close socket connection failed.", e);
			}
		}
		return true;
	}

	public static boolean preferIpv6() {
		if (Boolean.FALSE.equals(hasIpv6Address)) {
			LOGGER.debug("AddressUtils.preferIpv6 hasIpv6Address = false");
			return false;
		}
		if ("true".equalsIgnoreCase(System.getenv("tsf_prefer_ipv6"))) {
			LOGGER.debug("AddressUtils.preferIpv6 System.getenv = true");
			return true;
		}
		if ("true".equalsIgnoreCase(System.getProperty("tsf_prefer_ipv6"))) {
			LOGGER.debug("AddressUtils.preferIpv6 System.getProperty = true");
			return true;
		}
		if ("true".equalsIgnoreCase(BeanFactoryUtils.resolve("${tsf_prefer_ipv6}"))) {
			LOGGER.debug("AddressUtils.preferIpv6 BeanFactoryUtils.resolve = true");
			return true;
		}
		LOGGER.debug("AddressUtils.preferIpv6 result = false");
		return false;
	}

	/**
	 * Determine whether environment has an ipv6 address.
	 */
	private static boolean hasIpv6Address() {
		InetAddress result = null;
		try {
			int lowest = Integer.MAX_VALUE;
			for (Enumeration<NetworkInterface> nics = NetworkInterface
					.getNetworkInterfaces(); nics.hasMoreElements(); ) {
				NetworkInterface ifc = nics.nextElement();
				if (ifc.isUp()) {
					LOGGER.trace("Testing interface: " + ifc.getDisplayName());
					if (ifc.getIndex() < lowest || result == null) {
						lowest = ifc.getIndex();
					}
					else if (result != null) {
						continue;
					}

					for (Enumeration<InetAddress> addrs = ifc
							.getInetAddresses(); addrs.hasMoreElements(); ) {
						InetAddress address = addrs.nextElement();
						if (address instanceof Inet6Address
								&& !address.isLinkLocalAddress()
								&& !address.isLoopbackAddress()) {
							LOGGER.trace("Found non-loopback interface: " + ifc.getDisplayName());
							return true;
						}
					}
				}
			}
		}
		catch (IOException ex) {
			LOGGER.error("Cannot get first non-loopback address", ex);
		}
		return false;
	}
}
