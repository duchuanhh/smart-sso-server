package com.tiyiyun.sso.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatTokenModel {
	
	/**
	     *  接口访问凭证 wx֤
	*/
	private String accessToken;
	/**
	     * 接口访问凭证֤，刷新 wx
	*/
	private String refreshToken;
	/**
	     * 凭证有效期单位：second wx
	*/
	private int expiresIn;
	/**
	    * 授权用户唯一标识 wx
	*/
	private String openid;
	/**
	    * 授权用户唯一标识 wx
	*/
	private String unionId;
}
