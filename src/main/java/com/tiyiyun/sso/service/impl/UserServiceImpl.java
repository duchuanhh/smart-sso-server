package com.tiyiyun.sso.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smart.sso.client.entity.UserInfoEntity;
import com.smart.sso.client.rpc.Result;
import com.tiyiyun.sso.repository.UserInfoMapper;
import com.tiyiyun.sso.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("userService")
public class UserServiceImpl  implements UserService{
	
	
	@Autowired
	private UserInfoMapper userInfoMapper;
	
	public Result<UserInfoEntity> findByDevice(String deviceNo, Integer loginType) {
		UserInfoEntity userInfo = userInfoMapper.selectByUserNameAndPass(deviceNo);
		if (userInfo != null) {
			return Result.createSuccess(userInfo);
		}else {
			return Result.createError("账号或密码不正确");
		}
		
	}
	
}
