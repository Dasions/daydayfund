package com.dasion.daydayfund.quarzt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dasion.daydayfund.constant.RedisConstant;
import com.dasion.daydayfund.fund.FundBean;
import com.dasion.daydayfund.mail.MailTemplate;
import com.dasion.daydayfund.tool.JedisTool;
import com.google.gson.Gson;

import redis.clients.jedis.Jedis;
@Component
public class StopCrawlerServiceJob{
	private static final Logger logger = LoggerFactory.getLogger(StopCrawlerServiceJob.class);
	@Autowired
	private JedisTool jedisTool;
	@Autowired
	MailTemplate mailTemplate;

	public void execute() throws JobExecutionException {
		DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
		String nowDate = DateTime.now().toString(format);
		try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
			logger.info("守护任务: " + jedis.get(RedisConstant.SEMAPHORE_KEY));
			if (RedisConstant.SEMAPHORE_KEY_RUN.equals(jedis.get(RedisConstant.SEMAPHORE_KEY))) {
				if (jedis.exists(RedisConstant.SOURCE_DATA_QUEUE + nowDate) == false 
						&& jedis.exists(RedisConstant.FINAL_DATA_QUEUE + nowDate) == true) {
					jedis.set(RedisConstant.SEMAPHORE_KEY, RedisConstant.SEMAPHORE_KEY_STOP);
					try {
						TimeUnit.MILLISECONDS.sleep(30000);
						jedis.set(RedisConstant.SEMAPHORE_KEY, RedisConstant.SEMAPHORE_KEY_WAIT);
						List<FundBean> gongYinFunds = new ArrayList<>(1000);

						Gson gson = new Gson();
						String finalDataQueue = RedisConstant.FINAL_DATA_QUEUE + nowDate;
						int len = jedis.llen(finalDataQueue).intValue();
						List<String> jsonStr = jedis.lrange(finalDataQueue, 0, len - 1);
						for (String json : jsonStr) {
							gongYinFunds.add(gson.fromJson(json, FundBean.class));
						}

						mailTemplate.megerDataAndTemplate(gongYinFunds, null);
					} catch (Exception e) {
						logger.error("监控任务发生异常： " , e);
					}
				}
			}
		}
	}

}
