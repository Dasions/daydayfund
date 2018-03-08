package com.dasion.daydayfund.fund;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.dasion.daydayfund.constant.HttpHeadConstant;
import com.dasion.daydayfund.constant.RedisConstant;
import com.dasion.daydayfund.tool.HttpclientTool;
import com.dasion.daydayfund.tool.IPTool;
import com.dasion.daydayfund.tool.JedisTool;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import redis.clients.jedis.Jedis;


/**
 * 获取基金的的净值,计算指定时间之前两年的净值增长率
 * @author Dasion-PC
 *
 */
public class DayDayFundIncInfo implements Runnable {
	
	private static final String COLONS = ":";
	private static final String ZONE_STRING = "0";
	private static final String ONE_STRING = "1";
	private static final String HUNDERD_STRING = "100";
	private static final String DATE_FORMATE = "yyyy-MM-dd";
	private static final String DEFALUT_START_DATE = "2017-12-23";
	
	private String sourceTaskQueue;
	private String resultTaskQueue;
	private static ConcurrentLinkedQueue<String> ipQueue = new ConcurrentLinkedQueue<String>();
	private String startTime;
	private AtomicInteger threadNums;

	public DayDayFundIncInfo(String sourceTaskQueue, String resultTaskQueue, String startTime,
			AtomicInteger threadNums) {
		super();
		this.sourceTaskQueue = sourceTaskQueue;
		this.resultTaskQueue = resultTaskQueue;
		this.startTime = startTime;
		this.threadNums = threadNums;
	}

	public void getFundIncInfo(HttpclientTool tool) throws Exception {
		FundBean fund = null;
		Gson gson = new Gson();
		try (Jedis jedis = JedisTool.getInstance().getResource()) {
			if (jedis.llen(sourceTaskQueue).intValue() < 1) {
				TimeUnit.MILLISECONDS.sleep(6000);
			}

			fund = gson.fromJson(jedis.rpop(sourceTaskQueue), FundBean.class);
		}

		if (fund != null) {
			DateTime dateTime = null;
			if (startTime == null) {
				DateTimeFormatter format = DateTimeFormat.forPattern(DATE_FORMATE);
				dateTime = DateTime.parse(DEFALUT_START_DATE, format);
			} else {
				DateTimeFormatter format = DateTimeFormat.forPattern(DATE_FORMATE);
				dateTime = DateTime.parse(startTime, format);
			}

			fund.setPreYearInc(
					caculInc(getFundValue(fund, dateTime, tool), getFundValue(fund, dateTime.minusYears(1), tool)));
			fund.setPreTwoYearInc(caculInc(getFundValue(fund, dateTime.minusYears(1), tool),
					getFundValue(fund, dateTime.minusYears(2), tool)));
			fund.setSumInc(
					caculInc(getFundValue(fund, dateTime, tool), getFundValue(fund, dateTime.minusYears(2), tool)));

			try (Jedis jedis = JedisTool.getInstance().getResource()) {
				jedis.lpush(resultTaskQueue, gson.toJson(fund));
			}
		}
	}

	private String caculInc(String nowVal, String preVal) {
		if (StringUtils.isAllEmpty(nowVal)) {
			return ZONE_STRING;
		}
		if (StringUtils.isAllEmpty(preVal)) {
			return new BigDecimal(nowVal).divide(new BigDecimal(ONE_STRING), 3, RoundingMode.HALF_DOWN)
					.multiply(new BigDecimal(HUNDERD_STRING)).toString();
		}
		BigDecimal now = new BigDecimal(nowVal);
		BigDecimal pre = new BigDecimal(preVal);
		if (pre.compareTo(BigDecimal.ZERO) == 0) {
			pre.add(new BigDecimal("0.1"));
		}
		return (now.subtract(pre)).divide(pre, 3, RoundingMode.HALF_DOWN).multiply(new BigDecimal(HUNDERD_STRING)).toString();

	}

