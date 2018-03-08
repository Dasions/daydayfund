package com.dasion.daydayfund.exc;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import com.dasion.daydayfund.constant.QuarztConstant;
import com.dasion.daydayfund.constant.RedisConstant;
import com.dasion.daydayfund.quarzt.DayDayFundJob;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalQuarztStart {
	public static void main(String[] args) throws SchedulerException {
		// 初始化一个Schedule工厂
		SchedulerFactory schedulerFactory = new StdSchedulerFactory();
		// 通过schedule工厂类获得一个Scheduler类,通过SchedulerFactory获取一个调度器实例
		Scheduler scheduler = schedulerFactory.getScheduler();

		DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
		String nowDate = DateTime.now().toString(format);
		String queueOne = nowDate + RedisConstant.PARTONEQUEUE_QUEUENAME;
		String queueTwo = nowDate + RedisConstant.PARTTWOQUEUE_QUEUENAME;
		String queueThree = nowDate + RedisConstant.PARTTHREEQUEUE_QUEUENAME;
		AtomicInteger threadNums = new AtomicInteger();

		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put(QuarztConstant.JOBTYPE, QuarztConstant.PARTTWO_JOBTYPE);
		jobDataMap.put(QuarztConstant.INPUTQUEUENAME, queueOne);
		jobDataMap.put(QuarztConstant.OUTPUTQUEUENAME, queueTwo);
		jobDataMap.put(QuarztConstant.NOWDATE, null);
		jobDataMap.put(QuarztConstant.THREADNUMS, threadNums);
		JobDetail partTwo = newJob(DayDayFundJob.class).withIdentity("partTwo", "funds").setJobData(jobDataMap).build();

		jobDataMap = new JobDataMap();
		jobDataMap.put(QuarztConstant.JOBTYPE, QuarztConstant.PARTTHREE_JOBTYPE);
		jobDataMap.put(QuarztConstant.INPUTQUEUENAME, queueTwo);
		jobDataMap.put(QuarztConstant.OUTPUTQUEUENAME, queueThree);
		jobDataMap.put(QuarztConstant.NOWDATE, null);
		jobDataMap.put(QuarztConstant.THREADNUMS, threadNums);
		JobDetail partThree = newJob(DayDayFundJob.class).withIdentity("partThree", "funds").setJobData(jobDataMap)
				.build();

		SimpleTrigger cronTrigger = (SimpleTrigger) TriggerBuilder.newTrigger().withIdentity("cronTrigger2", "funds").startNow()
				.withSchedule(simpleSchedule().withIntervalInHours(3).repeatForever()).build();
		scheduler.scheduleJob(partTwo, cronTrigger);

		cronTrigger = (SimpleTrigger) TriggerBuilder.newTrigger().withIdentity("cronTrigger3", "funds").startNow()
				.withSchedule(simpleSchedule().withIntervalInHours(3).repeatForever()).build();
		scheduler.scheduleJob(partThree, cronTrigger);

		// 调度启动
		scheduler.start();
	}
}