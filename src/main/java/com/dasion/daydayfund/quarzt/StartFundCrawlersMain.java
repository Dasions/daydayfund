package com.dasion.daydayfund.quarzt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dasion.daydayfund.constant.HtmlConstant;
import com.dasion.daydayfund.constant.RedisConstant;
import com.dasion.daydayfund.enums.DaydayFundExcStepEnum;
import com.dasion.daydayfund.exc.FundCrawlersMain;
import com.dasion.daydayfund.fund.BaseBean;
import com.dasion.daydayfund.tool.ExecutorServiceThreadPool;
import com.dasion.daydayfund.tool.HttpclientTool;
import com.dasion.daydayfund.tool.JedisTool;
import com.google.gson.Gson;

import redis.clients.jedis.Jedis;
@Component
public class StartFundCrawlersMain{
	private static final Logger logger = LoggerFactory.getLogger(StartFundCrawlersMain.class);
	@Autowired
	private JedisTool jedisTool;
	private String FUND_COMPANY_URL ="http://fund.eastmoney.com/Company/home/KFSFundRank?gsid=";
	

	public void execute(){
		try {
			DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
			String nowDate = DateTime.now().toString(format);
			List<String> fndCompanys = getFundCompanysByHttp();
			String sourceQueueName = RedisConstant.SOURCE_DATA_QUEUE + nowDate;
			String finalQueueName = RedisConstant.FINAL_DATA_QUEUE + nowDate;
			Gson gson = new Gson();
			ExecutorService executor = ExecutorServiceThreadPool.getExecutor();
				delOldData(sourceQueueName);
				delOldData(finalQueueName);
				try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
				//开启统一开关
				logger.info("------开启统一开关------状态: " + RedisConstant.SEMAPHORE_KEY_RUN);
				jedis.set(RedisConstant.SEMAPHORE_KEY, RedisConstant.SEMAPHORE_KEY_RUN);
				for (String fundCompany : fndCompanys) {
						BaseBean baseBean = new BaseBean("daydayfund", DaydayFundExcStepEnum.BASE_INFO.getCode()
								, FUND_COMPANY_URL + fundCompany
								, sourceQueueName, sourceQueueName);
						jedis.lpush(sourceQueueName, gson.toJson(baseBean));
					}
				}
				
				int i = 0;
				while(i < 10){
					i++;
					executor.execute(new FundCrawlersMain());
				}

		} catch (Exception e) {
			logger.error("统一入口发生异常： " , e);
		}
	}

	private void delOldData(String queueName) {
		try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
			jedis.del(queueName);
		}
	}
	
	private static List<String> getFundCompanysByHttp() throws IOException {
		HttpclientTool tool = new HttpclientTool();
		tool.createDefaultConfigClientByHttpClients();
		tool.setDefaultCookieStore();
		tool.setDefaultHeader();
		tool.setUrl("http://fund.eastmoney.com/company/default.html");
		String fundListHtml = tool.getContentByGet();
		tool.getClient().close();
		Document fundListDoc = Jsoup.parse(fundListHtml);

		Elements trs = fundListDoc.getElementById("gspmTbl").getElementsByTag(HtmlConstant.TBODY).get(0)
				.getElementsByTag(HtmlConstant.TR);
		List<String> cps = new ArrayList<>();
		for (Element tr : trs) {
			String str = tr.getElementsByTag(HtmlConstant.TD).get(1).getElementsByTag(HtmlConstant.A).get(0)
					.attr(HtmlConstant.HREF);
			cps.add(str.substring(str.lastIndexOf("/") + 1, str.indexOf(".")));
		}
		return cps;
	}
}
