package com.dasion.daydayfund.tool;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import com.google.gson.Gson;

/**
 * 此工具只支持一个httpclient客户端，同一个httpclient同时只执行一个post或者get请求
 * 
 * @author Dasion-PC
 *
 */
public class HttpclientTool {

	final private static String CONTENT_TYPE_APPLICATION_JSON = "application/json";
	final private static String REQUEST_HEADER_CONTENT_ENCODING = "Content-Encoding";
	final private static String REQUEST_HEADER_CONTENT_ENCODING_GZIP = "gzip";
	private CloseableHttpClient client;

	/**
	 * 可以在创建客户端之后,通过设置context保持之前会话之后的cookie和其他细节
	 */
	private HttpClientContext context;

	/**
	 * 在创建httpclient时设置，则可以获取和修改客户端的cookie.
	 * 在创建httpclient时没设置，则需要设置context之后才可以获取和修改客户端的cookie
	 */
	private CookieStore cookieStore;

	private String url;

	private RequestConfig requestConfig;

	private HashMap<String, String> headerMap;

	/**
	 * 默认配置
	 */
	private RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(5000).setConnectTimeout(5000)
			.setConnectionRequestTimeout(5000)
			.build();

	/**
	 * 通过HttpClients接口创建默认配置的httpclient
	 */
	public void createDefaultConfigClientByHttpClients() {
		this.client = HttpClients.createDefault();
	}

	/**
	 * 通过HttpClients接口创建使用当前系统的配置的httpclient
	 */
	public void createSysConfigClientByHttpClients() {
		this.client = HttpClients.createSystem();
	}

	/**
	 * 通过HttpClients接口创建最少配置的httpclient， 创建后只能更改超时设置，其他设置均无法改变
	 */
	public void createMinimalClientByHttpClients() {
		this.client = HttpClients.createMinimal();
	}

	/**
	 * 通过HttpClients接口创建自定义配置的httpclient, requestConfig为null则使用默认配置
	 * 如果cookieStore不为空，则httpclient会自动管理cookie，必要时可手动更改cookie 新建cookie ： new
	 * BasicCookieStore();
	 */
	public void createCustomConfigClientByHttpClients(RequestConfig requestConfig, CookieStore cookieStore) {
		if (cookieStore != null) {
			setCookieStore(cookieStore);
		}

		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(200);
		connectionManager.setDefaultMaxPerRoute(20);
		this.client = HttpClients.custom()
				.setDefaultRequestConfig((requestConfig == null) ? this.defaultRequestConfig : requestConfig)
				.setDefaultCookieStore(getCookieStore())
				.setConnectionManager(connectionManager).build();
	}

