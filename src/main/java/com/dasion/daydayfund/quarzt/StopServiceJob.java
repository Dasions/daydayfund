package com.dasion.daydayfund.quarzt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import com.dasion.daydayfund.constant.RedisConstant;
import com.dasion.daydayfund.exc.FundExcut;
import com.dasion.daydayfund.fund.FundBean;
import com.dasion.daydayfund.mail.MailTemplate;
import com.dasion.daydayfund.tool.JedisTool;
import com.google.gson.Gson;

import redis.clients.jedis.Jedis;

public class StopServiceJob implements Job {

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
		String nowDate = DateTime.now().toString(format);
		try (Jedis jedis = JedisTool.getInstance().getResource()) {
			System.out.println("守护任务: " + jedis.get(RedisConstant.SEMAPHORE_KEY));
			if (RedisConstant.SEMAPHORE_KEY_RUN.equals(jedis.get(RedisConstant.SEMAPHORE_KEY))) {
				if (jedis.exists(nowDate + "-partOneQueue") == false 
						&& jedis.exists(nowDate + "-partTwoQueue") == false
						&& jedis.exists(nowDate + "-partThreeQueue") == true) {
					jedis.set(RedisConstant.SEMAPHORE_KEY, RedisConstant.SEMAPHORE_KEY_STOP);
					try {
						TimeUnit.MILLISECONDS.sleep(30000);
						jedis.set(RedisConstant.SEMAPHORE_KEY, RedisConstant.SEMAPHORE_KEY_WAIT);
						List<FundBean> gongYinFunds = new ArrayList<>(1000);

						Gson gson = new Gson();
						String partThreeQueue = nowDate + "-partThreeQueue";
						int len = jedis.llen(partThreeQueue).intValue();
						List<String> jsonStr = jedis.lrange(partThreeQueue, 0, len - 1);
						for (String json : jsonStr) {
							gongYinFunds.add(gson.fromJson(json, FundBean.class));
						}

						MailTemplate.megerDataAndTemplate(gongYinFunds, null);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}
