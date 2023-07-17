package com.tiyiyun.sso.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smart.sso.client.rpc.Result;
import com.tiyiyun.sso.entity.AppEntity;
import com.tiyiyun.sso.repository.AppMapper;
import com.tiyiyun.sso.service.AppService;

@Service("appService")
public class AppServiceImpl implements AppService {

	@Autowired
	private AppMapper appMapper;


	@Override
	public boolean exists(String appId) {
		AppEntity appInfo = appMapper.selectByPrimaryKey(appId);
		return appInfo!=null;
	}

	@Override
	public Result<Void> validate(String appId, String appSecret) {
		AppEntity appInfo = appMapper.selectByAppIdAndAppSec(appId, appSecret);
		if(appInfo != null ) {
			return Result.success();
		}else {
			return Result.createError("appid或appSecret有误");
		}
	}
}
