/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.tsf.gateway.core.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.invoke.MethodHandles;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shade.polaris.org.apache.org.apache.http.HttpEntityEnclosingRequest;
import shade.polaris.org.apache.org.apache.http.HttpHost;
import shade.polaris.org.apache.org.apache.http.HttpRequest;
import shade.polaris.org.apache.org.apache.http.NoHttpResponseException;
import shade.polaris.org.apache.org.apache.http.client.HttpRequestRetryHandler;
import shade.polaris.org.apache.org.apache.http.client.config.RequestConfig;
import shade.polaris.org.apache.org.apache.http.client.methods.CloseableHttpResponse;
import shade.polaris.org.apache.org.apache.http.client.methods.HttpGet;
import shade.polaris.org.apache.org.apache.http.client.methods.HttpPost;
import shade.polaris.org.apache.org.apache.http.client.methods.HttpRequestBase;
import shade.polaris.org.apache.org.apache.http.client.protocol.HttpClientContext;
import shade.polaris.org.apache.org.apache.http.client.utils.URLEncodedUtils;
import shade.polaris.org.apache.org.apache.http.config.Registry;
import shade.polaris.org.apache.org.apache.http.config.RegistryBuilder;
import shade.polaris.org.apache.org.apache.http.conn.ConnectTimeoutException;
import shade.polaris.org.apache.org.apache.http.conn.routing.HttpRoute;
import shade.polaris.org.apache.org.apache.http.conn.socket.ConnectionSocketFactory;
import shade.polaris.org.apache.org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import shade.polaris.org.apache.org.apache.http.conn.socket.PlainConnectionSocketFactory;
import shade.polaris.org.apache.org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import shade.polaris.org.apache.org.apache.http.entity.StringEntity;
import shade.polaris.org.apache.org.apache.http.impl.client.CloseableHttpClient;
import shade.polaris.org.apache.org.apache.http.impl.client.HttpClients;
import shade.polaris.org.apache.org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import shade.polaris.org.apache.org.apache.http.message.BasicNameValuePair;
import shade.polaris.org.apache.org.apache.http.protocol.HttpContext;
import shade.polaris.org.apache.org.apache.http.util.EntityUtils;


/**
 * @ClassName HttpConnectionPoolUtil
 * @Description httpclient连接池工具类
 * @Author vmershen
 * @Date 2019/7/8 11:56
 * @Version 1.0
 */
public final class HttpConnectionPoolUtil {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final int CONNECT_TIMEOUT = HttpConfigConstant.HTTP_CONNECT_TIMEOUT;
	private static final int SOCKET_TIMEOUT = HttpConfigConstant.HTTP_SOCKET_TIMEOUT;
	private static final int MAX_CONN = HttpConfigConstant.HTTP_MAX_POOL_SIZE;
	private static final int MAX_PRE_ROUTE = HttpConfigConstant.HTTP_MAX_POOL_SIZE;
	private static final int MAX_ROUTE = HttpConfigConstant.HTTP_MAX_POOL_SIZE;
	private final static Object syncLock = new Object();
	private volatile static CloseableHttpClient httpClient;
	private static PoolingHttpClientConnectionManager manager;
	private static ScheduledExecutorService monitorExecutor;

