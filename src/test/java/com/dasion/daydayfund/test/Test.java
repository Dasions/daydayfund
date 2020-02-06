package com.dasion.daydayfund.test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.quartz.SchedulerException;
import com.dasion.daydayfund.fund.FundBean;
import com.dasion.daydayfund.mail.MailTemplate;
import com.dasion.daydayfund.tool.JedisTool;
import com.google.gson.Gson;

import redis.clients.jedis.Jedis;

public class Test{

	public static void main(String[] args) throws SchedulerException, InterruptedException {
		BigDecimal count = new BigDecimal("7995");
		BigDecimal detailCount = new BigDecimal("3084" );

		System.out.println( "当前进度：" + (detailCount.divide(count,2, BigDecimal.ROUND_HALF_UP).floatValue()*0.4)*100 + " %"+ " detailCount " + detailCount.intValue() + " count " + count.intValue());

	}
	

	public static void test(){
//		List<FundBean> gongYinFunds = new ArrayList<>(1000);
//
//		Gson gson = new Gson();
//		String partThreeQueue = "2018-03-09-partThreeQueue";//DateTime.now().toString(format) + "-partThreeQueue";
//		try (Jedis jedis = JedisTool.getInstance().getResource()) {
//			int len = jedis.llen(partThreeQueue).intValue();
//			List<String> jsonStr = jedis.lrange(partThreeQueue, 0, len - 1);
//			for (String json : jsonStr) {
//				gongYinFunds.add(gson.fromJson(json, FundBean.class));
//			}
//		}
		//MailTemplate.megerDataAndTemplate(gongYinFunds, null);
	}

}
