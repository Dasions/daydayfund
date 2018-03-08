package com.dasion.daydayfund.exc;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dasion.daydayfund.fund.ComparatorFund;
import com.dasion.daydayfund.fund.ExecutorServiceTool;
import com.dasion.daydayfund.fund.FundBean;
import com.dasion.daydayfund.fund.SortTypeEnum;
import com.dasion.daydayfund.tool.HttpclientTool;
import com.dasion.daydayfund.tool.JedisTool;
import com.dasion.daydayfund.tool.MailTool;
import com.google.gson.Gson;

import redis.clients.jedis.Jedis;

public class FundExcut {

	private static List<String> getFundCompanysByHttp() throws IOException {
		HttpclientTool tool = new HttpclientTool();
		tool.createDefaultConfigClientByHttpClients();
		tool.setDefaultCookieStore();
		tool.setDefaultHeader();
		tool.setUrl("http://fund.eastmoney.com/company/default.html");
		String fundListHtml = tool.getContentByGet();
		tool.getClient().close();
		Document fundListDoc = Jsoup.parse(fundListHtml);

		Elements trs = fundListDoc.getElementById("gspmTbl").getElementsByTag("tbody").get(0).getElementsByTag("tr");
		List<String> cps = new ArrayList<>();
		for (Element tr : trs) {
			String index = tr.getElementsByTag("td").get(0).text().trim();
			if ("20".equals(index)) {
				break;
			}
			String str = tr.getElementsByTag("td").get(1).getElementsByTag("a").get(0).attr("href");
			cps.add(str.substring(str.lastIndexOf("/") + 1, str.indexOf(".")));
		}
		return cps;
	}

	public static void main(String[] args) throws Exception {
		List<Integer> sortEnum = new ArrayList<Integer>();
		// sortEnum.add(SortTypeEnum.SUMINC_DESC.getCode());
		// sortEnum.add(SortTypeEnum.PRETWOYEAR_DESC.getCode());
		// sortEnum.add(SortTypeEnum.PREYEAR_DESC.getCode());
		// sortEnum.add(SortTypeEnum.THREEYEARINC_DESC.getCode());
		// sortEnum.add(SortTypeEnum.TWOYEARINC_DESC.getCode());
		// sortEnum.add(SortTypeEnum.YEARINC_DESC.getCode());
		// sortEnum.add(SortTypeEnum.THIS_YEARINC_DESC.getCode());
		// sortEnum.add(SortTypeEnum.HALF_YEARINC_DESC.getCode());
		// sortEnum.add(SortTypeEnum.THIS_SEASON_DESC.getCode());
		sortEnum.add(SortTypeEnum.THIS_MONTH_DESC.getCode());
		sortEnum.add(SortTypeEnum.THIS_WEEK_DESC.getCode());
		sortEnum.add(SortTypeEnum.THIS_DAY_DESC.getCode());
		List<FundBean> gongYinFunds = new ArrayList<>(1000);

		Gson gson = new Gson();
		DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
		String partThreeQueue = DateTime.now().toString(format) + "-partThreeQueue";
		try (Jedis jedis = JedisTool.getInstance().getResource()) {
			int len = jedis.llen(partThreeQueue).intValue();
			List<String> jsonStr = jedis.lrange(partThreeQueue, 0, len - 1);
			for (String json : jsonStr) {
				gongYinFunds.add(gson.fromJson(json, FundBean.class));
			}
		}

		printAppraisement(gongYinFunds);

		for (FundBean fund : gongYinFunds) {
			List<String> fundCodes = Arrays.asList(getMyFunds());
			if (fundCodes.contains(fund.getFundCode())) {
				System.out.println(fund);
			}
		}

		System.out.println("数量 ： " + gongYinFunds.size());
		filterDiffFunds(gongYinFunds, 15);
		/**
		 * 增长率范围在0~20之间，实际收益太低，不值得投资 增长率范围在0-60之间可排除 16 17
		 * 年成立的基金的干扰,但是筛选不出收益连续稳定的目标 通过细化筛选范围，筛选收益连续稳定的目标
		 */
		// for (int rate = 5; rate < 20; rate = rate + 5) {
		// catchAreaDatas(gongYinFunds, rate, 60);
		// }

		// gongYinFunds = selectFundsByCreateDate(gongYinFunds, 2014);
		// for (int rate = 5; rate < 20; rate = rate + 5) {
		// catchAreaDatas(gongYinFunds, rate, 60);
		// }
		ExecutorServiceTool.getExecutor().shutdown();
	}