	//程序退出时，释放资源
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				closeConnectionPool();
			}
		});
	}

	private HttpConnectionPoolUtil() {

	}

	private static void setRequestConfig(HttpRequestBase httpRequestBase, Integer timeout) {
		RequestConfig requestConfig = RequestConfig
				.custom()
				.setConnectionRequestTimeout(timeout == null ? CONNECT_TIMEOUT : timeout)
				.setConnectTimeout(timeout == null ? CONNECT_TIMEOUT : timeout)
				.setSocketTimeout(timeout == null ? SOCKET_TIMEOUT : timeout)
				.build();
		httpRequestBase.setConfig(requestConfig);
	}

	public static CloseableHttpClient getHttpClient(String url) {
		logger.info("url is : {}", url);
		if (httpClient == null) {
			//多线程下多个线程同时调用getHttpClient容易导致重复创建httpClient对象的问题,所以加上了同步锁
			synchronized (syncLock) {
				if (httpClient == null) {
					httpClient = createHttpClient(url);
					//开启监控线程,对异常和空闲线程进行关闭
					monitorExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("gw-client", true));
					monitorExecutor.scheduleAtFixedRate(new TimerTask() {
						@Override
						public void run() {
							//关闭异常连接
							manager.closeExpiredConnections();
							//关闭5s空闲的连接
							manager.closeIdleConnections(HttpConfigConstant.HTTP_IDLE_TIMEOUT, TimeUnit.MILLISECONDS);
							logger.debug("close expired and idle for over {} ms connection", HttpConfigConstant.HTTP_IDLE_TIMEOUT);
						}
					}, HttpConfigConstant.HTTP_MONITOR_INTERVAL, HttpConfigConstant.HTTP_MONITOR_INTERVAL, TimeUnit.MILLISECONDS);
				}
			}
		}
		return httpClient;
	}

	public static CloseableHttpClient createHttpClient(String url) {
		ConnectionSocketFactory plainSocketFactory = PlainConnectionSocketFactory.getSocketFactory();
		LayeredConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactory.getSocketFactory();
		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", plainSocketFactory)
				.register("https", sslSocketFactory).build();
		manager = new PoolingHttpClientConnectionManager(registry);
		//设置连接参数
		manager.setMaxTotal(MAX_CONN); // 最大连接数
		manager.setDefaultMaxPerRoute(MAX_PRE_ROUTE); // 路由最大连接数
		//HttpHost httpHost = new HttpHost(host, port);
		HttpHost httpHost = new HttpHost(url);
		manager.setMaxPerRoute(new HttpRoute(httpHost), MAX_ROUTE);
		//请求失败时,进行请求重试
		HttpRequestRetryHandler handler = new HttpRequestRetryHandler() {
			@Override
			public boolean retryRequest(IOException e, int i, HttpContext httpContext) {
				if (i > 3) {
					//重试超过3次,放弃请求
					logger.error("retry has more than 3 time, give up request");
					return false;
				}
				if (e instanceof NoHttpResponseException) {
					//服务器没有响应,可能是服务器断开了连接,应该重试
					logger.error("receive no response from server, retry");
					return true;
				}
				if (e instanceof SSLHandshakeException) {
					// SSL握手异常
					logger.error("SSL hand shake exception");
					return false;
				}
				if (e instanceof InterruptedIOException) {
					//超时
					logger.error("InterruptedIOException");
					return false;
				}
				if (e instanceof UnknownHostException) {
					// 服务器不可达
					logger.error("server host unknown");
					return false;
				}
				if (e instanceof ConnectTimeoutException) {
					// 连接超时
					logger.error("Connection Time out");
					return false;
				}
				if (e instanceof SSLException) {
					logger.error("SSLException");
					return false;
				}
				HttpClientContext context = HttpClientContext.adapt(httpContext);
				HttpRequest request = context.getRequest();
				//如果请求不是关闭连接的请求
				return !(request instanceof HttpEntityEnclosingRequest);
			}
		};
		CloseableHttpClient client = HttpClients.custom().setConnectionManager(manager).setRetryHandler(handler)
				.build();
		return client;
	}

	/**
	 * get方式,请求参数以?形式拼接在url后面.
	 *
	 * @param url 请求url
	 * @param paramsMap 请求参数
	 * @param headerParamsMap 传入的header参数
	 * @param timeout 单位毫秒
	 * @return 返回结果
	 */
	public static String httpGet(String url, Map<String, String> paramsMap, Map<String, String> headerParamsMap, Integer timeout) {
		// 数据必填项校验
		if (StringUtils.isEmpty(url)) {
			throw new IllegalArgumentException("url can not be empty");
		}
		CloseableHttpResponse res = null;
		CloseableHttpClient httpClient = getHttpClient(url);
		String result = null;
		String baseUrl = buildUrl(url, paramsMap);
		logger.info("get request: {}", baseUrl);
		HttpGet httpGet = new HttpGet(baseUrl);
		setRequestConfig(httpGet, timeout);
		httpGet.addHeader("Content-type", "application/json; charset=utf-8");
		httpGet.setHeader("Accept", "application/json");
		// 添加传入的header参数
		buildHeaderParams(httpGet, headerParamsMap);
		try {
			res = httpClient.execute(httpGet, HttpClientContext.create());
			result = EntityUtils.toString(res.getEntity());
			logger.info("get response :{}", result);
			if (res.getStatusLine().getStatusCode() != 200) {
				logger.info("response error: {}", result);
				throw new IllegalStateException(String.format("call url: %s failed, response: code[%s] body[%s]",
						baseUrl, res.getStatusLine().getStatusCode(), result));
			}
			return result;
		}
		catch (IOException e) {
			logger.warn("Get request failed, url:" + baseUrl, e);
			throw new RuntimeException(e);
		}
		finally {
			closeHttpResponse(res);
		}
	}

	private static void closeHttpResponse(CloseableHttpResponse res) {
		try {
			if (res != null) {
				res.close();
			}
		}
		catch (IOException e) {
			logger.warn("Close httpClient failed!", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 关闭连接池.
	 */
	public static void closeConnectionPool() {
		try {
			httpClient.close();
			manager.close();
			monitorExecutor.shutdown();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String httpPostWithJSON(String url, Map<String, String> paramsMap, String json, Map<String, String> headerParamsMap, Integer timeout)
			throws Exception {
		// 数据必填项校验
		if (StringUtils.isBlank(url)) {
			throw new Exception("url can't be empty");
		}
		// 数据必填项校验
		if (StringUtils.isBlank(json)) {
			json = "";
		}
		String baseUrl = buildUrl(url, paramsMap);
		String result = null;
		CloseableHttpResponse res = null;
		CloseableHttpClient httpClient = getHttpClient(url);
		HttpPost httpPost = new HttpPost(baseUrl);
		setRequestConfig(httpPost, timeout);
		httpPost.addHeader("Content-type", "application/json; charset=utf-8");
		httpPost.setHeader("Accept", "application/json");
		// 添加传入的header参数
		buildHeaderParams(httpPost, headerParamsMap);

		httpPost.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
		logger.info("post url: {}", url);
		try {
			res = httpClient.execute(httpPost, HttpClientContext.create());
			result = EntityUtils.toString(res.getEntity());
			if (res.getStatusLine().getStatusCode() != 200 && res.getStatusLine().getStatusCode() != 201) {
				logger.info("response error: {}", result);
				throw new IllegalStateException(String.format("call url: %s failed, response: code[%s] body[%s]",
						baseUrl, res.getStatusLine().getStatusCode(), result));
			}
			return result;
		}
		catch (IOException e) {
			logger.warn("Get request failed, url:" + baseUrl, e);
			throw new RuntimeException(e);
		}
		finally {
			closeHttpResponse(res);
		}
	}

	private static String buildUrl(String url, Map<String, String> paramsMap) {
		String baseUrl = null;
		if (paramsMap != null && !paramsMap.isEmpty()) {
			List params = new ArrayList();
			for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				if (value != null) {
					params.add(new BasicNameValuePair(key, value));
				}
			}
			if (url.contains("?")) {
				baseUrl = url + "&" + URLEncodedUtils.format(params, "UTF-8");
			}
			else {
				baseUrl = url + "?" + URLEncodedUtils.format(params, "UTF-8");
			}
		}
		else {
			baseUrl = url;
		}
		return baseUrl;
	}

	private static void buildHeaderParams(HttpRequestBase httpRequestBase, Map<String, String> headerParamsMap) {
		if (null != headerParamsMap) {
			for (Map.Entry<String, String> entry : headerParamsMap.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				httpRequestBase.setHeader(key, value);
			}
		}
	}
}
