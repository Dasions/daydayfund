package com.dasion.daydayfund.fund;

public enum SortTypeEnum {
	SUMINC_DESC("累计净值前两年增长", -3),
	PRETWOYEAR_DESC("累计净值前年增长", -2),
	PREYEAR_DESC("累计净值上年增长", -1),
	THREEYEARINC_DESC("近三年增长", 1),
	TWOYEARINC_DESC("近两年增长", 2),
	YEARINC_DESC("近一年增长", 3),
	THIS_YEARINC_DESC("今年", 4),
	THIS_SEASON_DESC("近三个月", 6),
	THIS_MONTH_DESC("近一个月", 7),
	THIS_WEEK_DESC("近一周", 8),
	THIS_DAY_DESC("最近一天", 9),
	HALF_YEARINC_DESC("近半年", 5),
	appraisement_DESC("估值", 10);
	private String name ;
    private int code;
    
    private SortTypeEnum( String name , int code ){
        this.name = name ;
        this.code = code ;
    }

	public String getName() {
		return name;
	}
	
	public static String getName(int code) {
		for(SortTypeEnum sortTypeEnum : SortTypeEnum.values()){
			if(sortTypeEnum.getCode() == code){
				return sortTypeEnum.getName();
			}
		}
		return "";

	}
	

	public void setName(String name) {
		this.name = name;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
    

}