	private static String[] getMyFunds() {
		String[] fundCodes = { "001790", "001579", "020026", "000742", "070032", "519606", "001878", "004477",
				"160213" };

		return fundCodes;
	}

	public static Map<String, List<String>> getUserAndFunds() {
		HashMap<String, List<String>> userAndFundsMap = new HashMap<>();
		try (Jedis jedis = JedisTool.getInstance().getResource()) {
			String userAndFundsStr = jedis.get("userAndFunds");
			JSONArray userAndFunds = JSONArray.parseArray(userAndFundsStr);
			for (int i = 0; i < userAndFunds.size(); i++) {
				JSONObject userAndFund = userAndFunds.getJSONObject(i);
				JSONArray funds = userAndFund.getJSONArray("funds");
				List<String> fundList = new ArrayList<>();
				for(int j=0;j<funds.size();j++){
					fundList.add(funds.getString(j));
				}
				userAndFundsMap.put(userAndFund.getString("user"), fundList);
			}

		}

		return userAndFundsMap;
	}

	public static void sendMail(List<FundBean> funds, int count) {
		Map<String, List<String>> userAndFundsMap = getUserAndFunds();
		HashSet<String> fundSet = new HashSet<String>();
		for(Entry<String, List<String>> entry : userAndFundsMap.entrySet()){
			for(String fund : entry.getValue()){
				fundSet.add(fund);
			}
		}
		
		List<FundBean> fundList = new ArrayList<>();
		for (FundBean fund : funds) {
			if(fundSet.contains(fund.getFundCode())){
				fundList.add(fund);
			}
		}
		
		for(Entry<String, List<String>> entry : userAndFundsMap.entrySet()){
			String content = "<html>" + printAppraisement(funds) + "</br>";
			for (FundBean fund : fundList) {
				List<String> fundCodes = entry.getValue();
				if (fundCodes.contains(fund.getFundCode())) {
					content = content + fund.toMailString() + "</br>";
				}
			}

			content = content + "估值上涨最多  :  </br>";
			Collections.sort(funds, new ComparatorFund(SortTypeEnum.appraisement_DESC.getCode()));
			for (FundBean fund : funds.subList(0, count)) {
				content = content + fund.toMailString() + " </br>";
			}

			content = content + "估值下跌最多  :  </br>";
			List<FundBean> fundUnUsed = new ArrayList<>();
			for (FundBean fund : funds) {
				if ("".equals(fund.getAppraisement().trim())) {
					fundUnUsed.add(fund);
				}
			}
			funds.removeAll(fundUnUsed);
			Collections.sort(funds, new ComparatorFund(SortTypeEnum.appraisement_DESC.getCode()));
			for (FundBean fund : funds.subList(funds.size() - count, funds.size())) {
				content = content + fund.toMailString() + " </br>";
			}
			
			content = content + "最近一天下跌个数 : "
			+ diffCount(funds, -1, SortTypeEnum.THIS_DAY_DESC.getCode()) + " </br>";
			content = content + "最近一天上涨个数 : "
			+ diffCount(funds, 1, SortTypeEnum.THIS_DAY_DESC.getCode()) + " </br>";
			content = content + "</html>";
			MailTool.sendMailData(content, entry.getKey());
		}
		
	}

	private static List<FundBean> selectFundsByCreateDate(List<FundBean> funds, int year) {
		List<FundBean> fundList = new ArrayList<>();
		DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");

		for (FundBean fund : funds) {
			String createDate = fund.getFoundedOn();
			if (createDate == null || "--".equals(createDate)) {
				continue;
			}
			DateTime dateTime = DateTime.parse(createDate, format);
			if (year <= dateTime.getYear()) {
				fundList.add(fund);
			}
		}

		return fundList;
	}

	private static void printFundRange(List<FundBean> funds, List<Integer> sortEnum) {
		for (Integer sort : sortEnum) {
			System.out.println("------" + SortTypeEnum.getName(sort));
			Collections.sort(funds, new ComparatorFund(sort));
			for (int i = 0; i < funds.size(); i++) {
				List<String> fundCodes = Arrays.asList(getMyFunds());
				if (fundCodes.contains(funds.get(i).getFundCode())) {
					System.out.println(
							funds.get(i).getFundCode() + funds.get(i).getFundName() + " : " + i + "/" + funds.size());
				}
			}
		}

	}

