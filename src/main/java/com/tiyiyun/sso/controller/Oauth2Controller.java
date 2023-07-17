package com.tiyiyun.sso.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smart.sso.client.constant.Oauth2Constant;
import com.smart.sso.client.entity.UserInfoEntity;
import com.smart.sso.client.rpc.Result;
import com.smart.sso.client.rpc.RpcAccessToken;
import com.tiyiyun.sso.common.AccessTokenContent;
import com.tiyiyun.sso.common.CodeContent;
import com.tiyiyun.sso.common.RefreshTokenContent;
import com.tiyiyun.sso.constant.LoginType;
import com.tiyiyun.sso.exception.DomainOperationException;
import com.tiyiyun.sso.service.AlipayService;
import com.tiyiyun.sso.service.AppService;
import com.tiyiyun.sso.service.UserInfoService;
import com.tiyiyun.sso.service.UserService;
import com.tiyiyun.sso.service.WxService;
import com.tiyiyun.sso.session.AccessTokenManager;
import com.tiyiyun.sso.session.CodeManager;
import com.tiyiyun.sso.session.RefreshTokenManager;
import com.tiyiyun.sso.session.SessionManager;
import com.tiyiyun.sso.session.TicketGrantingTicketManager;

import lombok.extern.slf4j.Slf4j;

/**
 * Oauth2服务管理
 * 
 * @author Joe
 */
@Slf4j
@SuppressWarnings("rawtypes")
@RestController
@RequestMapping("/oauth2")
public class Oauth2Controller {
	
	@Autowired
	private AppService appService;
	@Autowired
	private UserService userService;
	@Autowired
	private UserInfoService userInfoService;

	@Autowired
	private CodeManager codeManager;
	@Autowired
	private AccessTokenManager accessTokenManager;
	@Autowired
	private RefreshTokenManager refreshTokenManager;
	@Autowired
	private TicketGrantingTicketManager ticketGrantingTicketManager;
	@Autowired
	private WxService wxService;
	@Autowired
	private AlipayService alipayService;
	@Autowired
	private SessionManager sessionManager;
	
	/**
	 * 获取accessToken
	 * 
	 * @param appId
	 * @param appSecret
	 * @param code
	 * @return
	 * @throws Exception 
	 */
	@RequestMapping(value = "/access_token", method = RequestMethod.GET)
	public Result getAccessToken(
			@RequestParam(value = Oauth2Constant.LOGIN_TYPE, required = true) String loginType,
			@RequestParam(value = Oauth2Constant.APP_ID, required = true) String appId,
			@RequestParam(value = Oauth2Constant.APP_SECRET, required = true) String appSecret,
			@RequestParam(value = Oauth2Constant.DEVICENO, required = false) String deviceNo,
			@RequestParam(value = Oauth2Constant.CODE, required = false) String code,
			@RequestParam(value = Oauth2Constant.SMSCODE, required = false) String smsCode,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		log.info("[access_token]-[登录方式：{}]-[手机号：{}]-[CODE:{}]-[smsCode:{}]", loginType,deviceNo,code, smsCode);
		try {
			// 校验基本参数
			Result<Void> result = validateParam(Integer.parseInt(loginType), code, smsCode);
			if (!result.isSuccess()) {
				return result;
			}

			// 校验应用
			Result<Void> appResult = appService.validate(appId, appSecret);
			if (!appResult.isSuccess()) {
				return appResult;
			}

			// 校验授权
			Result<AccessTokenContent> accessTokenResult = validateAuth(Integer.parseInt(loginType), code, deviceNo, appId, smsCode, request, response);
			if (!accessTokenResult.isSuccess()) {
				return accessTokenResult;
			}
			// 生成RpcAccessToken返回
			return Result.createSuccess(genereateRpcAccessToken(accessTokenResult.getData(), null));
		}catch (DomainOperationException e) {
			return Result.createError(e.getMessage());
		}catch (Exception e) {
			throw e;
		}
	}
	
