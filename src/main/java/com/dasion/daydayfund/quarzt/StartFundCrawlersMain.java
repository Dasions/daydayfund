package com.dasion.daydayfund.quarzt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.dasion.daydayfund.constant.ConfigConstant;
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
	@Autowired
	private ConfigConstant configConstant;

	public void execute(){
		String semaphore = null;
		try {
			//获取SEMAPHORE_KEY的值，如果为RUN，则说明当前服务为从节点，不需要获取基金列表和
			//设置SEMAPHORE_KEY为run， 直接往线程池添加FundCrawlersMain对象执行爬取任务即可
			try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
				semaphore = jedis.get(RedisConstant.SEMAPHORE_KEY);
			}

            if(RedisConstant.SEMAPHORE_KEY_WAIT.equals(semaphore)){
				DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
				String nowDate = DateTime.now().toString(format);
				List<String> fndCompanys = getFundCompanysByHttp();
				String sourceQueueName = RedisConstant.SOURCE_DATA_QUEUE + nowDate;
				String finalQueueName = RedisConstant.FINAL_DATA_QUEUE + nowDate;
				Gson gson = new Gson();
				delOldData(sourceQueueName);
				delOldData(finalQueueName);

				initFundListAndSetSemaphoreKeyToRun( fndCompanys, sourceQueueName, gson );
			}
			String ip = "";
			try {
				ip = InetAddress.getLocalHost().getHostAddress();
			} catch (Exception e) {
				logger.error("获取机器实例IP发生异常，{}", e);
			}
			try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
				//设置默认并发数
				jedis.set( RedisConstant.MAX_THREAD_NUM, configConstant.getMaxThreadNum() + "" );
				//设置当前实例最大并发数
				jedis.set( ip + "_" + RedisConstant.MAX_THREAD_NUM, configConstant.getMaxThreadNum() + "" );
			}
			ExecutorService executor = ExecutorServiceThreadPool.getExecutor();
			int i = 0;
			//这里为10，表示每个爬虫服务只会有10个线程进行数据的爬取，这个数量根据机器性能可以自己做调整
			while(i < 10){
				i++;
				executor.execute(new FundCrawlersMain());
			}

		} catch (Exception e) {
			logger.error("统一入口发生异常： " , e);
		}
	}

	private void initFundListAndSetSemaphoreKeyToRun(List<String> fndCompanys, String sourceQueueName, Gson gson) {
		try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
        //开启统一开关
        logger.info("------开启统一开关------状态: " + RedisConstant.SEMAPHORE_KEY_RUN);
        jedis.set(RedisConstant.SEMAPHORE_KEY, RedisConstant.SEMAPHORE_KEY_RUN);
	    jedis.set(RedisConstant.COUNT_BASE_INFO, "0");
	    jedis.set(RedisConstant.COUNT_DETAIL_INFO, "0");
		jedis.set(RedisConstant.COUNT_INC_INFO, "0");
        for (String fundCompany : fndCompanys) {
                BaseBean baseBean = new BaseBean("daydayfund", DaydayFundExcStepEnum.BASE_INFO.getCode()
                        , FUND_COMPANY_URL + fundCompany
                        , sourceQueueName, sourceQueueName);
                jedis.lpush(sourceQueueName, gson.toJson(baseBean));
            }
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
