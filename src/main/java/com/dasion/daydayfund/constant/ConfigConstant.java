package com.dasion.daydayfund.constant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 配置文件
 * 
 * @author Dasion-PC
 *
 */

@Configuration
public class ConfigConstant {
	@Value("${redisIp}")
	private String redisIp;
	@Value("${redisPort}")
	private int redisPort;
	@Value("${redisPwd}")
	private String redisPwd;
	@Value("${senderMail}")
	private String senderMail;
	@Value("${mailPwd}")
	private String mailPwd;
	@Value("${reciverMail}")
	private String reciverMail;
	@Value("${mailTitle}")
	private String mailTitle;
	@Value("${maxThreadNum}")
	private int maxThreadNum;

	public int getMaxThreadNum() {
		return maxThreadNum;
	}

	public void setMaxThreadNum(int maxThreadNum) {
		this.maxThreadNum = maxThreadNum;
	}

	public String getRedisIp() {
		return redisIp;
	}

	public void setRedisIp(String redisIp) {
		this.redisIp = redisIp;
	}

	public int getRedisPort() {
		return redisPort;
	}

	public void setRedisPort(int redisPort) {
		this.redisPort = redisPort;
	}

	public String getRedisPwd() {
		return redisPwd;
	}

	public void setRedisPwd(String redisPwd) {
		this.redisPwd = redisPwd;
	}

	public String getSenderMail() {
		return senderMail;
	}

	public void setSenderMail(String senderMail) {
		this.senderMail = senderMail;
	}

	public String getMailPwd() {
		return mailPwd;
	}

	public void setMailPwd(String mailPwd) {
		this.mailPwd = mailPwd;
	}

	public String getReciverMail() {
		return reciverMail;
	}

	public void setReciverMail(String reciverMail) {
		this.reciverMail = reciverMail;
	}

	public String getMailTitle() {
		return mailTitle;
	}

	public void setMailTitle(String mailTitle) {
		this.mailTitle = mailTitle;
	}

}
