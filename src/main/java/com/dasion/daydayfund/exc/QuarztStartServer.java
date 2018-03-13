package com.dasion.daydayfund.exc;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import java.util.concurrent.atomic.AtomicInteger;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;

import com.dasion.daydayfund.constant.QuarztConstant;
import com.dasion.daydayfund.constant.RedisConstant;
import com.dasion.daydayfund.quarzt.DayDayFundJob;
import com.dasion.daydayfund.quarzt.SingleSchedulerFactory;
import com.dasion.daydayfund.quarzt.StopServiceJob;
import com.dasion.daydayfund.tool.JedisTool;

import redis.clients.jedis.Jedis;

public class QuarztStartServer {
	public static void main(String[] args) throws SchedulerException {
		SchedulerFactory schedulerFactory = SingleSchedulerFactory.getSchedulerFactory();
		Scheduler scheduler = schedulerFactory.getScheduler();

		AtomicInteger threadNums = new AtomicInteger();
		JobDetail partOne = concatPartOneJobDetail(RedisConstant.PARTONEQUEUE_QUEUENAME);
		JobDetail partTwo = concatPartTwoJobDetail(RedisConstant.PARTONEQUEUE_QUEUENAME, RedisConstant.PARTTWOQUEUE_QUEUENAME, threadNums);
		JobDetail partThree = concatPartThreeJobDetail(RedisConstant.PARTTWOQUEUE_QUEUENAME, RedisConstant.PARTTHREEQUEUE_QUEUENAME, threadNums);

		JobDetail stopServiceJob = newJob(StopServiceJob.class).build();
		SimpleTrigger stopServiceTrigger = (SimpleTrigger) TriggerBuilder.newTrigger().startNow()
				.withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever()).build();
		scheduler.scheduleJob(stopServiceJob, stopServiceTrigger);
		String excTime = "0 15 11,14,20,22 ? * MON-FRI";
		try (Jedis jedis = JedisTool.getInstance().getResource()) {
			excTime = jedis.get("excTime");
		}
		//0 15 11,14,20,22 ? * MON-FRI
		CronTrigger partOneTrigger = getTrigger(excTime);
		scheduler.scheduleJob(partOne, partOneTrigger);
		CronTrigger partTwoTrigger = getTrigger(excTime);
		scheduler.scheduleJob(partTwo, partTwoTrigger);
		CronTrigger partThreeTrigger = getTrigger(excTime);
		scheduler.scheduleJob(partThree, partThreeTrigger);
		
		// 调度启动
		scheduler.start();
	}

	private static JobDetail concatPartOneJobDetail(String queueOne) {
		JobDetail partOne = newJob(DayDayFundJob.class)
				.usingJobData(QuarztConstant.URL, "http://fund.eastmoney.com/Company/home/KFSFundRank?gsid=")
				.usingJobData(QuarztConstant.OUTPUTQUEUENAME, queueOne)
				.usingJobData(QuarztConstant.JOBTYPE, QuarztConstant.PARTONE_JOBTYPE).build();
		return partOne;
	}

	private static JobDetail concatPartTwoJobDetail(String queueOne, String queueTwo, AtomicInteger threadNums) {
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put(QuarztConstant.JOBTYPE, QuarztConstant.PARTTWO_JOBTYPE);
		jobDataMap.put(QuarztConstant.INPUTQUEUENAME, queueOne);
		jobDataMap.put(QuarztConstant.OUTPUTQUEUENAME, queueTwo);
		jobDataMap.put(QuarztConstant.NOWDATE, null);
		jobDataMap.put(QuarztConstant.THREADNUMS, threadNums);
		JobDetail partTwo = newJob(DayDayFundJob.class).setJobData(jobDataMap).build();
		return partTwo;
	}

	private static JobDetail concatPartThreeJobDetail(String queueTwo, String queueThree, AtomicInteger threadNums) {
		JobDataMap jobDataMap;
		jobDataMap = new JobDataMap();
		jobDataMap.put(QuarztConstant.JOBTYPE, QuarztConstant.PARTTHREE_JOBTYPE);
		jobDataMap.put(QuarztConstant.INPUTQUEUENAME, queueTwo);
		jobDataMap.put(QuarztConstant.OUTPUTQUEUENAME, queueThree);
		jobDataMap.put(QuarztConstant.NOWDATE, null);
		jobDataMap.put(QuarztConstant.THREADNUMS, threadNums);
		JobDetail partThree = newJob(DayDayFundJob.class).setJobData(jobDataMap)
				.build();
		return partThree;
	}
	
	private static CronTrigger getTrigger(String schedulerTime) throws SchedulerException{
		CronTrigger trigger = TriggerBuilder.newTrigger().startNow()
				 .withSchedule(CronScheduleBuilder.cronSchedule(schedulerTime)).build();
        return trigger;
	}

}