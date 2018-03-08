package com.dasion.daydayfund.fund;

import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.dasion.daydayfund.constant.HtmlConstant;
import com.dasion.daydayfund.constant.RedisConstant;
import com.dasion.daydayfund.tool.HttpclientTool;
import com.dasion.daydayfund.tool.JedisTool;
import com.google.gson.Gson;

import redis.clients.jedis.Jedis;

/**
 * 获取基金公司的基金列表数据
 * @author Dasion-PC
 *
 */
public class DayDayFundBaseInfo implements Runnable {
	private static final String EMPTY = "";
	private static final String ZONE_STRING = "0";
	
	private String taskQueue;
	private String url;


	public DayDayFundBaseInfo(String taskQueue, String url) {
		super();
		this.taskQueue = taskQueue;
		this.url = url;
	}

	public void getFundBaseinfo() throws IOException, InterruptedException {
		HttpclientTool tool = getHttpClient();
		tool.setUrl(url);
		String fundListHtml = tool.getContentByGet();
		tool.getClient().close();
		Document fundListDoc = Jsoup.parse(fundListHtml);
		Elements fundListTable = fundListDoc.getElementsByClass("ttjj-table ttjj-table-hover common-sort-table");

		if (fundListTable.size() > 0) {
			Elements fundListTrs = fundListTable.get(0).getElementsByTag(HtmlConstant.TBODY).get(0).getElementsByTag(HtmlConstant.TR);
			for (Element fundTr : fundListTrs) {
				Elements fundTds = fundTr.getElementsByTag(HtmlConstant.TD);

				if(fundTds.get(0).getElementsByTag(HtmlConstant.A).size() < 1){
					continue;
				}
				
				FundBean fund = concatFundBean(fundTds);

				try (Jedis jedis = JedisTool.getInstance().getResource()) {
					Gson gson = new Gson();
					jedis.lpush(taskQueue, gson.toJson(fund));
				}
			}
			try (Jedis jedis = JedisTool.getInstance().getResource()) {
				jedis.set(RedisConstant.SEMAPHORE_KEY, RedisConstant.SEMAPHORE_KEY_RUN);
			}
		}
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

	private HttpclientTool getHttpClient() {
		HttpclientTool tool = new HttpclientTool();
		tool.createDefaultConfigClientByHttpClients();
		tool.setDefaultCookieStore();
		tool.setDefaultHeader();
		return tool;
	}

	private String format(String text) {
		if ("-".equals(text.trim())) {
			return ZONE_STRING;
		}

		return text.trim().replace("%", EMPTY);
	}

	@Override
	public void run() {
		try {
			getFundBaseinfo();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

	}
}
