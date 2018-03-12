package com.dasion.daydayfund.exc;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import com.dasion.daydayfund.constant.QuarztConstant;
import com.dasion.daydayfund.constant.RedisConstant;
import com.dasion.daydayfund.quarzt.DayDayFundJob;
import com.dasion.daydayfund.quarzt.QuarztSchedukeMonitor;
import com.dasion.daydayfund.quarzt.SingleSchedulerFactory;
import com.dasion.daydayfund.quarzt.StopServiceJob;

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
		//0 15 11,14,20,22 ? * MON-FRI
		Trigger partOneTrigger = getTrigger("0 15 11,14,20,22 ? * MON-FRI");
		scheduler.scheduleJob(partOne, partOneTrigger);
		Trigger partTwoTrigger = getTrigger("0 15 11,14,20,22 ? * MON-FRI");
		scheduler.scheduleJob(partTwo, partTwoTrigger);
		Trigger partThreeTrigger = getTrigger("0 15 11,14,20,22 ? * MON-FRI");
		scheduler.scheduleJob(partThree, partThreeTrigger);
		
        List<Trigger> triggerList = new ArrayList<>();
        triggerList.add(partOneTrigger);
        triggerList.add(partTwoTrigger);
        triggerList.add(partThreeTrigger);
		Map<String, List<Trigger>> schedulerConfigMap = new HashMap<>();
		schedulerConfigMap.put("daydayfundscheduler", triggerList);
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("schedulerConfigMap", schedulerConfigMap);
		jobDataMap.put("scheduler", scheduler);
		JobDetail quarztSchedukeMonitorJob = newJob(QuarztSchedukeMonitor.class).setJobData(jobDataMap)
				.build();
		SimpleTrigger quarztSchedukeMonitorTrigger = (SimpleTrigger) TriggerBuilder.newTrigger().startNow()
				.withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever()).build();
		scheduler.scheduleJob(quarztSchedukeMonitorJob, quarztSchedukeMonitorTrigger);
		
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
	
	private static Trigger getTrigger(String schedulerTime) throws SchedulerException{
        Trigger trigger = TriggerBuilder.newTrigger().startNow()
				 .withSchedule(CronScheduleBuilder.cronSchedule(schedulerTime)).build();
        return trigger;
	}

}