package com.dasion.daydayfund.fund;

import java.io.Serializable;
import java.util.List;

public class FundBean implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 基金代码
	 */
	private String fundCode;
	/**
	 * 
	 */
	private String shareType;
	/**
	 * 基金名称
	 */
	private String fundName;
	/**
	 * 
	 */
	private String fundState;
	/**
	 * 基金类型
	 */
	private String fundType;
	/**
	 * 日增长
	 */
	private String dayInc;
	/**
	 * 近一年增长
	 */
	private String yearInc;
	/**
	 * 近一个季度增长
	 */
	private String seasonhInc;
	/**
	 * 近一周增长
	 */
	private String weekInc;
	/**
	 * 单位净值
	 */
	private String netValue;
	/**
	 * 近一个月增长
	 */
	private String monthInc;
	/**
	 * 今年以来增长
	 */
	private String thisyearInc;
	/**
	 * 数据日期
	 */
	private String date;
	/**
	 * 成立以来增长
	 */
	private String setupInc;
	/**
	 * 
	 */
	private String totalNetValue;
	/**
	 * 近半年增长
	 */
	private String halfyearInc;
	/**
	 * 近两年增长
	 */
	private String twoyearInc;
	/**
	 * 近三年增长
	 */
	private String threeyearInc;
	/**
	 * 基金成立日期
	 */
	private String foundedOn;
	/**
	 * 经理
	 */

	private List<String> managers;
	
	/**
	 * 上一年增长
	 */
	private String preYearInc;
	
	/**
	 * 前一年 至 上一年 增长率
	 */
	private String preTwoYearInc;
	
	/**
	 * 累计净值 合计 增长
	 */
	private String sumInc;

	/**
	 * 估值
	 */
	private String appraisement;
	
	
	public String getAppraisement() {
		return appraisement;
	}

	public void setAppraisement(String appraisement) {
		this.appraisement = appraisement;
	}

	public String getSumInc() {
		return sumInc;
	}

	public void setSumInc(String sumInc) {
		this.sumInc = sumInc;
	}

	public String getPreYearInc() {
		return preYearInc;
	}

	public void setPreYearInc(String preYearInc) {
		this.preYearInc = preYearInc;
	}

	public String getPreTwoYearInc() {
		return preTwoYearInc;
	}

	public void setPreTwoYearInc(String preTwoYearInc) {
		this.preTwoYearInc = preTwoYearInc;
	}

	public String getFundCode() {
		return fundCode;
	}

	public void setFundCode(String fundCode) {
		this.fundCode = fundCode;
	}

	public String getShareType() {
		return shareType;
	}

	public void setShareType(String shareType) {
		this.shareType = shareType;
	}

	public String getFundName() {
		return fundName;
	}

	public void setFundName(String fundName) {
		this.fundName = fundName;
	}

	public String getFundState() {
		return fundState;
	}

	public void setFundState(String fundState) {
		this.fundState = fundState;
	}

	public String getFundType() {
		return fundType;
	}

	public void setFundType(String fundType) {
		this.fundType = fundType;
	}

	public String getDayInc() {
		return dayInc;
	}

	public void setDayInc(String dayInc) {
		this.dayInc = dayInc;
	}

	public String getYearInc() {
		return yearInc;
	}

	public void setYearInc(String yearInc) {
		this.yearInc = yearInc;
	}

	public String getSeasonhInc() {
		return seasonhInc;
	}

	public void setSeasonhInc(String seasonhInc) {
		this.seasonhInc = seasonhInc;
	}

	public String getWeekInc() {
		return weekInc;
	}

	public void setWeekInc(String weekInc) {
		this.weekInc = weekInc;
	}

	public String getNetValue() {
		return netValue;
	}

	public void setNetValue(String netValue) {
		this.netValue = netValue;
	}

	public String getMonthInc() {
		return monthInc;
	}

	public void setMonthInc(String monthInc) {
		this.monthInc = monthInc;
	}

	public String getThisyearInc() {
		return thisyearInc;
	}

	public void setThisyearInc(String thisyearInc) {
		this.thisyearInc = thisyearInc;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getSetupInc() {
		return setupInc;
	}

	public void setSetupInc(String setupInc) {
		this.setupInc = setupInc;
	}

	public String getTotalNetValue() {
		return totalNetValue;
	}

	public void setTotalNetValue(String totalNetValue) {
		this.totalNetValue = totalNetValue;
	}

	public String getHalfyearInc() {
		return halfyearInc;
	}

	public void setHalfyearInc(String halfyearInc) {
		this.halfyearInc = halfyearInc;
	}

	public String getTwoyearInc() {
		return twoyearInc;
	}

	public void setTwoyearInc(String twoyearInc) {
		this.twoyearInc = twoyearInc;
	}

	public String getThreeyearInc() {
		return threeyearInc;
	}

	public void setThreeyearInc(String threeyearInc) {
		this.threeyearInc = threeyearInc;
	}

	public String getFoundedOn() {
		return foundedOn;
	}

	public void setFoundedOn(String foundedOn) {
		this.foundedOn = foundedOn;
	}

	public List<String> getManagers() {
		return managers;
	}

	public void setManagers(List<String> managers) {
		this.managers = managers;
	}

	public String toMailString() {
		return "基金代码:" + fundCode 
				+ fundName
				+" 估值: " + appraisement
				+ ", 近一周增长=" + weekInc
				+ ", 日增长=" + dayInc
				+ ", 基金类型=" + fundType 
				+ ", 经理=" + managers;
	}
	
	public String toString() {
		while(fundName.length() < 15){
			fundName = fundName + "格";
		}
		return "FundBean [基金代码=" + fundCode 
				+ fundName
				+"估值: " + appraisement
				+ ", 上一年增长(累计净值)=" + preYearInc
				+ ", 前年增长(累计净值)=" + preTwoYearInc
				+ ", 合计(累计净值)=" + sumInc
				+ ", 近一周增长=" + weekInc
				+ ", 日增长=" + dayInc
				+ ", 近一个月增长=" + monthInc
				+ ", 近一个季度增长=" + seasonhInc 
				+ ", 近半年增长=" + halfyearInc 
				+ ", 今年增长=" + thisyearInc 
				+ ", 近一年增长=" + yearInc 
				+ ", 近两年增长=" + twoyearInc 
				+ ", 近三年增长=" + threeyearInc
				+ ", 数据发布日期=" + date 
				+ ", 成立以来增长=" + setupInc 
				+ ", totalNetValue=" + totalNetValue 
				+ ", 基金成立日期=" + foundedOn 
				+ ", 基金类型=" + fundType 
				+ ", 经理=" + managers + "]";
	}
	
	public String toDayString() {
		return "FundBean [基金代码=" + fundCode 
				+ ", 日增长=" + dayInc
				+ ", 近一周增长=" + weekInc
				+ ", 近一个月增长=" + monthInc
				+ ", 近一个季度增长=" + seasonhInc 
				+ ", 近半年增长=" + halfyearInc 
				+ ", 今年增长=" + thisyearInc 
				+ ", 近一年增长=" + yearInc 
				+ ", 近两年增长=" + twoyearInc 
				+ ", 近三年增长=" + threeyearInc
				+ ", 基金名称=" + fundName
				+ ", 上一年增长(累计净值)=" + preYearInc
				+ ", 前年增长(累计净值)=" + preTwoYearInc
				+ ", 合计(累计净值)=" + sumInc
				+ ", 数据发布日期=" + date 
				+ ", 成立以来增长=" + setupInc 
				+ ", totalNetValue=" + totalNetValue 
				+ ", 基金成立日期=" + foundedOn 
				+ ", 基金类型=" + fundType 
				+ ", 经理=" + managers + "]";
	}
	
	public String toWeekString() {
		return "FundBean [基金代码=" + fundCode 
				+ ", 近一周增长=" + weekInc
				+ ", 日增长=" + dayInc
				+ ", 近一个月增长=" + monthInc
				+ ", 近一个季度增长=" + seasonhInc 
				+ ", 近半年增长=" + halfyearInc 
				+ ", 今年增长=" + thisyearInc 
				+ ", 近一年增长=" + yearInc 
				+ ", 近两年增长=" + twoyearInc 
				+ ", 近三年增长=" + threeyearInc
				+ ", 基金名称=" + fundName
				+ ", 上一年增长(累计净值)=" + preYearInc
				+ ", 前年增长(累计净值)=" + preTwoYearInc
				+ ", 合计(累计净值)=" + sumInc
				+ ", 数据发布日期=" + date 
				+ ", 成立以来增长=" + setupInc 
				+ ", totalNetValue=" + totalNetValue 
				+ ", 基金成立日期=" + foundedOn 
				+ ", 基金类型=" + fundType 
				+ ", 经理=" + managers + "]";
	}
	
	public String toMonthString() {
		return "FundBean [基金代码=" + fundCode 
				+ ", 近一个月增长=" + monthInc
				+ ", 日增长=" + dayInc
				+ ", 近一周增长=" + weekInc
				+ ", 近一个季度增长=" + seasonhInc 
				+ ", 近半年增长=" + halfyearInc 
				+ ", 今年增长=" + thisyearInc 
				+ ", 近一年增长=" + yearInc 
				+ ", 近两年增长=" + twoyearInc 
				+ ", 近三年增长=" + threeyearInc
				+ ", 基金名称=" + fundName
				+ ", 上一年增长(累计净值)=" + preYearInc
				+ ", 前年增长(累计净值)=" + preTwoYearInc
				+ ", 合计(累计净值)=" + sumInc
				+ ", 数据发布日期=" + date 
				+ ", 成立以来增长=" + setupInc 
				+ ", totalNetValue=" + totalNetValue 
				+ ", 基金成立日期=" + foundedOn 
				+ ", 基金类型=" + fundType 
				+ ", 经理=" + managers + "]";
	}
	
	public String toSeasonString() {
		return "FundBean [基金代码=" + fundCode 
				+ ", 近一个季度增长=" + seasonhInc 
				+ ", 日增长=" + dayInc
				+ ", 近一周增长=" + weekInc
				+ ", 近一个月增长=" + monthInc
				+ ", 近半年增长=" + halfyearInc 
				+ ", 今年增长=" + thisyearInc 
				+ ", 近一年增长=" + yearInc 
				+ ", 近两年增长=" + twoyearInc 
				+ ", 近三年增长=" + threeyearInc
				+ ", 基金名称=" + fundName
				+ ", 上一年增长(累计净值)=" + preYearInc
				+ ", 前年增长(累计净值)=" + preTwoYearInc
				+ ", 合计(累计净值)=" + sumInc
				+ ", 数据发布日期=" + date 
				+ ", 成立以来增长=" + setupInc 
				+ ", totalNetValue=" + totalNetValue 
				+ ", 基金成立日期=" + foundedOn 
				+ ", 基金类型=" + fundType 
				+ ", 经理=" + managers + "]";
	}

}
