package com.dasion.daydayfund.exc;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import com.dasion.daydayfund.constant.QuarztConstant;
import com.dasion.daydayfund.constant.RedisConstant;
import com.dasion.daydayfund.quarzt.DayDayFundJob;
import com.dasion.daydayfund.quarzt.StopServiceJob;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class QuarztStartServer {
	public static void main(String[] args) throws SchedulerException {
		SchedulerFactory schedulerFactory = new StdSchedulerFactory();
		Scheduler scheduler = schedulerFactory.getScheduler();
		String queueOne = RedisConstant.PARTONEQUEUE_QUEUENAME;
		String queueTwo = RedisConstant.PARTTWOQUEUE_QUEUENAME;
		String queueThree = RedisConstant.PARTTHREEQUEUE_QUEUENAME;
		AtomicInteger threadNums = new AtomicInteger();
	
		JobDetail partOne = newJob(DayDayFundJob.class).withIdentity("partOne", "funds")
				.usingJobData(QuarztConstant.URL, "http://fund.eastmoney.com/Company/home/KFSFundRank?gsid=")
				.usingJobData(QuarztConstant.OUTPUTQUEUENAME, queueOne)
				.usingJobData(QuarztConstant.JOBTYPE, QuarztConstant.PARTONE_JOBTYPE).build();

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

		JobDetail stopServiceJob = newJob(StopServiceJob.class).withIdentity("StopServiceJob", "funds1")
				.build();

		// 在当前时间1秒后运行
		 Date startTime = DateBuilder.nextGivenSecondDate(null, 1);
		
		CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity("cronTrigger",
				 "funds").startAt(startTime)
				 .withSchedule(CronScheduleBuilder.cronSchedule("0 15 11,14,20,22 ? * MON-FRI")).build();
		scheduler.scheduleJob(partOne, cronTrigger);

		cronTrigger = TriggerBuilder.newTrigger().withIdentity("cronTrigger2",
				 "funds").startAt(startTime)
				 .withSchedule(CronScheduleBuilder.cronSchedule("0 15 11,14,20,22 ? * MON-FRI")).build();
		scheduler.scheduleJob(partTwo, cronTrigger);

		cronTrigger = TriggerBuilder.newTrigger().withIdentity("cronTrigger3",
				 "funds").startAt(startTime)
				 .withSchedule(CronScheduleBuilder.cronSchedule("0 15 11,14,20,22 ? * MON-FRI")).build();
		scheduler.scheduleJob(partThree, cronTrigger);

		
//		SimpleTrigger cronTrigger = (SimpleTrigger) TriggerBuilder.newTrigger()
//				.withIdentity("cronTrigger1", "funds").startNow()
//				.withSchedule(simpleSchedule().withIntervalInHours(10).repeatForever()).build();
//		scheduler.scheduleJob(partOne, cronTrigger);
//		
//		cronTrigger = (SimpleTrigger) TriggerBuilder.newTrigger()
//				.withIdentity("cronTrigger2", "funds").startNow()
//				.withSchedule(simpleSchedule().withIntervalInHours(10).repeatForever()).build();
//		scheduler.scheduleJob(partTwo, cronTrigger);
//		
//		cronTrigger = (SimpleTrigger) TriggerBuilder.newTrigger()
//				.withIdentity("cronTrigger3", "funds").startNow()
//				.withSchedule(simpleSchedule().withIntervalInHours(10).repeatForever()).build();
//		scheduler.scheduleJob(partThree, cronTrigger);
		
		SimpleTrigger daemonCronTrigger = (SimpleTrigger) TriggerBuilder.newTrigger()
				.withIdentity("daemonCronTrigger", "funds1").startNow()
				.withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever()).build();
		scheduler.scheduleJob(stopServiceJob, daemonCronTrigger);
		// 调度启动
		scheduler.start();
	}
}