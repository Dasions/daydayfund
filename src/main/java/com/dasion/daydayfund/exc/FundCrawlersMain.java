package com.dasion.daydayfund.exc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dasion.daydayfund.constant.RedisConstant;
import com.dasion.daydayfund.crawler.DaydayFundCrawler;
import com.dasion.daydayfund.fund.BaseBean;
import com.dasion.daydayfund.tool.BeanContext;
import com.dasion.daydayfund.tool.HttpclientTool;
import com.dasion.daydayfund.tool.JedisTool;
import com.google.gson.Gson;

import redis.clients.jedis.Jedis;

@Component
public class FundCrawlersMain implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(FundCrawlersMain.class);

	@Override
	public void run() {
		Gson gson = new Gson();
		String currentyDate = DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd"));
		JedisTool jedisTool = BeanContext.getBean(JedisTool.class);
		DaydayFundCrawler daydayFundCrawler = BeanContext.getBean(DaydayFundCrawler.class);
		String sourceQueueName = RedisConstant.SOURCE_DATA_QUEUE + currentyDate;
		while (true) {
			BaseBean baseBean = null;
			// 根据统一开关决定是否执行 爬取动作
			try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
				if (RedisConstant.SEMAPHORE_KEY_STOP.equals(jedis.get(RedisConstant.SEMAPHORE_KEY))) {
					logger.warn("统一开关状态是" + jedis.get(RedisConstant.SEMAPHORE_KEY) + " 统一执行接口将停止执行!");
					return;
				}

				if (!jedis.exists(sourceQueueName) || jedis.llen(sourceQueueName).intValue() == 0) {
					logger.info("---------------sourceQueue队列数据处理完毕,  统一执行接口将停止执行!");
					return;
				}
				baseBean = gson.fromJson(jedis.rpop(sourceQueueName), BaseBean.class);
			}
			if (baseBean == null) {
				logger.error("---------------sourceQueue队列数据 is empty,  统一执行接口将停止执行!---------------------");
				return;
			}
			try {
				switch (baseBean.getCrawlerType()) {
				case "daydayfund":
					daydayFundCrawler.excRoute(baseBean, getHttpClient());
					break;
				default:
					logger.error("未定义爬虫类型： " + baseBean.getCrawlerType());
					break;
				}
			} catch (Exception e) {
				logger.error("爬虫统一执行接口发生异常：e:{}, baseBean:{}", e, gson.toJson(baseBean));
			}
		}

	}

	private HttpclientTool getHttpClient() {
		HttpclientTool tool = new HttpclientTool();
		tool.createDefaultConfigClientByHttpClients();
		tool.setDefaultCookieStore();
		tool.setDefaultHeader();
		return tool;
	}
}
