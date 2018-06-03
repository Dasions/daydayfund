package com.dasion.daydayfund.constant;

public class RedisConstant {
	public static final String SEMAPHORE_KEY = "isStop";
	public static final String SEMAPHORE_KEY_RUN = "run";
	public static final String SEMAPHORE_KEY_STOP = "stop";
	public static final String SEMAPHORE_KEY_WAIT = "wait";
	/**
	 * 需要处理的队列数据
	 */
	public static final String SOURCE_DATA_QUEUE = "sourceDataQueue_";
	/**
	 * 最终的数据
	 */
	public static final String FINAL_DATA_QUEUE = "finalDataQueue_";
}
