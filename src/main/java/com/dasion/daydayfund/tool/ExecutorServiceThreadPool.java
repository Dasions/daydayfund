package com.dasion.daydayfund.tool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorServiceThreadPool {
	static ExecutorService executor = Executors.newCachedThreadPool();

	public static ExecutorService getExecutor() {
		return executor;
	}
	
	
}
