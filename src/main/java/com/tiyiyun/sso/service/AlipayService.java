package com.tiyiyun.sso.service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import com.smart.sso.client.entity.UserInfoEntity;
import com.tiyiyun.sso.config.AliPayProperty;
import com.tiyiyun.sso.constant.LoginType;
import com.tiyiyun.sso.entity.AlipayUserInfoEntity;
import com.tiyiyun.sso.repository.AlipayUserInfoMapper;
import com.tiyiyun.sso.util.StringRedisTemplateService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AlipayService {
	
	public static final String ACCESSTOKEN_KEY = "WeChat_ACCESSTOKEN_KEY";
	public static final String REFRESHTOKEN_KEY = "WeChat_REFRESHTOKEN_KEY";
	
	@Autowired
	private AliPayProperty aliPayProperty;
	@Autowired
	private AlipayUserInfoMapper aliPayUserInfoMapper;
    @Autowired
    private UserInfoService userInfoService;
	

	// 支付宝登录接口---唤起登录后调用"
	public UserInfoEntity alipayLogin(String authCode) throws Exception {
		AlipayUserInfoEntity alipayUserInfo = null;
		
		// 1. 利用 授权码 authCode 获取 access_token、user_id
		AlipaySystemOauthTokenResponse response = getAccessToken(authCode);
		if (response.isSuccess()) {
			/**
			 * 获取到用户信息后保存到数据库 1. 如果数据库不存在, 则注册 2. 如果存在，则获取数据库中的信息再返回
			 */
			String accessToken = response.getAccessToken();

			// 2. ★★★这里是从数据库查询该用户是否存在的操作( 假设不存在---> null )
			alipayUserInfo = aliPayUserInfoMapper.findByOpenId(alipayUserInfo.getOpenId());
			if (alipayUserInfo == null) {
				// 如果用户不存在，则通过支付宝api获取用户的信息后，再注册用户到自己平台数据库
				AlipayUserInfoShareResponse aliUserInfo = getAliUserInfo(accessToken);
				if (aliUserInfo != null) {
					alipayUserInfo = new AlipayUserInfoEntity();
					alipayUserInfo.setNickName(aliUserInfo.getNickName());
					alipayUserInfo.setOpenId(response.getOpenId());
					alipayUserInfo.setUnionid(response.getUnionId());
					alipayUserInfo.setUserId(response.getUserId());
					alipayUserInfo.setGender(aliUserInfo.getGender());
					alipayUserInfo.setProvince(aliUserInfo.getProvince());
					alipayUserInfo.setCity(aliUserInfo.getCity());
					alipayUserInfo.setAvatar(aliUserInfo.getAvatar());
					alipayUserInfo.setCreateTime(new Date());
					alipayUserInfo.setUpdateTime(new Date());
					// 记录支付宝用户信息
					aliPayUserInfoMapper.insertSelective(alipayUserInfo);
					// 添加平台用户
		    		Long id = userInfoService.addUserInfo(LoginType.AlIPAY, alipayUserInfo.getId(), aliUserInfo.getNickName(), null, null);
		    		return userInfoService.findById(id);
				}
			}else {
				return userInfoService.findBySubId(alipayUserInfo.getId());
			}
		}
		return null;
	}

	
	/**
	  * Explain：获取AccessToken
	 */
   public AlipaySystemOauthTokenResponse getAccessToken(String code) throws Exception {
   	// 1.查询缓存数据
       String accessToken = StringRedisTemplateService.get(ACCESSTOKEN_KEY+"-"+code);
       if (StringUtils.isNotBlank(accessToken)) {
           log.info("缓存中获取支付宝accesstoken~!");
           return JSONObject.parseObject(accessToken, AlipaySystemOauthTokenResponse.class);
       }
       
       // 2.缓存不存在并且创建过token自动刷新
       String refreshToken = StringRedisTemplateService.get(REFRESHTOKEN_KEY+"-"+code);
       if(StringUtils.isNotBlank(refreshToken)) {
       	log.info("支付宝refreshToken 已刷新成功！");
       	return callAliForAccessToken(code, refreshToken);
       }
       return callAliForAccessToken(code, null);
   }
	
	/**
	 * Explain：用授权码获取 access_token, user_id 注：refreshToken 不为空时为延长access_token时效
	 * 官方文档: https://opendocs.alipay.com/open/02ailc
	 */
	private AlipaySystemOauthTokenResponse callAliForAccessToken(String autoCode, String refreshToken) throws AlipayApiException {
		AlipayClient alipayClient = aliPayProperty.getAlipayClient();

		AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
		request.setGrantType(StringUtils.isNotBlank(refreshToken) ? "refresh_token " : "authorization_code");// 请求参数 grant_type
		request.setCode(autoCode);// 请求参数,授权码 code
		if(StringUtils.isNotBlank(refreshToken)) {
			request.setRefreshToken(refreshToken);
		}
		
		log.info("调用-[支付宝获取access_token]-入参：{}", JSONObject.toJSON(request));
		AlipaySystemOauthTokenResponse response = alipayClient.execute(request);
		log.info("调用-[支付宝获取access_token]-返回：{}", JSONObject.toJSON(request));
		if (!response.isSuccess()) {
			log.error("调用-支付宝获取access_token异常：{}", response.getMsg());
			throw new AlipayApiException(response.getMsg());
		}
        StringRedisTemplateService.setBasedTime(ACCESSTOKEN_KEY+"-"+autoCode, JSON.toJSONString(response), 1 * 60 * 60, TimeUnit.SECONDS);
        StringRedisTemplateService.setBasedTime(REFRESHTOKEN_KEY+"-"+autoCode, response.getRefreshToken(), 1 * 60 * 60, TimeUnit.SECONDS);
		return response;
	}

	/**
	 * 获取支付宝用户信息 官方文档: https://opendocs.alipay.com/open/02aild
	 */
	private AlipayUserInfoShareResponse getAliUserInfo(String accessToken) throws Exception {
		AlipayClient alipayClient = aliPayProperty.getAlipayClient();
		AlipayUserInfoShareRequest request = new AlipayUserInfoShareRequest();
		log.info("调用-[支付宝用户信息]-入参：{}，accessToken:{}", JSONObject.toJSON(request), accessToken);
		AlipayUserInfoShareResponse response = alipayClient.execute(request, accessToken);
		log.info("调用-[支付宝用户信息]-返回：{}", JSONObject.toJSON(response));
		if (!response.isSuccess()) {
			log.error("调用-支付宝获取access_token异常：{}", response.getMsg());
			throw new Exception(response.getMsg());
		} 
		return response;
	}
}
