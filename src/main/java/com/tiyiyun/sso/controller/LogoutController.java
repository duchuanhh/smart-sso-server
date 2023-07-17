package com.tiyiyun.sso.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.smart.sso.client.rpc.Result;
import com.tiyiyun.sso.session.SessionManager;

import lombok.extern.slf4j.Slf4j;

/**
 * 单点登出
 * 
 * @author duchuan
 */
@Slf4j
@RestController
@RequestMapping("/logout")
public class LogoutController {

	@Autowired
	private SessionManager sessionManager;

	/**
	 * 登出
	 * 
	 * @param redirectUri
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public Result<String> logout(HttpServletRequest request, HttpServletResponse response) {
		log.info("用户退出登录！！");
		sessionManager.invalidate(request, response);
        return Result.createSuccess("");
	}
}