	public static void filterDiffFunds(List<FundBean> funds, int count) {

		System.out.println("最近一天下跌最多");
		Collections.sort(funds, new ComparatorFund(SortTypeEnum.THIS_DAY_DESC.getCode()));
		printList(funds, funds.size() - count, funds.size(), SortTypeEnum.THIS_DAY_DESC.getCode());
		// 最近一周下跌最多
		System.out.println("最近一周下跌最多");
		Collections.sort(funds, new ComparatorFund(SortTypeEnum.THIS_WEEK_DESC.getCode()));
		printList(funds, funds.size() - count, funds.size(), SortTypeEnum.THIS_WEEK_DESC.getCode());
		// 最近一个月下跌最多
		System.out.println("最近一个月下跌最多");
		Collections.sort(funds, new ComparatorFund(SortTypeEnum.THIS_MONTH_DESC.getCode()));
		printList(funds, funds.size() - count, funds.size(), SortTypeEnum.THIS_MONTH_DESC.getCode());
		// 最近一个季度下跌最多
		System.out.println("最近一个季度下跌最多");
		Collections.sort(funds, new ComparatorFund(SortTypeEnum.THIS_SEASON_DESC.getCode()));
		printList(funds, funds.size() - count, funds.size(), SortTypeEnum.THIS_SEASON_DESC.getCode());

		System.out.println("最近一天上涨最多");
		Collections.sort(funds, new ComparatorFund(SortTypeEnum.THIS_DAY_DESC.getCode()));
		printList(funds, 0, count, SortTypeEnum.THIS_DAY_DESC.getCode());
		// 最近一周上涨最多
		System.out.println("最近一周上涨最多");
		Collections.sort(funds, new ComparatorFund(SortTypeEnum.THIS_WEEK_DESC.getCode()));
		printList(funds, 0, count, SortTypeEnum.THIS_WEEK_DESC.getCode());
		// 最近一个月上涨最多
		System.out.println("最近一个月上涨最多");
		Collections.sort(funds, new ComparatorFund(SortTypeEnum.THIS_MONTH_DESC.getCode()));
		printList(funds, 0, count, SortTypeEnum.THIS_MONTH_DESC.getCode());
		// 最近一个季度上涨最多
		System.out.println("最近一个季度上涨最多");
		Collections.sort(funds, new ComparatorFund(SortTypeEnum.THIS_SEASON_DESC.getCode()));
		printList(funds, 0, count, SortTypeEnum.THIS_SEASON_DESC.getCode());

		System.out.print("最近一天下跌个数 : ");
		diffCount(funds, -1, SortTypeEnum.THIS_DAY_DESC.getCode());
		// 最近一周下跌个数
		System.out.print("最近一周下跌个数 : ");
		diffCount(funds, -1, SortTypeEnum.THIS_WEEK_DESC.getCode());
		// 最近一个月下跌个数
		System.out.print("最近一个月下跌个数 : ");
		diffCount(funds, -1, SortTypeEnum.THIS_MONTH_DESC.getCode());
		// 最近一个季度下跌个数
		System.out.print("最近一个季度下跌个数 : ");
		diffCount(funds, -1, SortTypeEnum.THIS_SEASON_DESC.getCode());

		System.out.print("最近一天上涨个数 : ");
		diffCount(funds, 1, SortTypeEnum.THIS_DAY_DESC.getCode());
		// 最近一周上涨个数
		System.out.print("最近一周上涨个数 : ");
		diffCount(funds, 1, SortTypeEnum.THIS_WEEK_DESC.getCode());
		// 最近一个月上涨个数
		System.out.print("最近一个月上涨个数 : ");
		diffCount(funds, 1, SortTypeEnum.THIS_MONTH_DESC.getCode());
		// 最近一个季度上涨个数
		System.out.print("最近一个季度上涨个数 : ");
		diffCount(funds, 1, SortTypeEnum.THIS_SEASON_DESC.getCode());

		System.out.println("估值上涨最多");
		Collections.sort(funds, new ComparatorFund(SortTypeEnum.appraisement_DESC.getCode()));
		printList(funds, 0, count, SortTypeEnum.appraisement_DESC.getCode());

		System.out.println("估值下跌最多");
		List<FundBean> fundUnUsed = new ArrayList<>();
		for (FundBean fund : funds) {
			if ("".equals(fund.getAppraisement().trim())) {
				fundUnUsed.add(fund);
			}
		}
		funds.removeAll(fundUnUsed);
		Collections.sort(funds, new ComparatorFund(SortTypeEnum.appraisement_DESC.getCode()));
		printList(funds, funds.size() - count, funds.size(), SortTypeEnum.appraisement_DESC.getCode());
	}

