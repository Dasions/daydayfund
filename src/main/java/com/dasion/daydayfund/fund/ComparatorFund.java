package com.dasion.daydayfund.fund;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;
/**
 * 排序算法
 * @author Dasion-PC
 *
 */
public class ComparatorFund implements Comparator<FundBean> {

	private final static int SMALL = -1;
	private final static int BIG = 1;
	private final static int EQ = 0;
	private final static String ZONE_STRING = "0";
	private final static String DECIMALFORMAT_STRING = "####.#######";
	private int sortType;

	public ComparatorFund(int sortType) {
		this.sortType = sortType;
	}

	/**
	 * 按 三年 ，两年， 近一年 降序
	 */
	@Override
	public int compare(FundBean fundOne, FundBean fundTwo) {

		try {
			if (this.sortType == SortTypeEnum.THREEYEARINC_DESC.getCode()) {
				return sortByThreeyearInc(fundOne.getThreeyearInc(), fundTwo.getThreeyearInc());
			}
			
			if (this.sortType == SortTypeEnum.TWOYEARINC_DESC.getCode()) {
				return sortByThreeyearInc(fundOne.getTwoyearInc(), fundTwo.getTwoyearInc());
			}
			
			if (this.sortType == SortTypeEnum.YEARINC_DESC.getCode()) {
				return sortByThreeyearInc(fundOne.getYearInc(), fundTwo.getYearInc());
			}
			
			if (this.sortType == SortTypeEnum.THIS_YEARINC_DESC.getCode()) {
				return sortByThreeyearInc(fundOne.getThisyearInc(), fundTwo.getThisyearInc());
			}
			
			if (this.sortType == SortTypeEnum.HALF_YEARINC_DESC.getCode()) {
				return sortByThreeyearInc(fundOne.getHalfyearInc(), fundTwo.getHalfyearInc());
			}
			
			if (this.sortType == SortTypeEnum.THIS_SEASON_DESC.getCode()) {
				return sortByThreeyearInc(fundOne.getSeasonhInc(), fundTwo.getSeasonhInc());
			}
			
			if (this.sortType == SortTypeEnum.THIS_MONTH_DESC.getCode()) {
				return sortByThreeyearInc(fundOne.getMonthInc(), fundTwo.getMonthInc());
			}
			
			if (this.sortType == SortTypeEnum.THIS_WEEK_DESC.getCode()) {
				return sortByThreeyearInc(fundOne.getWeekInc(), fundTwo.getWeekInc());
			}
			
			if (this.sortType == SortTypeEnum.THIS_DAY_DESC.getCode()) {
				return sortByThreeyearInc(fundOne.getDayInc(), fundTwo.getDayInc());
			}
			
			if (this.sortType == SortTypeEnum.PRETWOYEAR_DESC.getCode()) {
				return sortByThreeyearInc(fundOne.getPreTwoYearInc(), fundTwo.getPreTwoYearInc());
			}
			
			if (this.sortType == SortTypeEnum.PREYEAR_DESC.getCode()) {
				return sortByThreeyearInc(fundOne.getPreYearInc(), fundTwo.getPreYearInc());
			}
			
			if (this.sortType == SortTypeEnum.SUMINC_DESC.getCode()) {
				return sortByThreeyearInc(fundOne.getSumInc(), fundTwo.getSumInc());
			}
			
			if (this.sortType == SortTypeEnum.appraisement_DESC.getCode()) {
				return sortByThreeyearInc(fundOne.getAppraisement(), fundTwo.getAppraisement());
			}
			
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return EQ;
	}

	private int sortByThreeyearInc(String fundOneInc, String fundTwoInc) throws ParseException {

		if (StringUtils.isEmpty(fundOneInc) && StringUtils.isEmpty(fundTwoInc)) {
			return EQ;
		}
		if (StringUtils.isEmpty(fundOneInc)) {
			fundOneInc = ZONE_STRING;
		}

		if (StringUtils.isEmpty(fundTwoInc)) {
			fundTwoInc = ZONE_STRING;
		}

		DecimalFormat df = new DecimalFormat(DECIMALFORMAT_STRING);
		Number fundOneIncValue = null;
		fundOneIncValue = df.parse(fundOneInc);
		Number fundTwoIncValue = null;
		fundTwoIncValue = df.parse(fundTwoInc);
		if (fundOneIncValue.doubleValue() > fundTwoIncValue.doubleValue()) {
			return SMALL;
		}

		if (fundOneIncValue.doubleValue() < fundTwoIncValue.doubleValue()) {
			return BIG;
		}

		return EQ;
	}



}
