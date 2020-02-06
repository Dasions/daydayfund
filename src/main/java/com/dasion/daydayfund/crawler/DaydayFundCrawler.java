package com.dasion.daydayfund.crawler;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dasion.daydayfund.constant.HtmlConstant;
import com.dasion.daydayfund.constant.HttpHeadConstant;
import com.dasion.daydayfund.constant.RedisConstant;
import com.dasion.daydayfund.enums.DaydayFundExcStepEnum;
import com.dasion.daydayfund.fund.BaseBean;
import com.dasion.daydayfund.fund.FundBean;
import com.dasion.daydayfund.tool.HttpclientTool;
import com.dasion.daydayfund.tool.IPTool;
import com.dasion.daydayfund.tool.JedisTool;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import redis.clients.jedis.Jedis;

@Component
public class DaydayFundCrawler {
	private static final Logger logger = LoggerFactory.getLogger(DaydayFundCrawler.class);
	@Autowired
	private JedisTool jedisTool;

	private static final String EMPTY = "";
	private static final String ZONE_STRING = "0";
	private static final String DETAIL_INFO_URL = "http://fund.eastmoney.com/";
	private static final String COLONS = ":";
	private static final String ISO88591 = "ISO-8859-1";
	private static final String UTF8 = "utf-8";
	private static final String ONE_STRING = "1";
	private static final String HUNDERD_STRING = "100";
	private static final String DATE_FORMATE = "yyyy-MM-dd";
	private static final String DEFALUT_START_DATE = DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd"));;
	private static ConcurrentLinkedQueue<String> ipQueue = new ConcurrentLinkedQueue<String>();
	private static final String TARGET_URL = "http://fund.eastmoney.com/";

	public void excRoute(BaseBean baseBean, HttpclientTool tool) throws Exception {
		String excStep = baseBean.getExcStep();
		DaydayFundExcStepEnum excStepEnum = DaydayFundExcStepEnum.getEnum(excStep);
		switch (excStepEnum) {
		case BASE_INFO:
			//logger.info("获取基本数据");
			getFundBaseinfo(tool, baseBean);
			break;
		case DETAIL_INFO:
			//logger.info("获取详情数据");
			getFunddInfo(tool, baseBean);
			break;
		case INC_INFO:
			//logger.info("获取增长率");
			getFundIncInfo(tool, baseBean);
			break;
		default:
			logger.error(baseBean.getCrawlerType() + " 中未定义的执行步骤： " + excStepEnum);
			return;
		}
	}