	private String getFundValue(FundBean fund, DateTime dateTime, HttpclientTool tool) throws IOException {
		JsonObject dataJson = null;
		String data = null;
		for (int i = 0; i <= 6; i = i + 3) {
			data = getValue(fund.getFundCode(), dateTime.plusDays(i), tool);
			dataJson = new JsonParser().parse(data.substring(data.indexOf("(") + 1, data.lastIndexOf(")")))
					.getAsJsonObject();
			if (dataJson.get("ErrCode").getAsInt() != 0) {
				if (i >= 6) {
					return null;
				}
				continue;
			}
			break;
		}

		JsonArray datas = dataJson.get("Data").getAsJsonObject().get("LSJZList").getAsJsonArray();
		if (datas.size() < 1) {
			return null;
		}
		String nowVal = datas.get(0).getAsJsonObject().get("LJJZ").getAsString();
		return nowVal;
	}

	private String getValue(String fundCode, DateTime dateTime, HttpclientTool tool) throws IOException {
		tool.setUrl("http://api.fund.eastmoney.com/f10/lsjz?callback=jQuery18302748120744675864_1515846991963&fundCode="
				+ fundCode + "&pageIndex=1&pageSize=20&startDate=" + dateTime.minusDays(6).toString(DATE_FORMATE)
				+ "&endDate=" + dateTime.toString(DATE_FORMATE) + "&_=1515847185916");
		HashMap<String, String> headerMap = new HashMap<String, String>();
		headerMap.put(HttpHeadConstant.ACCEPT, "*/*");
		headerMap.put(HttpHeadConstant.CONNECTION, "keep-alive");
		headerMap.put(HttpHeadConstant.USERAGENT,
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.89 Safari/537.36");
		headerMap.put(HttpHeadConstant.ACCEPTENCODING, "gzip, deflate, br");
		headerMap.put(HttpHeadConstant.ACCEPTLANGUAGE, "zh-CN,zh;q=0.9");
		headerMap.put(HttpHeadConstant.REFERER, "http://fund.eastmoney.com/f10/jjjz_" + fundCode + ".html");
		headerMap.put(HttpHeadConstant.HOST, "api.fund.eastmoney.com");
		tool.setHeaderMap(headerMap);
		String data = getByProxy(tool);
		return data;
	}

	private String getByProxy(HttpclientTool tool) {
		String fundHtml = null;
		Boolean fail = true;
		int retry = 0;
		while (fail) {
			try {
				fundHtml = tool.getContentByGet();
				fail = false;
			} catch (Exception e) {
				if ((retry % 6) > 0) {
					List<String> ips = IPTool.getIps(1, "http://fund.eastmoney.com/");
					if (ipQueue.isEmpty()) {
						for (String ip : ips) {
							tool.setRequestConfig(
									tool.getProxyRequestConfig(ip.split(COLONS)[0], Integer.parseInt(ip.split(COLONS)[1])));
							ipQueue.offer(ip);
						}
					} else {
						String ipFromQueue = ipQueue.poll();
						if (ipFromQueue != null) {
							tool.setRequestConfig(tool.getProxyRequestConfig(ipFromQueue.split(COLONS)[0],
									Integer.parseInt(ipFromQueue.split(COLONS)[1])));
							ipQueue.offer(ipFromQueue);
						}
					}

				} else {
					tool.setRequestConfig(RequestConfig.custom().setSocketTimeout(1000).setConnectTimeout(1000)
							.setConnectionRequestTimeout(1000).build());
				}
				retry++;

			}
		}
		return fundHtml;
	}

	@Override
	public void run() {
		HttpclientTool tool = new HttpclientTool();
		tool.createDefaultConfigClientByHttpClients();
		tool.setDefaultCookieStore();
		tool.setDefaultHeader();
		try {
			while (true) {
				if (threadNums.get() > 20) {
					TimeUnit.MILLISECONDS.sleep(1000);
					continue;
				}
				try (Jedis jedis = JedisTool.getInstance().getResource()) {
					System.out.println("inc: " + jedis.get(RedisConstant.SEMAPHORE_KEY));
					if (RedisConstant.SEMAPHORE_KEY_STOP.equals(jedis.get(RedisConstant.SEMAPHORE_KEY))) {
						return;
					}
				}
				threadNums.getAndIncrement();
				getFundIncInfo(tool);
				threadNums.decrementAndGet();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
