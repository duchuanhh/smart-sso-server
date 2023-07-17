package com.tiyiyun.sso.service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.smart.sso.client.entity.UserInfoEntity;
import com.smart.sso.client.entity.WxUserInfoEntity;
import com.tiyiyun.sso.constant.LoginType;
import com.tiyiyun.sso.dto.AccessTokenDto;
import com.tiyiyun.sso.dto.WechatUserInfoDto;
import com.tiyiyun.sso.exception.DomainOperationException;
import com.tiyiyun.sso.repository.WxUserInfoMapper;
import com.tiyiyun.sso.util.StringRedisTemplateService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WxService {
	
	public static final String WeChat_ACCESSTOKEN_KEY = "WeChat_ACCESSTOKEN_KEY";
	public static final String WeChat_REFRESHTOKEN_KEY = "WeChat_REFRESHTOKEN_KEY";
	

    public static final String WEIXIN_URL = "https://api.weixin.qq.com";
    
    @Value("${weixin.appid}")
    private String appid;

    @Value("${weixin.secret}")
    private String secret;  
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private WxUserInfoMapper wxUserInfoMapper;
    
    @Autowired
    private UserInfoService userInfoService;
    
    @Transactional(rollbackFor = Exception.class)
    public UserInfoEntity wechatLogin(String code, String deviceNo, String smsCode) throws Exception {
    	// 1.获取accessToken
    	AccessTokenDto accessToken = getAccessToken(code, deviceNo);
    	// 2.查询微信用户信息
    	WechatUserInfoDto wechatUserInfo = queryWechatUserInfo(accessToken.getAccess_token(), accessToken.getOpenid());
    	// 3.查询是否入库
    	WxUserInfoEntity wxUserInfoEntity = wxUserInfoMapper.findByOpenId(accessToken.getOpenid());
    	if(wxUserInfoEntity != null) {
    		String nickName = wechatUserInfo.getNickname();
    		String headImgurl = wechatUserInfo.getHeadimgurl();
    		// 更新微信用户信息
    		wxUserInfoMapper.updateByPrimaryKeySelective(WxUserInfoEntity.builder().id(wxUserInfoEntity.getId()).nickName(nickName).headImgUrl(headImgurl).build());
    		UserInfoEntity entity = userInfoService.findBySubId(wxUserInfoEntity.getId());
    		//updateUserNameAndHeadImag(wechatUserInfo, entity);// 更新用户头像和昵称
    		return entity;
    	}else {
    		String key = "fixture-SMS-CODE-"+deviceNo;
			String value = StringRedisTemplateService.get(key);
			if(StringUtils.isBlank(value) || !smsCode.equals(value)) {
				throw new DomainOperationException("验证码有误或已过期！");
			}
    		wxUserInfoEntity = WxUserInfoEntity.builder()
    				.nickName(wechatUserInfo.getNickname())
    				.openId(wechatUserInfo.getOpenid())
    				.unionid(wechatUserInfo.getUnionid())
    				.sex(wechatUserInfo.getSex())
    				.country(wechatUserInfo.getCountry())
    				.province(wechatUserInfo.getProvince())
    				.city(wechatUserInfo.getCity())
    				.headImgUrl(wechatUserInfo.getHeadimgurl())
    				.createTime(new Date())
    				.updateTime(new Date())
    				.build();
    		// 记录微信用户信息
    		wxUserInfoMapper.insertSelective(wxUserInfoEntity);
    		// 添加平台用户
    		UserInfoEntity userInfo = userInfoService.findBydeviceNo(deviceNo);
    		if(userInfo != null) {
    			updateUserNameAndHeadImag(wechatUserInfo, userInfo);
    			userInfoService.updateUserInfo(UserInfoEntity.builder().id(userInfo.getId()).subId(wxUserInfoEntity.getId())
    					.updateTime(new Date()).type(2).build());
    			return userInfoService.findById(userInfo.getId());
    		}else {
    			Long id = userInfoService.addUserInfo(LoginType.WECHAT, wxUserInfoEntity.getId(), wechatUserInfo.getNickname(), deviceNo, wechatUserInfo.getHeadimgurl());
        		return userInfoService.findById(id);
    		}
    	}
    }



	private void updateUserNameAndHeadImag(WechatUserInfoDto wechatUserInfo, UserInfoEntity userInfo) {
		UserInfoEntity entity = new UserInfoEntity();
		entity.setId(userInfo.getId());
		entity.setUserName(wechatUserInfo.getNickname());
		entity.setUserProfile(wechatUserInfo.getHeadimgurl());
		userInfoService.updateUserInfo(entity);
	}
    
    
    
    public WechatUserInfoDto queryWechatUserInfo(String accessToken, String openId) throws Exception {
    	String url = WEIXIN_URL + "/sns/userinfo?access_token="+ accessToken+ "&openid="+openId+"&lang=zh_CN";
    	log.info("调用微信接口-[userinfo]-获取用户信息，URL-[{}]", url);
    	HttpHeaders headers = new HttpHeaders();
    	headers.setContentType(MediaType.APPLICATION_JSON);
    	headers.set("Accept-Charset", "UTF-8");
        String res = restTemplate.getForObject(url, String.class);
        log.info("调用微信接口-[userinfo]-获取用户信息反参：{}", res);
        JSONObject result = JSON.parseObject(res);
        Integer errCode = result.getInteger("errcode");
        if (errCode != null) {
            log.info("AccessToken获取用户信息失败：{}", errCode);
            throw new Exception("AccessToken获取用户信息失败");
        }
        
        return JSONObject.toJavaObject(result, WechatUserInfoDto.class);
    }
    
	/**
	  * Explain：获取AccessToken
	 */
    public AccessTokenDto getAccessToken(String code, String deviceNo) throws Exception {
    	// 1.查询缓存数据
        String accessToken = StringRedisTemplateService.get(WeChat_ACCESSTOKEN_KEY+"-"+deviceNo);// 暂时不可用，微信登录的code是变动的，鉴于是登录功能不会频繁调用此接口，暂不优化
        if (StringUtils.isNotBlank(accessToken)) {
            log.info("缓存中获取微信accesstoken~!");
            return JSONObject.parseObject(accessToken, AccessTokenDto.class);
        }
        
        // 2.缓存不存在并且创建过token自动刷新
        String refreshToken = StringRedisTemplateService.get(WeChat_REFRESHTOKEN_KEY+"-"+deviceNo);
        if(StringUtils.isNotBlank(refreshToken)) {
        	log.info("微信refreshToken 已刷新成功！");
        	return refreshToken(refreshToken, code);// 暂时不可用，微信登录的code是变动的，鉴于是登录功能不会频繁调用此接口，暂不优化
        }
        return callWxAccessToken(code, deviceNo);
    }

    public AccessTokenDto callWxAccessToken(String code, String deviceNo) throws Exception {
        String url = WEIXIN_URL + "/sns/oauth2/access_token?appid="+ appid+ "&secret="+secret+"&code="+code+"&grant_type=authorization_code";
        log.info("调用微信接口-[access_token]-获取token，URL-[{}]", url);
        String res = restTemplate.getForObject(url, String.class);
        log.info("调用微信接口-[access_token]-获取token反参：{}", res);
        JSONObject result = JSON.parseObject(res);
        Integer errCode = result.getInteger("errcode");
        if (errCode != null && errCode != 0) {
            log.info("获取微信AccessToken失败：{}", errCode);
            throw new Exception("获取微信AccessToken失败");
        }
        AccessTokenDto accessTokenDto = JSONObject.toJavaObject(result, AccessTokenDto.class);
        if(StringUtils.isNoneBlank(deviceNo)) {
        	 StringRedisTemplateService.setBasedTime(WeChat_ACCESSTOKEN_KEY+"-"+deviceNo, JSON.toJSONString(accessTokenDto), 1 * 60 * 60, TimeUnit.SECONDS);
             StringRedisTemplateService.setBasedTime(WeChat_REFRESHTOKEN_KEY+"-"+deviceNo, accessTokenDto.getRefresh_token(), 30, TimeUnit.DAYS);
        }
        return accessTokenDto;
    }
    
    /**
     * Explain：AccessToken过期刷新
     * @return 
     * @throws Exception 
     */
    public AccessTokenDto refreshToken(String refreshToken, String deviceNo) throws Exception {
        String url = WEIXIN_URL + "/sns/oauth2/refresh_token?appid="+ appid+ "&grant_type=refresh_token"+"&refresh_token="+refreshToken;
        String res = restTemplate.getForObject(url, String.class);
        log.info("refreshToken反参：{}", res);
        JSONObject result = JSON.parseObject(res);
        Integer errCode = result.getInteger("errcode");
        if (errCode != null && errCode != 0) {
            log.info("刷新微信AccessToken失败：{}", errCode);
            throw new Exception("刷新微信AccessToken失败");
        }
        AccessTokenDto accessTokenDto = JSONObject.toJavaObject(result, AccessTokenDto.class);
        if(StringUtils.isNoneBlank(deviceNo)) {
       	 	StringRedisTemplateService.setBasedTime(WeChat_ACCESSTOKEN_KEY+"-"+deviceNo, JSON.toJSONString(accessTokenDto), 1 * 60 * 60, TimeUnit.SECONDS);
            StringRedisTemplateService.setBasedTime(WeChat_REFRESHTOKEN_KEY+"-"+deviceNo, accessTokenDto.getRefresh_token(), 30, TimeUnit.DAYS);
       }
        return accessTokenDto;
    }
}
