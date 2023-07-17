package com.tiyiyun.sso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessTokenDto {
	private String access_token;
	private Long expires_in;
	private String refresh_token;
	private String openid;
	private String scope;
	private String unionid;
}
