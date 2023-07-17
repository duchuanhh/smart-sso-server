package com.tiyiyun.sso.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.sso.client.constant.Oauth2Constant;
import com.smart.sso.client.constant.SsoConstant;
import com.smart.sso.client.entity.UserInfoEntity;
import com.smart.sso.client.rpc.Result;
import com.tiyiyun.sso.constant.AppConstant;
import com.tiyiyun.sso.constant.LoginType;
import com.tiyiyun.sso.service.AppService;
import com.tiyiyun.sso.service.UserService;
import com.tiyiyun.sso.service.WxService;
import com.tiyiyun.sso.session.CodeManager;
import com.tiyiyun.sso.session.SessionManager;

import lombok.extern.slf4j.Slf4j;



/**
 * 单点登录管理
 * 
 * @author Joe
 */
@Slf4j
@Controller
@RequestMapping("/login")
public class LoginController{

	@Autowired
	private CodeManager codeManager;
	@Autowired
	private SessionManager sessionManager;
	@Autowired
	private UserService userService;
	@Autowired
	private AppService appService;
	@Autowired
	private WxService wxService;
	
	/**
	 * 	跳转登录页
	 * 
	 * @param redirectUri
	 * @param appId
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public Result<Object> goLogin(
			@RequestParam(value = SsoConstant.REDIRECT_URI, required = true) String redirectUri,
			@RequestParam(value = Oauth2Constant.APP_ID, required = true) String appId,
			HttpServletRequest request) throws UnsupportedEncodingException {
		String tgt = sessionManager.getTgt(request);
		if (StringUtils.isEmpty(tgt)) {
			return goLoginPath(redirectUri, appId, request);
		}
		return generateCodeAndRedirect(redirectUri, tgt);
	}
	
	/**
	 * 设置request的redirectUri和appId参数，跳转到登录页
	 * 
	 * @param redirectUri
	 * @param request
	 * @return
	 */
	private Result<Object> goLoginPath(String redirectUri, String appId, HttpServletRequest request) {
		request.setAttribute(SsoConstant.REDIRECT_URI, redirectUri);
		request.setAttribute(Oauth2Constant.APP_ID, appId);
		return Result.create(SsoConstant.NO_LOGIN, "未登录或已过期");
	}
	
	/**
	 * 生成授权码，返回给app
	 * 
	 * @param redirectUri
	 * @param tgt
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private Result<Object> generateCodeAndRedirect(String redirectUri, String tgt) throws UnsupportedEncodingException {
		// 生成授权码
		String code = codeManager.generate(tgt, true, redirectUri);
		return Result.createSuccess(authRedirectUri(redirectUri, code));
	}

	/**
	 * 将授权码拼接到回调redirectUri中
	 * 
	 * @param redirectUri
	 * @param code
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private String authRedirectUri(String redirectUri, String code) throws UnsupportedEncodingException {
		StringBuilder sbf = new StringBuilder(redirectUri);
		if (redirectUri.indexOf("?") > -1) {
			sbf.append("&");
		}
		else {
			sbf.append("?");
		}
		sbf.append(Oauth2Constant.AUTH_CODE).append("=").append(code);
		return URLDecoder.decode(sbf.toString(), "utf-8");
	}

}