	private Result<Void> validateParam(Integer grantType, String code, String smsCode) {
		if (grantType == null) {
			return Result.createError("grantType不能为空");
		}
		
		if (grantType == 2 && StringUtils.isEmpty(code)) {
			return Result.createError("第三方授权码不能为空");
		}
		
		if (grantType == 1 && StringUtils.isEmpty(smsCode)) {
			return Result.createError("短信验证码不能为空");
		}
		return Result.success();
	}
	
	private Result<AccessTokenContent> validateAuth(Integer loginType, String code, String deviceNo,String appId, String smsCode,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		UserInfoEntity userInfoEntity = new UserInfoEntity();
		if(LoginType.DEVICENO == loginType) {
			// 1.手机号登录
			userInfoEntity = userInfoService.deviceNoLogin(deviceNo, smsCode);
		}else if(LoginType.WECHAT == loginType) {
			// 2.微信登录
			userInfoEntity = wxService.wechatLogin(code,deviceNo, smsCode);
		}else {
			// 3.支付宝登录
			userInfoEntity = alipayService.alipayLogin(code);
		}
		
		String tgt = ticketGrantingTicketManager.generate(userInfoEntity);
		//String tgt = sessionManager.setUserInfoEntity(userInfoEntity, request, response);
		CodeContent codeContent = new CodeContent(tgt, false, null);
		AccessTokenContent authDto = new AccessTokenContent(codeContent, userInfoEntity, appId);
		return Result.createSuccess(authDto);
	}
	
	/**
	 * 刷新accessToken，并延长TGT超时时间
	 * 
	 * accessToken刷新结果有两种：
	 * 1. 若accessToken已超时，那么进行refreshToken会生成一个新的accessToken，新的超时时间；
	 * 2. 若accessToken未超时，那么进行refreshToken不会改变accessToken，但超时时间会刷新，相当于续期accessToken。
	 * 
	 * @param appId
	 * @param refreshToken
	 * @return
	 */
	@RequestMapping(value = "/refresh_token", method = RequestMethod.GET)
	public Result refreshToken(
			@RequestParam(value = Oauth2Constant.APP_ID, required = true) String appId,
			@RequestParam(value = Oauth2Constant.REFRESH_TOKEN, required = true) String refreshToken) {
		log.info("[refresh_token]-[token:{}]", refreshToken);
		if(!appService.exists(appId)) {
			return Result.createError("非法应用");
		}
		
		RefreshTokenContent refreshTokenContent = refreshTokenManager.validate(refreshToken);
		if (refreshTokenContent == null) {
			return Result.createError("refreshToken有误或已过期");
		}else {
			accessTokenManager.detele(refreshTokenContent.getAccessToken());
		}
		AccessTokenContent accessTokenContent = refreshTokenContent.getAccessTokenContent();
		if (!appId.equals(accessTokenContent.getAppId())) {
			return Result.createError("非法应用");
		}
		UserInfoEntity userInfo = userInfoService.findById(accessTokenContent.getUserInfoEntity().getId());
		if (userInfo == null) {
			log.warn("用户已注销需要重新注册登录！");
			return Result.createError("用户已注销！");
		}else {
			accessTokenContent.setUserInfoEntity(userInfo);// 防止用户更新，没有更新缓存中的用户信息
		}

		return Result.createSuccess(genereateRpcAccessToken(accessTokenContent, refreshTokenContent.getAccessToken()));
	}
	
	private RpcAccessToken genereateRpcAccessToken(AccessTokenContent accessTokenContent, String accessToken) {
		String newAccessToken = accessToken;
		if (newAccessToken == null || !accessTokenManager.refresh(newAccessToken)) {
			newAccessToken = accessTokenManager.generate(accessTokenContent);
		}

		String refreshToken = refreshTokenManager.generate(accessTokenContent, newAccessToken);

		UserInfoEntity userInfo = new UserInfoEntity();
		BeanUtils.copyProperties(accessTokenContent.getUserInfoEntity(), userInfo);
		
		return new RpcAccessToken(newAccessToken, accessTokenManager.getExpiresIn(), refreshToken,
				userInfo);
	}
}