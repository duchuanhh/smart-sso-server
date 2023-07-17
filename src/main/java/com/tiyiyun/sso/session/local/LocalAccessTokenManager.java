package com.tiyiyun.sso.session.local;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.tiyiyun.sso.common.AccessTokenContent;
import com.tiyiyun.sso.common.CodeContent;
import com.tiyiyun.sso.common.ExpirationPolicy;
import com.tiyiyun.sso.session.AccessTokenManager;

import lombok.extern.slf4j.Slf4j;

/**
 * 本地调用凭证管理
 * 
 * @author Joe
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sso.session.manager", havingValue = "local")
public class LocalAccessTokenManager implements AccessTokenManager, ExpirationPolicy {

	
	@Value("${sso.timeout}")
    private int timeout;

	private Map<String, DummyAccessToken> accessTokenMap = new ConcurrentHashMap<>();
	private Map<String, Set<String>> tgtMap = new ConcurrentHashMap<>();

	@Override
	public void create(String accessToken, AccessTokenContent accessTokenContent) {
		DummyAccessToken dat = new DummyAccessToken(accessTokenContent, System.currentTimeMillis() + getExpiresIn() * 1000);
		accessTokenMap.put(accessToken, dat);

		tgtMap.computeIfAbsent(accessTokenContent.getCodeContent().getTgt(), a -> new HashSet<>()).add(accessToken);
		log.info("调用凭证生成成功, accessToken:{}", accessToken);
	}
	
	@Override
	public AccessTokenContent get(String accessToken) {
		DummyAccessToken dummyAt = accessTokenMap.get(accessToken);
		if (dummyAt == null || System.currentTimeMillis() > dummyAt.expired) {
			return null;
		}
		else {
			return dummyAt.accessTokenContent;
		}
	}
	
	@Override
	public boolean refresh(String accessToken) {
		DummyAccessToken dummyAt = accessTokenMap.get(accessToken);
		if (dummyAt == null || System.currentTimeMillis() > dummyAt.expired) {
			return false;
		}
		dummyAt.expired = System.currentTimeMillis() + getExpiresIn() * 1000;
		return true;
	}

	@Override
	public void remove(String tgt) {
		Set<String> accessTokenSet = tgtMap.remove(tgt);
		if (CollectionUtils.isEmpty(accessTokenSet)) {
			return;
		}
		accessTokenSet.forEach(accessToken -> {
			DummyAccessToken dummyAt = accessTokenMap.get(accessToken);
			if (dummyAt == null || System.currentTimeMillis() > dummyAt.expired) {
				return;
			}
			CodeContent codeContent = dummyAt.accessTokenContent.getCodeContent();
			if (codeContent == null || !codeContent.isSendLogoutRequest()) {
				return;
			}
			log.debug("发起客户端登出请求, accessToken:{}, url:{}", accessToken, codeContent.getRedirectUri());
			sendLogoutRequest(codeContent.getRedirectUri(), accessToken);
		});
	}

	@Scheduled(cron = SCHEDULED_CRON)
	@Override
	public void verifyExpired() {
		accessTokenMap.forEach((accessToken, dummyAt) -> {
			if (System.currentTimeMillis() > dummyAt.expired) {
				accessTokenMap.remove(accessToken);
				log.debug("调用凭证已失效, accessToken:{}", accessToken);
			}
		});
	}
	
	/**
	 * accessToken时效为登录session时效的1/2
	 */
	@Override
	public int getExpiresIn() {
		return timeout / 2;
	}

	private class DummyAccessToken {
		private AccessTokenContent accessTokenContent;
		private long expired; // 过期时间

		public DummyAccessToken(AccessTokenContent accessTokenContent, long expired) {
			super();
			this.accessTokenContent = accessTokenContent;
			this.expired = expired;
		}
	}

	@Override
	public void detele(String accessToken) {
		/**
		 * Explain：添加权益产品信息
		 */
	}
}
