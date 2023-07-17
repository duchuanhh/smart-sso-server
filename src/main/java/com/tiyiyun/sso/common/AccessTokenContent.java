package com.tiyiyun.sso.common;

import com.smart.sso.client.entity.UserInfoEntity;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class AccessTokenContent {

	private CodeContent codeContent;
	private UserInfoEntity UserInfoEntity;
	private String appId;

	public AccessTokenContent(CodeContent codeContent, UserInfoEntity UserInfoEntity, String appId) {
		this.codeContent = codeContent;
		this.UserInfoEntity = UserInfoEntity;
		this.appId = appId;
	}
}