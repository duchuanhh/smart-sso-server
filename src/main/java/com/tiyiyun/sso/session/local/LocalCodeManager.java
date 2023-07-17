package com.tiyiyun.sso.session.local;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tiyiyun.sso.common.CodeContent;
import com.tiyiyun.sso.common.ExpirationPolicy;
import com.tiyiyun.sso.session.CodeManager;

import lombok.extern.slf4j.Slf4j;

/**
 * 本地授权码管理
 * 
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sso.session.manager", havingValue = "local")
public class LocalCodeManager implements CodeManager, ExpirationPolicy {
	

	private Map<String, DummyCode> codeMap = new ConcurrentHashMap<>();
	
	@Override
	public void create(String code, CodeContent codeContent) {
		codeMap.put(code, new DummyCode(codeContent, System.currentTimeMillis() + getExpiresIn() * 1000));
		log.info("授权码生成成功, code:{}", code);
	}

	@Override
	public CodeContent getAndRemove(String code) {
		DummyCode dc = codeMap.remove(code);
        if (dc == null || System.currentTimeMillis() > dc.expired) {
            return null;
        }
        return dc.codeContent;
	}
	
	@Scheduled(cron = SCHEDULED_CRON)
	@Override
    public void verifyExpired() {
		codeMap.forEach((code, dummyCode) -> {
            if (System.currentTimeMillis() > dummyCode.expired) {
                codeMap.remove(code);
                log.info("授权码已失效, code:{}", code);
            }
        });
    }
	
    private class DummyCode {
    	private CodeContent codeContent;
        private long expired; // 过期时间

        public DummyCode(CodeContent codeContent, long expired) {
            this.codeContent = codeContent;
            this.expired = expired;
        }
    }
}
