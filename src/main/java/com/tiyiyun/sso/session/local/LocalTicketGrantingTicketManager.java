package com.tiyiyun.sso.session.local;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.smart.sso.client.entity.UserInfoEntity;
import com.tiyiyun.sso.common.ExpirationPolicy;
import com.tiyiyun.sso.session.TicketGrantingTicketManager;

import lombok.extern.slf4j.Slf4j;

/**
 * 本地登录凭证管理
 * 
 * @author Joe
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sso.session.manager", havingValue = "local")
public class LocalTicketGrantingTicketManager implements TicketGrantingTicketManager, ExpirationPolicy {

	
	@Value("${sso.timeout}")
    private int timeout;

	private Map<String, DummyTgt> tgtMap = new ConcurrentHashMap<>();

	@Override
	public void create(String tgt, UserInfoEntity userInfoEntity) {
		tgtMap.put(tgt, new DummyTgt(userInfoEntity, System.currentTimeMillis() + getExpiresIn() * 1000));
		log.info("登录凭证生成成功, tgt:{}", tgt);
	}

	@Override
	public UserInfoEntity getAndRefresh(String tgt) {
		DummyTgt dummyTgt = tgtMap.get(tgt);
		long currentTime = System.currentTimeMillis();
		if (dummyTgt == null || currentTime > dummyTgt.expired) {
			return null;
		}
		dummyTgt.expired = currentTime + getExpiresIn() * 1000;
		return dummyTgt.UserInfoEntity;
	}
	
	@Override
	public void set(String tgt, UserInfoEntity UserInfoEntity) {
		DummyTgt dummyTgt = tgtMap.get(tgt);
		if (dummyTgt == null) {
			return;
		}
		dummyTgt.UserInfoEntity = UserInfoEntity;
	}

	@Override
	public void remove(String tgt) {
		tgtMap.remove(tgt);
		log.debug("登录凭证删除成功, tgt:{}", tgt);
	}

	@Scheduled(cron = SCHEDULED_CRON)
	@Override
	public void verifyExpired() {
		tgtMap.forEach((tgt, dummyTgt) -> {
			if (System.currentTimeMillis() > dummyTgt.expired) {
				tgtMap.remove(tgt);
				log.debug("登录凭证已失效, tgt:{}", tgt);
			}
		});
	}

	@Override
	public int getExpiresIn() {
		return timeout;
	}
	
	private class DummyTgt {
		private UserInfoEntity UserInfoEntity;
		private long expired;

		public DummyTgt(UserInfoEntity UserInfoEntity, long expired) {
			super();
			this.UserInfoEntity = UserInfoEntity;
			this.expired = expired;
		}
	}
}