	/**
	 * 获取基金的基础信息
	 * 
	 * @param sourceBaseBean
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void getFundBaseinfo(HttpclientTool tool, BaseBean sourceBaseBean) throws IOException, InterruptedException {
		tool.setUrl(sourceBaseBean.getRequestUrl());
		String fundListHtml = tool.getContentByGet();
		tool.getClient().close();
		Document fundListDoc = Jsoup.parse(fundListHtml);
		Elements fundListTable = fundListDoc.getElementsByClass("ttjj-table ttjj-table-hover common-sort-table");

		if (fundListTable.size() > 0) {
			Elements fundListTrs = fundListTable.get(0).getElementsByTag(HtmlConstant.TBODY).get(0)
					.getElementsByTag(HtmlConstant.TR);
			for (Element fundTr : fundListTrs) {
				Elements fundTds = fundTr.getElementsByTag(HtmlConstant.TD);

				if (fundTds.get(0).getElementsByTag(HtmlConstant.A).size() < 1) {
					continue;
				}

				FundBean fund = concatFundBean(fundTds);

				BaseBean baseBean = copyBaseBean(sourceBaseBean);
				String fromQueue = baseBean.getFromQueue();
				// 后面还需要爬取数据，仍然放入sourceDataQueue_+日期 队列
				baseBean.setToQueue(fromQueue);
				// 设置下一步请求数据的url
				baseBean.setRequestUrl(DETAIL_INFO_URL + fund.getFundCode() + ".html");
				baseBean.setFundBean(fund);
				sourceBaseBean.setExcStep(DaydayFundExcStepEnum.DETAIL_INFO.getCode());
				
				try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
					Gson gson = new Gson();
					jedis.lpush(baseBean.getToQueue(), gson.toJson(baseBean));
					jedis.incr(RedisConstant.COUNT_BASE_INFO);
				}
			}
		}
	}

	public void getFunddInfo(HttpclientTool tool, BaseBean sourceBaseBean) throws Exception {
		FundBean fund = sourceBaseBean.getFundBean();
		Gson gson = new Gson();

		if (fund != null && sourceBaseBean.getRequestUrl() != null) {
			tool.setUrl(sourceBaseBean.getRequestUrl());
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
				fund.setDate(fundDataDoc.getElementsByClass("dataItem02").get(0).getElementsByTag(HtmlConstant.P).get(0)
						.text().substring(14, 24));
			}

			if (fundDataDoc.getElementById("gz_gztime") != null) {
				fund.setAppraisementTime(fundDataDoc.getElementById("gz_gztime").text());
			}

			// if (date != null && !date.equals(fund.getDate())) {
			// return;
			// }

			Elements ddItems = fundDataDoc.getElementsByClass("dataItem02").get(0).getElementsByTag(HtmlConstant.DD);
			fund.setDayInc(ddItems.get(0).getElementsByTag(HtmlConstant.SPAN).get(1).text().replace("%", EMPTY)
					.replace("--", ZONE_STRING));
			fund.setNetValue(ddItems.get(0).getElementsByTag(HtmlConstant.SPAN).get(0).text().replace("%", EMPTY)
					.replace("--", ZONE_STRING));
			fund.setThreeyearInc(ddItems.get(2).getElementsByTag(HtmlConstant.SPAN).get(1).text().replace("%", EMPTY)
					.replace("--", ZONE_STRING));
			List<String> managers = new ArrayList<>();
			Elements managerList = fundDataDoc.getElementsByClass("infoOfFund").get(0)
					.getElementsByTag(HtmlConstant.TD);
			for (Element mg : managerList) {
				String text = new String(mg.text().getBytes(ISO88591), UTF8).replaceAll(" ", EMPTY);
				if (text.contains("经理")) {
					managers.add(
							new String(mg.getElementsByTag(HtmlConstant.A).get(0).text().getBytes(ISO88591), UTF8));
				}
				if (text.contains("成立日")) {
					fund.setFoundedOn(text.split("：")[1].trim());
				}
			}
			fund.setManagers(managers);

			// 基金持股列表
			Elements trs = fundDataDoc.getElementsByClass("poptableWrap").get(0).getElementsByTag(HtmlConstant.TR);
			String tag = new String(trs.get(0).getElementsByTag(HtmlConstant.TH).get(0).text().getBytes(ISO88591),
					UTF8);
			try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
				if ("股票名称".equals(tag.trim())) {
					String shares = EMPTY;
					for (int i = 1; i < trs.size(); i++) {
						tag = new String(trs.get(i).getElementsByTag(HtmlConstant.TD).get(0).text().getBytes(ISO88591),
								UTF8);
						shares = shares + tag.trim() + ",";
					}
					jedis.hset("shares", fund.getFundCode(), shares);
				}

				// 后面还需要爬取数据，仍然放入sourceDataQueue_+日期 队列
				sourceBaseBean.setToQueue(sourceBaseBean.getFromQueue());
				sourceBaseBean.setExcStep(DaydayFundExcStepEnum.INC_INFO.getCode());
				jedis.lpush(sourceBaseBean.getToQueue(), gson.toJson(sourceBaseBean));
				jedis.incr(RedisConstant.COUNT_DETAIL_INFO);
			}
		}
	}

	/**
	 * 计算增长率
	 */
	public void getFundIncInfo(HttpclientTool tool, BaseBean sourceBaseBean) throws Exception {
		FundBean fund = sourceBaseBean.getFundBean();
		Gson gson = new Gson();

		if (fund != null) {
			DateTime dateTime = null;
			// if (startTime == null) {
			DateTimeFormatter format = DateTimeFormat.forPattern(DATE_FORMATE);
			dateTime = DateTime.parse(DEFALUT_START_DATE, format);
			// } else {
			// DateTimeFormatter format =
			// DateTimeFormat.forPattern(DATE_FORMATE);
			// dateTime = DateTime.parse(startTime, format);
			// }

			fund.setPreYearInc(
					caculInc(getFundValue(fund, dateTime, tool), getFundValue(fund, dateTime.minusYears(1), tool)));
			fund.setPreTwoYearInc(caculInc(getFundValue(fund, dateTime.minusYears(1), tool),
					getFundValue(fund, dateTime.minusYears(2), tool)));
			fund.setSumInc(
					caculInc(getFundValue(fund, dateTime, tool), getFundValue(fund, dateTime.minusYears(2), tool)));

			try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
				String resultQueue = RedisConstant.FINAL_DATA_QUEUE + sourceBaseBean.getFromQueue().split("_")[1];
				jedis.lpush(resultQueue, gson.toJson(fund));
				jedis.incr(RedisConstant.COUNT_INC_INFO);
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
		return (now.subtract(pre)).divide(pre, 3, RoundingMode.HALF_DOWN).multiply(new BigDecimal(HUNDERD_STRING))
				.toString();

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
				logger.warn("-----使用代理IP----");
				if ((retry % 6) > 0) {
					List<String> ips = IPTool.getIps(1, TARGET_URL);
					if (ipQueue.isEmpty()) {
						for (String ip : ips) {
							tool.setRequestConfig(tool.getProxyRequestConfig(ip.split(COLONS)[0],
									Integer.parseInt(ip.split(COLONS)[1])));
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

	private BaseBean copyBaseBean(BaseBean baseBean) {
		BaseBean baseBeanNew = new BaseBean();
		BeanUtils.copyProperties(baseBean, baseBeanNew);
		return baseBeanNew;
	}

	private FundBean concatFundBean(Elements fundTds) {
		FundBean fund = new FundBean();
		fund.setFundCode(fundTds.get(0).getElementsByTag(HtmlConstant.A).get(1).text());
		fund.setFundName(fundTds.get(0).getElementsByTag(HtmlConstant.A).get(0).text());
		fund.setFundType(fundTds.get(2).text());
		fund.setHalfyearInc(format(fundTds.get(7).text().trim()));
		fund.setMonthInc(format(fundTds.get(5).text().trim()));
		fund.setSeasonhInc(format(fundTds.get(6).text().trim()));
		fund.setSetupInc(format(fundTds.get(11).text().trim()));
		fund.setThisyearInc(format(fundTds.get(10).text().trim()));
		fund.setTwoyearInc(format(fundTds.get(9).text().trim()));
		fund.setWeekInc(format(fundTds.get(4).text().trim()));
		fund.setYearInc(format(fundTds.get(8).text().trim()));
		return fund;
	}

	private String format(String text) {
		if ("-".equals(text.trim())) {
			return ZONE_STRING;
		}

		return text.trim().replace("%", EMPTY);
	}

}
