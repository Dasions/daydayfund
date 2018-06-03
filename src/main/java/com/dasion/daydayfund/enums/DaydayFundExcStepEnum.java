package com.dasion.daydayfund.enums;

public enum DaydayFundExcStepEnum {
	BASE_INFO("基础信息", "baseInfo"),
	DETAIL_INFO("详情信息", "detailInfo"),
	INC_INFO("增长率", "incInfo");
	private String name ;
    private String code;
    
    private DaydayFundExcStepEnum( String name , String code ){
        this.name = name ;
        this.code = code ;
    }

	public static DaydayFundExcStepEnum getEnum(String code) {
		for(DaydayFundExcStepEnum daydayFundExcStepEnum : DaydayFundExcStepEnum.values()){
			if(daydayFundExcStepEnum.getCode().equals(code)){
				return daydayFundExcStepEnum;
			}
		}
		return null;

	}
	public String getName() {
		return name;
	}
	

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}