	public void createSSLClientByHttpClients(RequestConfig requestConfig, CookieStore cookieStore)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(new TrustStrategy() {

			@Override
			public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				final X509Certificate cert = chain[0];
				return "CN=httpbin.org".equalsIgnoreCase(cert.getSubjectDN().getName());
			}

		}).build();

		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1.2" }, null,
				SSLConnectionSocketFactory.getDefaultHostnameVerifier());

		if (cookieStore != null) {
			setCookieStore(cookieStore);
		}

		this.client = HttpClients.custom()
				.setDefaultRequestConfig((requestConfig == null) ? this.defaultRequestConfig : requestConfig)
				.setDefaultCookieStore(getCookieStore()).setSSLSocketFactory(sslsf).build();

	}

	/**
	 * 设置默认请求头
	 */
	public void setDefaultHeader() {
		this.headerMap = new HashMap<String, String>();
		this.headerMap.put("Accept", "text/plain, */*; q=0.01");
		this.headerMap.put("Connection", "keep-alive");
		this.headerMap.put("X-Requested-With", "XMLHttpRequest");
		this.headerMap.put("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.89 Safari/537.36");
		this.headerMap.put("Accept-Encoding", "gzip, deflate, br");
		this.headerMap.put("Accept-Language", "zh-CN,zh;q=0.9");
	}

	public void setDefaultCookieStore() {
		setCookieStore(new BasicCookieStore());
	}

	/**
	 * 通过GET请求获取数据
	 * 
	 * @return
	 * @throws IOException
	 */
	public String getContentByGet() throws IOException {
		HttpGet httpget = new HttpGet(getUrl());
		setGetHeader(httpget);
		setGetConfig(httpget);
		HttpEntity entity;
		try (CloseableHttpResponse response = getClient().execute(httpget, getContext())) {
			entity = getContent(response);
			if (entity != null) {
				return EntityUtils.toString(entity);
			}
		} catch (IOException e) {
			throw e;
		}
		return null;
	}

	/**
	 * 通过post请求获取数据
	 * 
	 * @return
	 * @throws IOException
	 */
	public String getContentByPost() throws IOException {
		HttpPost httpPost = new HttpPost(getUrl());
		setPostHeader(httpPost);
		setPostConfig(httpPost);
		HttpEntity entity;
		try (CloseableHttpResponse response = getClient().execute(httpPost, getContext())) {
			entity = getContent(response);
			if (entity != null) {
				return EntityUtils.toString(entity);
			}
		} catch (IOException e) {
			throw e;
		}
		return null;
	}

	private void setPostConfig(HttpPost httpPost) {
		if (getRequestConfig() != null) {
			httpPost.setConfig(getRequestConfig());
		}
	}

	private void setGetConfig(HttpGet httpget) {
		if (getRequestConfig() != null) {
			httpget.setConfig(getRequestConfig());
		}
	}

	/**
	 * 设置post请求的请求头
	 * 
	 * @param httpPost
	 */
	private void setPostHeader(HttpPost httpPost) {
		if (getHeaderMap() != null) {
			for (Entry<String, String> entry : getHeaderMap().entrySet()) {
				httpPost.setHeader(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * 设置GET请求的请求头
	 * 
	 * @param httpget
	 */
	private void setGetHeader(HttpGet httpget) {
		if (getHeaderMap() != null) {
			for (Entry<String, String> entry : getHeaderMap().entrySet()) {
				httpget.setHeader(entry.getKey(), entry.getValue());
			}
		}
	}

	public String getContentByPostParams(Map<String, String> params) throws IOException {
		StringEntity requestEntity = new StringEntity(transformMapToJsonStr(params), StandardCharsets.UTF_8);
		HttpPost httpPost = new HttpPost(getUrl());
		setPostHeader(httpPost);
		setPostConfig(httpPost);
		requestEntity.setContentType(CONTENT_TYPE_APPLICATION_JSON);
		httpPost.setEntity(requestEntity);
		HttpEntity entity;
		try (CloseableHttpResponse response = getClient().execute(httpPost, getContext())) {
			entity = getContent(response);
			// response.close();
			if (entity != null) {
				return EntityUtils.toString(entity);
			}
		} catch (IOException e) {
			throw e;
		}
		return null;
	}

	public String getContentByFormPostParams(Map<String, String> params) throws IOException, URISyntaxException {
		HttpEntity requestEntity = getFormEntity(params);
		HttpPost httpPost = new HttpPost(getUrl());
		setPostHeader(httpPost);
		setPostConfig(httpPost);
		httpPost.setEntity(requestEntity);
		HttpEntity entity;
		try (CloseableHttpResponse response = getClient().execute(httpPost, getContext())) {
			entity = getContent(response);
			if (entity != null) {
				return EntityUtils.toString(entity);
			}
		} catch (IOException e) {
			throw e;
		}
		return null;
	}

	public HttpEntity getFormEntity(Map<String, String> params) throws UnsupportedEncodingException {
		List<NameValuePair> formParams = new ArrayList<>();
		for (Entry<String, String> param : params.entrySet()) {
			formParams.add(new BasicNameValuePair(param.getKey(), param.getValue()));
		}
		return new UrlEncodedFormEntity(formParams, "UTF-8");
	}

	public String getContentByGetParams(Map<String, String> paramsMap) throws ParseException, IOException {

		HttpGet httpget = new HttpGet(getUrl() + "?" + transformMapToStr(paramsMap));
		setGetHeader(httpget);
		setGetConfig(httpget);
		HttpEntity entity;
		try (CloseableHttpResponse response = getClient().execute(httpget, getContext())) {
			entity = getContent(response);
			if (entity != null) {
				return EntityUtils.toString(entity);
			}
		} catch (IOException e) {
			throw e;
		}
		return null;
	}

	private String transformMapToStr(Map<String, String> paramsMap) throws ParseException, IOException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		for (Entry<String, String> param : paramsMap.entrySet()) {
			params.add(new BasicNameValuePair(param.getKey(), param.getValue()));
		}
		String paramStr = EntityUtils.toString(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

		return paramStr;
	}

	private String transformMapToJsonStr(Map<String, String> params) {
		Gson gson = new Gson();
		String jsonStr = gson.toJson(params);
		return jsonStr;
	}

	/**
	 * 获取返回数据
	 * 
	 * @param response
	 * @return
	 */
	private HttpEntity getContent(CloseableHttpResponse response) {
		Header[] headers = response.getHeaders(REQUEST_HEADER_CONTENT_ENCODING);
		boolean isGzip = false;
		for (Header h : headers) {
			if (h.getValue().equals(REQUEST_HEADER_CONTENT_ENCODING_GZIP)) {
				// 返回头中含有gzip
				isGzip = true;
			}
		}

		if (isGzip) {
			// 需要进行gzip解压处理
			return new GzipDecompressingEntity(response.getEntity());
		} else {
			return response.getEntity();
		}
	}

	public CloseableHttpClient getClient() {
		return client;
	}

	public void setClient(CloseableHttpClient client) {
		this.client = client;
	}

	public HttpClientContext getContext() {
		return context;
	}

	public void setContext(HttpClientContext context) {
		this.context = context;
	}

	public CookieStore getCookieStore() {
		return cookieStore;
	}

	public void setCookieStore(CookieStore cookieStore) {
		this.cookieStore = cookieStore;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public RequestConfig getRequestConfig() {
		return requestConfig;
	}

	public RequestConfig getProxyRequestConfig(String ip, Integer port) {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(1000).setConnectTimeout(1000)
				.setConnectionRequestTimeout(1000).setProxy(new HttpHost(ip, port)) // 设置客户端代理
				.build();
		return requestConfig;
	}

	public void setRequestConfig(RequestConfig requestConfig) {
		this.requestConfig = requestConfig;
	}

	public HashMap<String, String> getHeaderMap() {
		return headerMap;
	}

	public void setHeaderMap(HashMap<String, String> headerMap) {
		this.headerMap = headerMap;
	}

}
