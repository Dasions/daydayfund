package com.dasion.daydayfund.fund;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorServiceTool {
	static ExecutorService executor = Executors.newCachedThreadPool();

	public static ExecutorService getExecutor() {
		return executor;
	}
	
	
}
