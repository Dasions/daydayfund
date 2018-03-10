package com.dasion.daydayfund.fund;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.config.RequestConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.dasion.daydayfund.constant.HtmlConstant;
import com.dasion.daydayfund.constant.RedisConstant;
import com.dasion.daydayfund.tool.HttpclientTool;
import com.dasion.daydayfund.tool.IPTool;
import com.dasion.daydayfund.tool.JedisTool;
import com.google.gson.Gson;

import redis.clients.jedis.Jedis;

/**
 * 获取基金的详细数据
 * @author Dasion-PC
 *
 */
public class DayDayFundDetailInfo implements Runnable {
	private static final String COLONS = ":";
	private static final String EMPTY = "";
	private static final String ZONE_STRING = "0";
	private static final String ISO88591 = "ISO-8859-1";
	private static final String UTF8 = "utf-8";
	
	private static final String TARGET_URL = "http://fund.eastmoney.com/";
	
	private String sourceTaskQueue;
	private String resultTaskQueue;
	private String date;
	private AtomicInteger threadNums;
	private static ConcurrentLinkedQueue<String> ipQueue = new ConcurrentLinkedQueue<String>();

	public DayDayFundDetailInfo(String sourceTaskQueue, String resultTaskQueue, String date, AtomicInteger threadNums) {
		super();
		this.sourceTaskQueue = sourceTaskQueue;
		this.resultTaskQueue = resultTaskQueue;
		this.date = date;
		this.threadNums = threadNums;
	}

	public void getFunddInfo(HttpclientTool tool) throws Exception {
		FundBean fund = null;
		Gson gson = new Gson();
		try (Jedis jedis = JedisTool.getInstance().getResource()) {
			fund = gson.fromJson(jedis.rpop(sourceTaskQueue), FundBean.class);
		}

		if (fund != null) {
			tool.setUrl(TARGET_URL + fund.getFundCode() + ".html");
			String fundHtml = getByProxy(tool);

			Document fundDataDoc = Jsoup.parse(fundHtml);

			Element item = fundDataDoc.getElementById("gz_gszzl");
			if (item != null) {
				if (!"-".equals(item.text().trim())) {
					fund.setAppraisement(item.text().replace("%", EMPTY).trim().replace("+", EMPTY));
				}
			}

			Elements dataItems = fundDataDoc.getElementsByClass("dataItem02");

			if (dataItems.size() < 1) {
				return;
			}
			if (dataItems.get(0).getElementsByTag(HtmlConstant.P).size() < 1) {
				return;
			}
			if (dataItems.get(0).getElementsByTag(HtmlConstant.P).get(0).text().length() >= 24) {
				fund.setDate(fundDataDoc.getElementsByClass("dataItem02").get(0).getElementsByTag(HtmlConstant.P).get(0).text()
						.substring(14, 24));
			}

			if(fundDataDoc.getElementById("gz_gztime") != null){
				fund.setAppraisementTime(fundDataDoc.getElementById("gz_gztime").text());
			}
			
			if (date != null && !date.equals(fund.getDate())) {
				return;
			}

			Elements ddItems = fundDataDoc.getElementsByClass("dataItem02").get(0).getElementsByTag(HtmlConstant.DD);
			fund.setDayInc(ddItems.get(0).getElementsByTag(HtmlConstant.SPAN).get(1).text().replace("%", EMPTY).replace("--", ZONE_STRING));
			fund.setNetValue(ddItems.get(0).getElementsByTag(HtmlConstant.SPAN).get(0).text().replace("%", EMPTY).replace("--", ZONE_STRING));
			fund.setThreeyearInc(
					ddItems.get(2).getElementsByTag(HtmlConstant.SPAN).get(1).text().replace("%", EMPTY).replace("--", ZONE_STRING));
			List<String> managers = new ArrayList<>();
			Elements managerList = fundDataDoc.getElementsByClass("infoOfFund").get(0).getElementsByTag(HtmlConstant.TD);
			for (Element mg : managerList) {
				String text = new String(mg.text().getBytes(ISO88591), UTF8).replaceAll(" ", EMPTY);
				if (text.contains("经理")) {
					managers.add(new String(mg.getElementsByTag(HtmlConstant.A).get(0).text().getBytes(ISO88591), UTF8));
				}
				if (text.contains("成立日")) {
					fund.setFoundedOn(text.split("：")[1].trim());
				}
			}
			fund.setManagers(managers);

			// 基金持股列表
			Elements trs = fundDataDoc.getElementsByClass("poptableWrap").get(0).getElementsByTag(HtmlConstant.TR);
			String tag = new String(trs.get(0).getElementsByTag(HtmlConstant.TH).get(0).text().getBytes(ISO88591), UTF8);
			try (Jedis jedis = JedisTool.getInstance().getResource()) {
				if ("股票名称".equals(tag.trim())) {
					String shares = EMPTY;
					for (int i = 1; i < trs.size(); i++) {
						tag = new String(trs.get(i).getElementsByTag(HtmlConstant.TD).get(0).text().getBytes(ISO88591),
								UTF8);
						shares = shares + tag.trim() + ",";
					}
					jedis.hset("shares", fund.getFundCode(), shares);
				}

				jedis.lpush(resultTaskQueue, gson.toJson(fund));
			}
		}
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
					List<String> ips = IPTool.getIps(1, TARGET_URL);
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
		while (true) {
			try {
				if (threadNums.get() > 15) {
					TimeUnit.MILLISECONDS.sleep(1000);
					continue;
				}
				
				try (Jedis jedis = JedisTool.getInstance().getResource()) {
					System.out.println("detail: "+jedis.get(RedisConstant.SEMAPHORE_KEY));
					if(RedisConstant.SEMAPHORE_KEY_STOP.equals(jedis.get(RedisConstant.SEMAPHORE_KEY))){
						return;
					}
				}
				
				threadNums.getAndIncrement();
				getFunddInfo(tool);
				threadNums.decrementAndGet();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
