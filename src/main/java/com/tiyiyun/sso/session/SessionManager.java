package com.tiyiyun.sso.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.smart.sso.client.entity.UserInfoEntity;
import com.smart.sso.client.session.SessionAccessToken;
import com.smart.sso.client.util.SessionUtils;
import com.tiyiyun.sso.constant.AppConstant;
import com.tiyiyun.sso.util.CookieUtils;

/**
 * 服务端凭证管理
 * 
 * @author Joe
 */
@Component
public class SessionManager {
	
	@Autowired
	private AccessTokenManager accessTokenManager;
	@Autowired
	private TicketGrantingTicketManager ticketGrantingTicketManager;

	public String setUserInfoEntity(UserInfoEntity UserInfoEntity, HttpServletRequest request, HttpServletResponse response) {
		String tgt = getCookieTgt(request);
		if (StringUtils.isEmpty(tgt)) {// cookie中没有
			tgt = ticketGrantingTicketManager.generate(UserInfoEntity);
			
			// TGT存cookie，和Cas登录保存cookie中名称一致为：TGC
			CookieUtils.addCookie(AppConstant.TGC, tgt, "/", request, response);
		}
		else if(ticketGrantingTicketManager.getAndRefresh(tgt) == null){
			ticketGrantingTicketManager.create(tgt, UserInfoEntity);
		}
		else {
			ticketGrantingTicketManager.set(tgt, UserInfoEntity);
		}
		return tgt;
	}

	public UserInfoEntity getUserInfoEntity(HttpServletRequest request) {
		String tgt = getCookieTgt(request);
		if (StringUtils.isEmpty(tgt)) {
			return null;
		}
		return ticketGrantingTicketManager.getAndRefresh(tgt);
	}

	public void invalidate(HttpServletRequest request, HttpServletResponse response) {
		String tgt = getCookieTgt(request);
		if (StringUtils.isEmpty(tgt)) {
			return;
		}
		// 删除登录凭证
		ticketGrantingTicketManager.remove(tgt);
		// 删除凭证Cookie
		CookieUtils.removeCookie(AppConstant.TGC, "/", response);
		// 删除所有tgt对应的调用凭证，并通知客户端登出注销本地session
	    accessTokenManager.remove(tgt);
	}

	public String getTgt(HttpServletRequest request) {
		String tgt = getCookieTgt(request);
		if (StringUtils.isEmpty(tgt) || ticketGrantingTicketManager.getAndRefresh(tgt) == null) {
			return null;
		}
		else {
			return tgt;
		}
	}
	
	private String getCookieTgt(HttpServletRequest request) {
		return CookieUtils.getCookie(request, AppConstant.TGC);
	}
}