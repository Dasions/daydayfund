package com.dasion.daydayfund.quarzt;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import com.dasion.daydayfund.tool.JedisTool;

import redis.clients.jedis.Jedis;

public class QuarztSchedukeMonitor implements Job {
	private Map<String, List<Trigger>> schedulerConfigMap;
	private Scheduler scheduler;
	@SuppressWarnings("unchecked")
	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap dataMap = context.getJobDetail().getJobDataMap();
		schedulerConfigMap = (Map<String, List<Trigger>>) dataMap.get("schedulerConfigMap");
		scheduler = (Scheduler) dataMap.get("scheduler");
		for(Entry<String, List<Trigger>> entry : schedulerConfigMap.entrySet()){
			try (Jedis jedis = JedisTool.getInstance().getResource()) {
				if(jedis.exists(entry.getKey())){
					String schedulerTime = jedis.get(entry.getKey());
					for(Trigger trigger : entry.getValue()){
						scheduler.rescheduleJob(trigger.getKey(), getTrigger(schedulerTime));
					}
				}
			} catch (SchedulerException e) {
				e.printStackTrace();
			}
		}
		
	}

	private Trigger getTrigger(String schedulerTime){
        Trigger trigger = TriggerBuilder.newTrigger().startNow()
				 .withSchedule(CronScheduleBuilder.cronSchedule(schedulerTime)).build();
        return trigger;
	}
}