	private static String printAppraisement(List<FundBean> funds) {
		int asc = 0;
		int desc = 0;
		for (FundBean fund : funds) {
			if (StringUtils.isEmpty(fund.getAppraisement())) {
				continue;
			}

			if (new BigDecimal(fund.getAppraisement().trim()).compareTo(BigDecimal.ZERO) > 0) {
				asc++;
			}

			if (new BigDecimal(fund.getAppraisement().trim()).compareTo(BigDecimal.ZERO) < 0) {
				desc++;
			}
		}

		String content = "预估上涨数量: " + asc + "  预估下跌数量: " + desc + " 总: " + funds.size();
		System.out.println(content);
		return content;
	}

	/**
	 * 
	 * @param funds
	 * @param type
	 *            >0上涨 <0下降
	 * @param sortType
	 */
	private static String diffCount(List<FundBean> funds, int type, int sortType) {
		int count = 0;
		for (FundBean fund : funds) {
			try {

				if (sortType == SortTypeEnum.THIS_DAY_DESC.getCode()) {
					if (new BigDecimal(fund.getDayInc()).compareTo(BigDecimal.ZERO) > 0 && type > 0) {
						count++;
					}

					if (new BigDecimal(fund.getDayInc()).compareTo(BigDecimal.ZERO) < 0 && type < 0) {
						count++;
					}
				}

				// 周
				if (sortType == SortTypeEnum.THIS_WEEK_DESC.getCode()) {
					if (new BigDecimal(fund.getWeekInc()).compareTo(BigDecimal.ZERO) > 0 && type > 0) {
						count++;
					}

					if (new BigDecimal(fund.getWeekInc()).compareTo(BigDecimal.ZERO) < 0 && type < 0) {
						count++;
					}
				}

				// 月
				if (sortType == SortTypeEnum.THIS_MONTH_DESC.getCode()) {
					if (new BigDecimal(fund.getMonthInc()).compareTo(BigDecimal.ZERO) > 0 && type > 0) {
						count++;
					}

					if (new BigDecimal(fund.getMonthInc()).compareTo(BigDecimal.ZERO) < 0 && type < 0) {
						count++;
					}
				}
				// 季度
				if (sortType == SortTypeEnum.THIS_SEASON_DESC.getCode()) {
					if (new BigDecimal(fund.getSeasonhInc()).compareTo(BigDecimal.ZERO) > 0 && type > 0) {
						count++;
					}

					if (new BigDecimal(fund.getSeasonhInc()).compareTo(BigDecimal.ZERO) < 0 && type < 0) {
						count++;
					}
				}
			} catch (Exception e) {

			}
		}
		System.out.println(count + " / " + funds.size());
		return count + " / " + funds.size();
	}

	private static void printList(List<FundBean> funds, int from, int to, int sortType) {
		for (FundBean fund : funds.subList(from, to)) {
			if (sortType == SortTypeEnum.THIS_DAY_DESC.getCode()) {
				System.out.println(fund.toDayString());
			}

			// 周
			if (sortType == SortTypeEnum.THIS_WEEK_DESC.getCode()) {
				System.out.println(fund.toWeekString());
			}

			// 月
			if (sortType == SortTypeEnum.THIS_MONTH_DESC.getCode()) {
				System.out.println(fund.toMonthString());
			}
			// 季度
			if (sortType == SortTypeEnum.THIS_SEASON_DESC.getCode()) {
				System.out.println(fund.toSeasonString());
			}

			if (sortType == SortTypeEnum.appraisement_DESC.getCode()) {
				System.out.println(fund.toString());
			}
		}
	}

