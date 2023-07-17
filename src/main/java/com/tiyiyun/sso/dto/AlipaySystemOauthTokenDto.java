package com.tiyiyun.sso.dto;

import java.util.Date;

import com.tiyiyun.sso.dto.AlipayAuthUserDto.AlipayAuthUserDtoBuilder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlipaySystemOauthTokenDto {

	//授权方式。支持：
	//1.authorization_code，表示换取使用用户授权码code换取授权令牌access_token。
	//2.refresh_token，表示使用refresh_token刷新获取新授权令牌。
	private String grant_type;
	//授权码，用户对应用授权后得到。本参数在 grant_type 为 authorization_code 时必填；
	//为 refresh_token 时不填。
	private String code;
	//刷新令牌，上次换取访问令牌时得到。本参数在 grant_type 为 authorization_code 时不填；
	//为 refresh_token 时必填，且该值来源于此接口的返回值 
	//app_refresh_token（即至少需要通过 grant_type=authorization_code 调用此接口一次才能获取）。
	private String refresh_token;
}
