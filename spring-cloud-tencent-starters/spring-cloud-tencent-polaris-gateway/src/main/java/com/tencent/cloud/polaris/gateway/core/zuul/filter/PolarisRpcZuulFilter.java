/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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
package com.tencent.cloud.polaris.gateway.core.zuul.filter;

import com.netflix.client.ClientException;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.tencent.cloud.metadata.constant.MetadataConstant;
import com.tencent.cloud.metadata.context.MetadataContextHolder;
import com.tencent.cloud.polaris.gateway.core.zuul.discovery.PolarisProviderDiscovery;
import com.tencent.cloud.polaris.ratelimit.utils.Consts;
import com.tencent.polaris.api.pojo.Instance;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.Host;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author: hongyihui
 */
public class PolarisRpcZuulFilter extends ZuulFilter implements ApplicationListener<EnvironmentChangeEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(PolarisRpcZuulFilter.class);

    private final Timer connectionManagerTimer = new Timer("PolarisRpcZuulFilter.connectionManagerTimer", true);

    private boolean sslHostnameValidationEnabled;

    private boolean forceOriginalQueryStringEncoding;

    private ProxyRequestHelper helper;

    private ZuulProperties properties;

    private Host hostProperties;

    private ApacheHttpClientConnectionManagerFactory connectionManagerFactory;

    private ApacheHttpClientFactory httpClientFactory;

    private HttpClientConnectionManager connectionManager;

    private CloseableHttpClient httpClient;

    private boolean useServlet31 = true;

    private PolarisProviderDiscovery polarisProviderDiscovery;

    @Override
    @SuppressWarnings("Deprecation")
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        onPropertyChange(event);
    }

    @Deprecated
    public void onPropertyChange(EnvironmentChangeEvent event) {
        boolean createNewClient = false;

        for (String key : event.getKeys()) {
            if (key.startsWith("zuul.host.")) {
                createNewClient = true;
                break;
            }
        }

        if (createNewClient) {
            try {
                this.httpClient.close();
            } catch (IOException ex) {
                LOG.error("error closing client", ex);
            }
            // Re-create connection manager (may be shut down on HTTP client close)
            try {
                this.connectionManager.shutdown();
            } catch (RuntimeException ex) {
                LOG.error("error shutting down connection manager", ex);
            }

            this.connectionManager = newConnectionManager();
            this.httpClient = newClient();
        }
    }

    public PolarisRpcZuulFilter(ProxyRequestHelper helper, ZuulProperties properties,
                                ApacheHttpClientConnectionManagerFactory connectionManagerFactory,
                                ApacheHttpClientFactory httpClientFactory, PolarisProviderDiscovery polarisProviderDiscovery) {
        this.helper = helper;

        this.properties = properties;
        this.hostProperties = properties.getHost();
        this.sslHostnameValidationEnabled = properties.isSslHostnameValidationEnabled();
        this.forceOriginalQueryStringEncoding = properties.isForceOriginalQueryStringEncoding();

        this.connectionManagerFactory = connectionManagerFactory;
        this.httpClientFactory = httpClientFactory;

        this.polarisProviderDiscovery = polarisProviderDiscovery;
        checkServletVersion();
    }

    protected void checkServletVersion() {
        // To support Servlet API 3.1 we need to check if getContentLengthLong exists
        // Spring 5 minimum support is 3.0, so this stays
        try {
            HttpServletRequest.class.getMethod("getContentLengthLong");
            useServlet31 = true;
        } catch (NoSuchMethodException e) {
            useServlet31 = false;
        }
    }

    @PostConstruct
    private void initialize() {
        this.connectionManager = newConnectionManager();
        this.httpClient = newClient();

        this.connectionManagerTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (PolarisRpcZuulFilter.this.connectionManager == null) {
                    return;
                }
                PolarisRpcZuulFilter.this.connectionManager.closeExpiredConnections();
            }
        }, 30000, 5000);
    }

    protected HttpClientConnectionManager newConnectionManager() {
        return connectionManagerFactory.newConnectionManager(
                !this.sslHostnameValidationEnabled,
                this.hostProperties.getMaxTotalConnections(),
                this.hostProperties.getMaxPerRouteConnections(),
                this.hostProperties.getTimeToLive(), this.hostProperties.getTimeUnit(),
                null);
    }

    protected CloseableHttpClient newClient() {
        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(this.hostProperties.getConnectionRequestTimeoutMillis())
                .setSocketTimeout(this.hostProperties.getSocketTimeoutMillis())
                .setConnectTimeout(this.hostProperties.getConnectTimeoutMillis())
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
        return httpClientFactory.createBuilder().setDefaultRequestConfig(requestConfig)
                .setConnectionManager(this.connectionManager).disableRedirectHandling()
                .build();
    }

    @PreDestroy
    public void stop() {
        this.connectionManagerTimer.cancel();
    }

    @Override
    public String filterType() {
        return FilterConstants.ROUTE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.RIBBON_ROUTING_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();
        MultiValueMap<String, String> headers = this.helper.buildZuulRequestHeaders(request);
        MultiValueMap<String, String> params = this.helper.buildZuulRequestQueryParams(request);
        String verb = getVerb(request);
        InputStream requestEntity = getRequestBody(request);
        if (getContentLength(request) < 0) {
            context.setChunkedRequestBody();
        }

        String uri = this.helper.buildZuulRequestURI(request);
        this.helper.addIgnoredHeaders();

        try {
            String peerNamespace = MetadataContextHolder.get().getSystemMetadata(MetadataConstant.SystemMetadataKey.PEER_NAMESPACE);
            String serviceId = (String) context.get(FilterConstants.SERVICE_ID_KEY);
            String proxy = (String) context.get(FilterConstants.PROXY_KEY);
            // TODO retry
            Boolean retryable = (Boolean) context.get(FilterConstants.RETRYABLE_KEY);

            Instance instance = polarisProviderDiscovery.selectProvider(peerNamespace, serviceId);
            if (instance == null) {
                // skip later execution
                context.setSendZuulResponse(false);

                context.setResponseStatusCode(HttpStatus.BAD_GATEWAY.value());
                context.getResponse().getWriter().write(Consts.QUOTA_LIMITED_INFO + "no providers");

                return null;
            }

            uri = handleUri(instance, proxy, uri);
            CloseableHttpResponse response = forward(this.httpClient, verb, uri, request, headers, params, requestEntity);
            setResponse(response);

            // skip later execution
            context.setSendZuulResponse(false);
        } catch (Exception ex) {
            throw new ZuulRuntimeException(handleException(ex));
        }
        return null;
    }

    protected String getVerb(HttpServletRequest request) {
        String sMethod = request.getMethod();
        return sMethod.toUpperCase();
    }

    protected InputStream getRequestBody(HttpServletRequest request) {
        InputStream requestEntity = null;
        try {
            requestEntity = (InputStream) RequestContext.getCurrentContext().get(FilterConstants.REQUEST_ENTITY_KEY);
            if (requestEntity == null) {
                requestEntity = request.getInputStream();
            }
        } catch (IOException ex) {
            LOG.error("error during getRequestBody", ex);
        }
        return requestEntity;
    }

    // Get the header value as a long in order to more correctly proxy very large requests
    protected long getContentLength(HttpServletRequest request) {
        if (useServlet31) {
            return request.getContentLengthLong();
        }
        String contentLengthHeader = request.getHeader(HttpHeaders.CONTENT_LENGTH);
        if (contentLengthHeader != null) {
            try {
                return Long.parseLong(contentLengthHeader);
            } catch (NumberFormatException ignored) {
            }
        }
        return request.getContentLength();
    }

    protected String handleUri(Instance instance, String prefix, String uri) {
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }

        // handle stripPrefix
        if (uri.startsWith("/" + prefix) && this.properties.isStripPrefix()) {
            uri = uri.substring(1 + prefix.length());
        }

        return "http://" + instance.getHost() + ":" + instance.getPort() + uri;
    }

    protected CloseableHttpResponse forward(CloseableHttpClient httpclient, String verb, String uri,
                                            HttpServletRequest request, MultiValueMap<String, String> headers,
                                            MultiValueMap<String, String> params, InputStream requestEntity) throws Exception {
        URL host = new URL(uri);
        HttpHost httpHost = getHttpHost(host);
        long contentLength = getContentLength(request);

        ContentType contentType = null;

        if (request.getContentType() != null) {
            contentType = ContentType.parse(request.getContentType());
        }

        InputStreamEntity entity = new InputStreamEntity(requestEntity, contentLength, contentType);

        HttpRequest httpRequest = buildHttpRequest(verb, uri, entity, headers, params, request);
        LOG.debug(httpHost.getHostName() + " " + httpHost.getPort() + " " + httpHost.getSchemeName());
        return forwardRequest(httpclient, httpHost, httpRequest);
    }

    protected HttpHost getHttpHost(URL host) {
        return new HttpHost(host.getHost(), host.getPort(), host.getProtocol());
    }

    protected HttpRequest buildHttpRequest(String verb, String uri, InputStreamEntity entity,
                                           MultiValueMap<String, String> headers, MultiValueMap<String, String> params,
                                           HttpServletRequest request) {
        HttpRequest httpRequest;
        String uriWithQueryString = uri + (this.forceOriginalQueryStringEncoding ? getEncodedQueryString(request)
                : this.helper.getQueryString(params));

        switch (verb.toUpperCase()) {
            case "POST":
                HttpPost httpPost = new HttpPost(uriWithQueryString);
                httpRequest = httpPost;
                httpPost.setEntity(entity);
                break;
            case "PUT":
                HttpPut httpPut = new HttpPut(uriWithQueryString);
                httpRequest = httpPut;
                httpPut.setEntity(entity);
                break;
            case "PATCH":
                HttpPatch httpPatch = new HttpPatch(uriWithQueryString);
                httpRequest = httpPatch;
                httpPatch.setEntity(entity);
                break;
            case "DELETE":
                BasicHttpEntityEnclosingRequest entityRequest = new BasicHttpEntityEnclosingRequest(verb, uriWithQueryString);
                httpRequest = entityRequest;
                entityRequest.setEntity(entity);
                break;
            default:
                httpRequest = new BasicHttpRequest(verb, uriWithQueryString);
                LOG.debug(uriWithQueryString);
        }

        httpRequest.setHeaders(convertHeaders(headers));

        return httpRequest;
    }

    protected String getEncodedQueryString(HttpServletRequest request) {
        String query = request.getQueryString();
        return (query != null) ? "?" + query : "";
    }

    protected Header[] convertHeaders(MultiValueMap<String, String> headers) {
        List<Header> list = new ArrayList<>();
        for (String name : headers.keySet()) {
            for (String value : headers.get(name)) {
                list.add(new BasicHeader(name, value));
            }
        }
        return list.toArray(new BasicHeader[0]);
    }

    protected MultiValueMap<String, String> revertHeaders(Header[] headers) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        for (Header header : headers) {
            String name = header.getName();
            if (!map.containsKey(name)) {
                map.put(name, new ArrayList<>());
            }
            map.get(name).add(header.getValue());
        }
        return map;
    }

    protected CloseableHttpResponse forwardRequest(CloseableHttpClient httpclient, HttpHost httpHost,
                                                   HttpRequest httpRequest) throws IOException {
        return httpclient.execute(httpHost, httpRequest);
    }

    protected void setResponse(HttpResponse response) throws IOException {
        RequestContext.getCurrentContext().set("zuulResponse", response);
        this.helper.setResponse(response.getStatusLine().getStatusCode(),
                response.getEntity() == null ? null : response.getEntity().getContent(),
                revertHeaders(response.getAllHeaders()));
    }

    protected ZuulException handleException(Exception ex) {
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        Throwable cause = ex;
        String message = ex.getMessage();

        ClientException clientException = findClientException(ex);

        if (clientException != null) {
            if (clientException
                    .getErrorType() == ClientException.ErrorType.SERVER_THROTTLED) {
                statusCode = HttpStatus.SERVICE_UNAVAILABLE.value();
            }
            cause = clientException;
            message = clientException.getErrorType().toString();
        }
        return new ZuulException(cause, "Forwarding error", statusCode, message);
    }

    protected ClientException findClientException(Throwable t) {
        if (t == null) {
            return null;
        }
        if (t instanceof ClientException) {
            return (ClientException) t;
        }
        return findClientException(t.getCause());
    }
}