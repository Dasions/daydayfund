package com.dasion.daydayfund.quarzt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dasion.daydayfund.constant.HtmlConstant;
import com.dasion.daydayfund.constant.QuarztConstant;
import com.dasion.daydayfund.fund.DayDayFundBaseInfo;
import com.dasion.daydayfund.fund.DayDayFundDetailInfo;
import com.dasion.daydayfund.fund.DayDayFundIncInfo;
import com.dasion.daydayfund.fund.ExecutorServiceTool;
import com.dasion.daydayfund.tool.HttpclientTool;
import com.dasion.daydayfund.tool.JedisTool;

import redis.clients.jedis.Jedis;

public class DayDayFundJob implements Job {
	private String inputQueueName;
	private String outputQueueName;
	private String url;
	private String jobType;
	private AtomicInteger threadNums;

	public DayDayFundJob() {
	}

	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
			String nowDate = DateTime.now().toString(format);
			List<String> fndCompanys = getFundCompanysByHttp();
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();
			this.inputQueueName = nowDate + (String) dataMap.get(QuarztConstant.INPUTQUEUENAME);
			this.outputQueueName = nowDate + (String) dataMap.get(QuarztConstant.OUTPUTQUEUENAME);
			this.jobType = (String) dataMap.get(QuarztConstant.JOBTYPE);
			this.url = (String) dataMap.get(QuarztConstant.URL);
			this.threadNums = (AtomicInteger) dataMap.get(QuarztConstant.THREADNUMS);

			if (jobType == null) {
				return;
			}

			ExecutorService executor = ExecutorServiceTool.getExecutor();

			if (QuarztConstant.PARTONE_JOBTYPE.equals(jobType)) {
				delOldData(this.outputQueueName);
				for (String fundCompany : fndCompanys) {
					executor.execute(new DayDayFundBaseInfo(this.outputQueueName, this.url + fundCompany));
				}
			}

			if (QuarztConstant.PARTTWO_JOBTYPE.equals(jobType)) {
				delOldData(this.outputQueueName);
				executor.execute(
						new DayDayFundDetailInfo(this.inputQueueName, this.outputQueueName, null, threadNums));
				executor.execute(
						new DayDayFundDetailInfo(this.inputQueueName, this.outputQueueName, null, threadNums));
			}

			if (QuarztConstant.PARTTHREE_JOBTYPE.equals(jobType)) {
				delOldData(this.outputQueueName);
				executor.execute(new DayDayFundIncInfo(this.inputQueueName, this.outputQueueName, nowDate, threadNums));
				executor.execute(new DayDayFundIncInfo(this.inputQueueName, this.outputQueueName, nowDate, threadNums));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void delOldData(String queueName) {
		try (Jedis jedis = JedisTool.getInstance().getResource()) {
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