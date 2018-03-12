package com.dasion.daydayfund.quarzt;

import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

public class SingleSchedulerFactory {
	private final static SchedulerFactory schedulerFactory = new StdSchedulerFactory();

	private SingleSchedulerFactory(){
		
	}
	
	public static SchedulerFactory getSchedulerFactory(){
		return schedulerFactory;
	}
}