	private static void catchAreaDatas(List<FundBean> funds, int rateStart, int rateEnd) {
		System.out.println("==============增长率" + rateStart + "~" + rateEnd + "===================");
		HashSet<String> fundCodes = new HashSet();
		for (FundBean fund : funds) {
			// 去重
			try {
				if (fundCodes.add(fund.getFundCode())) {
					BigDecimal preYearInc = new BigDecimal(fund.getPreYearInc());
					BigDecimal preTwoYearInc = new BigDecimal(fund.getPreTwoYearInc());
					if (rateEnd > preYearInc.doubleValue() && preYearInc.doubleValue() > rateStart
							&& rateEnd > preTwoYearInc.doubleValue() && preTwoYearInc.doubleValue() > rateStart) {
						System.out.println(fund.toString());
					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	/**
	 * 
	 * @param list
	 * @param sortEnum
	 * @param increaRate
	 *            增长率
	 */
	private static List<FundBean> printKeepIncrea(List<FundBean> list, List<Integer> sortEnum, int rateStart,
			int rateEnd, String[] unIncludeds) {
		List<FundBean> resultFunds = new ArrayList<>();
		System.out.println("==============增长率" + rateStart + "~" + rateEnd + "===================");
		Map<String, Integer> map = new HashMap<String, Integer>();
		Map<String, Integer> fundMap = new HashMap<String, Integer>();
		for (Integer sortEnumValue : sortEnum) {
			System.out.println("==============" + SortTypeEnum.getName(sortEnumValue) + "===================");
			Collections.sort(list, new ComparatorFund(sortEnumValue));
			for (FundBean fund : list) {
				if (fund.getPreYearInc() == null || fund.getPreTwoYearInc() == null) {
					continue;
				}

				boolean isSkip = false;
				for (String unIncluded : unIncludeds) {
					if (fund.getFundType().contains(unIncluded)) {
						isSkip = true;
						break;
					}
				}

				if (isSkip) {
					continue;
				}

				BigDecimal preYearInc = new BigDecimal(fund.getPreYearInc());
				BigDecimal preTwoYearInc = new BigDecimal(fund.getPreTwoYearInc());
				if (rateEnd > preYearInc.doubleValue() && preYearInc.doubleValue() > rateStart
						&& rateEnd > preTwoYearInc.doubleValue() && preTwoYearInc.doubleValue() > rateStart) {
					anlyManger(fund.getManagers(), map);
					anlyFund(fund.getFundCode(), fundMap);
					System.out.println(fund.toString());
					resultFunds.add(fund);
				}
			}
		}

		System.out.println("\r\n");
		// 最佳经理
		for (Entry<String, Integer> man : map.entrySet()) {
			System.out.print(man.getKey() + " : " + man.getValue() + "; ");
		}

		System.out.println("\r\n");
		// 最佳基金
		for (Entry<String, Integer> fund : fundMap.entrySet()) {
			System.out.print(fund.getKey() + " : " + fund.getValue() + "; ");
		}

		System.out.println("\r\n");
		return resultFunds;
	}

	private static void printByDiffSort(List<FundBean> list, List<Integer> sortEnum, int count) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		Map<String, Integer> fundMap = new HashMap<String, Integer>();
		for (Integer sortEnumValue : sortEnum) {
			System.out.println("==============" + SortTypeEnum.getName(sortEnumValue) + "===================");
			Collections.sort(list, new ComparatorFund(sortEnumValue));
			for (int i = 0; i < count; i++) {
				anlyManger(list.get(i).getManagers(), map);
				anlyFund(list.get(i).getFundCode(), fundMap);
				System.out.println(list.get(i).toString());
			}
		}

		System.out.println("\r\n");
		// 最佳经理
		for (Entry<String, Integer> man : map.entrySet()) {
			System.out.print(man.getKey() + " : " + man.getValue() + "; ");
		}

		System.out.println("\r\n");
		// 最佳基金
		for (Entry<String, Integer> fund : fundMap.entrySet()) {
			System.out.print(fund.getKey() + " : " + fund.getValue() + "; ");
		}

		System.out.println("\r\n");
	}

	public static void anlyFund(String fundName, Map<String, Integer> fundMap) {
		if (fundMap.containsKey(fundName)) {
			fundMap.put(fundName, fundMap.get(fundName) + 1);
		} else {
			fundMap.put(fundName, 1);
		}
	}

	public static void anlyManger(List<String> managers, Map<String, Integer> map) {

		if (managers == null) {
			return;
		}
		for (int i = 0; i < managers.size(); i++) {
			if (map.containsKey(managers.get(i))) {
				map.put(managers.get(i), map.get(managers.get(i)) + 1);
			} else {
				map.put(managers.get(i), 1);
			}
		}

	}

}
