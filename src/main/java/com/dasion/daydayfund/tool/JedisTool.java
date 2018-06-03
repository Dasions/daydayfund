package com.dasion.daydayfund.tool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dasion.daydayfund.constant.ConfigConstant;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Component
public class JedisTool{

	@Autowired
	private ConfigConstant configConstant;
	// 可用连接实例的最大数目，默认为8；
	// 如果赋值为-1，则表示不限制，如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)
	private static Integer MAX_TOTAL = 1024;
	// 控制一个pool最多有多少个状态为idle(空闲)的jedis实例，默认值是8
	private static Integer MAX_IDLE = 100;
	// 等待可用连接的最大时间，单位是毫秒，默认值为-1，表示永不超时。
	// 如果超过等待时间，则直接抛出JedisConnectionException
	private static Integer MAX_WAIT_MILLIS = 100000;
	private static Integer TIMEOUT = 100000;
	// 在borrow(用)一个jedis实例时，是否提前进行validate(验证)操作；
	// 如果为true，则得到的jedis实例均是可用的
	private static Boolean TEST_ON_BORROW = true;

	private static volatile JedisPool pool;

	private JedisTool(){
	
	}
	public JedisPool getJedisTool() {
		if (pool == null) {
			synchronized (JedisTool.class) {
				if (pool == null) {
					JedisPoolConfig config = new JedisPoolConfig();
					/*
					 * 注意： 在高版本的jedis jar包，比如本版本2.9.0，
					 * JedisPoolConfig没有setMaxActive和setMaxWait属性了
					 * 这是因为高版本中官方废弃了此方法，用以下两个属性替换。 maxActive ==> maxTotal
					 * maxWait==> maxWaitMillis
					 */
					config.setMaxTotal(MAX_TOTAL);
					config.setMaxIdle(MAX_IDLE);
					config.setMaxWaitMillis(MAX_WAIT_MILLIS);
					config.setTestOnBorrow(TEST_ON_BORROW);
					pool = new JedisPool(config, configConstant.getRedisIp(), configConstant.getRedisPort(), TIMEOUT, configConstant.getRedisPwd());
				}
			}
		}
		return pool;
	}
}
