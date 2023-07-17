package com.tiyiyun.sso.service;

import com.smart.sso.client.entity.UserInfoEntity;
import com.smart.sso.client.rpc.Result;

/**
 * 用户服务接口
 * 
 * @author Joe
 */
public interface UserService {
	
	public Result<UserInfoEntity> findByDevice(String deviceNo, Integer loginType);
}
