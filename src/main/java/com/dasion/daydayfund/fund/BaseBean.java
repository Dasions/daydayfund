package com.dasion.daydayfund.fund;

import java.io.Serializable;

public class BaseBean implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2587750921612759486L;
	private String crawlerType;
	private String excStep;
	private FundBean fundBean;
	private String requestUrl;
	private String fromQueue;
	private String toQueue;
	
	
	public BaseBean() {
		super();
	}

	public BaseBean(String crawlerType, String excStep, String requestUrl, String fromQueue, String toQueue) {
		super();
		this.crawlerType = crawlerType;
		this.excStep = excStep;
		this.requestUrl = requestUrl;
		this.fromQueue = fromQueue;
		this.toQueue = toQueue;
	}
	
	public String getFromQueue() {
		return fromQueue;
	}
	public void setFromQueue(String fromQueue) {
		this.fromQueue = fromQueue;
	}
	public String getToQueue() {
		return toQueue;
	}
	public void setToQueue(String toQueue) {
		this.toQueue = toQueue;
	}
	public String getRequestUrl() {
		return requestUrl;
	}
	public void setRequestUrl(String requestUrl) {
		this.requestUrl = requestUrl;
	}
	public String getCrawlerType() {
		return crawlerType;
	}
	public void setCrawlerType(String crawlerType) {
		this.crawlerType = crawlerType;
	}
	public String getExcStep() {
		return excStep;
	}
	public void setExcStep(String excStep) {
		this.excStep = excStep;
	}
	public FundBean getFundBean() {
		return fundBean;
	}
	public void setFundBean(FundBean fundBean) {
		this.fundBean = fundBean;
	}
	
	
}
