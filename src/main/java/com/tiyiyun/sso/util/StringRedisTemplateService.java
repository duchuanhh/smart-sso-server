package com.tiyiyun.sso.util;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StringRedisTemplateService {

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	private static StringRedisTemplate stringRedis;

	@PostConstruct
	public void init() {
		stringRedis = stringRedisTemplate;
	}

	// 模糊匹配所有以keyword:开头的所有key值
	public static Set<String> keys(String keyword) {
		Set<String> sets = Sets.newHashSet();
		try {
			sets = stringRedis.keys(keyword + "*");
		} catch (Exception e) {
			log.info("redis get exception:{}", e.getMessage());
		}
		return sets;
	}

	public static String get(String key) {
		String value = null;
		try {
			value = stringRedis.opsForValue().get(key);
		} catch (Exception e) {
			log.info("redis get exception:{}", e.getMessage());
		}
		return value;
	}

	public static void set(String key, String value) {
		try {
			stringRedis.opsForValue().set(key, value, 10 * 365, TimeUnit.DAYS);
		} catch (Exception e) {
			log.info("redis set exception:{}", e.getMessage());
		}
	}

	public static void setBasedTime(String key, String value, long timeout, TimeUnit unit) {
		try {
			stringRedis.opsForValue().set(key, value, timeout, unit);
		} catch (Exception e) {
			log.info("redis set exception:{}", e.getMessage());
		}
	}

	public static void leftPushAll(String key, List<String> list, long timeout, TimeUnit unit) {
		try {
			stringRedis.opsForList().leftPushAll(key, list);
			stringRedis.expire(key, timeout, unit);
		} catch (Exception e) {
			log.info("redis leftPushAll exception:{}", e.getMessage());
		}
	}

	public static List<String> range(String key, int start, int end) {
		List<String> list = null;
		try {
			list = stringRedis.opsForList().range(key, start, end);
		} catch (Exception e) {
			log.info("redis range exception:{}", e.getMessage());
		}
		return list;
	}

	public static void delete(Set<String> keys) {
		try {
			stringRedis.delete(keys);
		} catch (Exception e) {
			log.info("redis delete exception:{}", e.getMessage());
		}
	}

	/*
	 * public void hash(Long merchantId, Long userId) { String key =
	 * "commdetail.html"+merchantId; Map<Long, Integer> map = new HashMap<Long,
	 * Integer>(); map.put(userId, 1); if(haskey(key)) {
	 * stringRedis.opsForHash().putAll(key, map); }else {
	 * stringRedis.opsForHash().putAll(key, map); stringRedis.expire("map3", 1000,
	 * TimeUnit.SECONDS); } }
	 * 
	 * public Boolean haskey(String key) { return stringRedis.hasKey(key); }
	 */
}
