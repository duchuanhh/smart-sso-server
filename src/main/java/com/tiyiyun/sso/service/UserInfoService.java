package com.tiyiyun.sso.service;

import java.util.Date;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.smart.sso.client.entity.UserInfoEntity;
import com.smart.sso.client.util.SessionUtils;
import com.tiyiyun.sso.exception.DomainOperationException;
import com.tiyiyun.sso.repository.UserInfoMapper;
import com.tiyiyun.sso.util.Des3;
import com.tiyiyun.sso.util.StringRedisTemplateService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserInfoService {

	@Value("${defaultavatar}")
	private String defaultavatar;
	
	@Autowired
	private UserInfoMapper userInfoMapper;
	
	public UserInfoEntity findBySubId(Long id) {
		return userInfoMapper.findBySubId(id);
	}
	
	public UserInfoEntity findBydeviceNo(String deviceNo) {
		return userInfoMapper.findByDeviceNo(deviceNo);
	}
	
	public int updateUserInfo(UserInfoEntity record) {
		return userInfoMapper.updateByPrimaryKeySelective(record);
	}
	
	public UserInfoEntity findById(Long id) {
		return userInfoMapper.selectByPrimaryKey(id);
	}
	
	public UserInfoEntity deviceNoLogin(String deviceNo, String smsCode) throws Exception{
		String key = "fixture-SMS-CODE:"+deviceNo;
		String value = StringRedisTemplateService.get(key);
		if(StringUtils.isBlank(value) || !smsCode.equals(value)) {
			throw new DomainOperationException("验证码有误或已过期！");
		}
		UserInfoEntity userInfo = userInfoMapper.findByDeviceNo(deviceNo);
		if(userInfo != null) {
			if(userInfo.getIsDelete() == 1) {
				userInfoMapper.updateByPrimaryKeySelective(UserInfoEntity.builder().id(userInfo.getId()).isDelete(0).build());
			}
			if(userInfo.getIsDelete() == 2) {
				throw new DomainOperationException("您已被拉黑！");
			}
			return userInfo;
		}else {
			Long id = addUserInfo(1, null, getStringRandom(11), deviceNo, defaultavatar);
			return findById(id);
		}
	}
	
	// 随机生成用户名
	public static String getStringRandom(int length) {
        String val = "";
        Random random = new Random();
 
        //参数length，表示生成几位随机数
        for (int i = 0; i < length; i++) {
 
            String charOrNum = random.nextInt(2) % 2 == 0 ? "char" : "num";
            //输出字母还是数字
            if ("char".equalsIgnoreCase(charOrNum)) {
                //输出是大写字母还是小写字母
                int temp = random.nextInt(2) % 2 == 0 ? 65 : 97;
                val += (char) (random.nextInt(26) + temp);
            } else if ("num".equalsIgnoreCase(charOrNum)) {
                val += String.valueOf(random.nextInt(10));
            }
        }
        return val;
    }
	
	public Long addUserInfo(Integer type, Long subId, String userName, String deviceNo, String userProfile) throws Exception {
		UserInfoEntity entity = new UserInfoEntity();
		entity.setUserName(userName);
		String des3EncodeCBC = Des3.des3EncodeCBC("test@telefen.com~!@#$250", "88888888", RandomStringUtils.randomAlphanumeric(6));
		entity.setPassWord(des3EncodeCBC);
		entity.setType(type);
		entity.setSubId(subId);
		entity.setDeviceNo(deviceNo);
		entity.setUserProfile(userProfile);
		entity.setLevelId(1);
		entity.setCreateTime(new Date());
		entity.setUpdateTime(new Date());
		userInfoMapper.insertSelective(entity);
		return entity.getId();
    }
	
	/**
	 * Explain：获取用户信息，目前没有用户，属于测试代码
	 */
	public UserInfoEntity queryUserInfo(HttpServletRequest req) {
		UserInfoEntity user = SessionUtils.getUser(req);
		if(user != null) {
			return user;
		}else {
			log.info("目前是测试用户！！！！！！！！！！");
			return UserInfoEntity.builder().id(1L).build();
		}
	}
}
