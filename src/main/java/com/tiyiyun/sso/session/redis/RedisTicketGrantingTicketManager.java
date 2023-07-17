package com.tiyiyun.sso.session.redis;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.smart.sso.client.entity.UserInfoEntity;
import com.tiyiyun.sso.session.TicketGrantingTicketManager;

/**
 * 分布式登录凭证管理
 * 
 * @author Joe
 */
@Component
@ConditionalOnProperty(name = "sso.session.manager", havingValue = "redis")
public class RedisTicketGrantingTicketManager implements TicketGrantingTicketManager {
	
	@Value("${sso.timeout}")
    private int timeout;
	@Autowired
	private StringRedisTemplate redisTemplate;

	@Override
	public void create(String tgt, UserInfoEntity UserInfoEntity) {
		redisTemplate.opsForValue().set(tgt, JSON.toJSONString(UserInfoEntity), getExpiresIn(),
				TimeUnit.SECONDS);
	}

	@Override
	public UserInfoEntity getAndRefresh(String tgt) {
		String UserInfoEntity = redisTemplate.opsForValue().get(tgt);
		if (StringUtils.isEmpty(UserInfoEntity)) {
			return null;
		}
		redisTemplate.expire(tgt, timeout, TimeUnit.SECONDS);
		return JSONObject.parseObject(UserInfoEntity, UserInfoEntity.class);
	}
	
	@Override
	public void set(String tgt, UserInfoEntity UserInfoEntity) {
		create(tgt, UserInfoEntity);
	}

	@Override
	public void remove(String tgt) {
		redisTemplate.delete(tgt);
	}

	@Override
	public int getExpiresIn() {
		return timeout;
	}
}