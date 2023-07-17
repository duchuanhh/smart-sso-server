package com.tiyiyun.sso.session;

import java.util.UUID;

import com.smart.sso.client.entity.UserInfoEntity;
import com.tiyiyun.sso.common.Expiration;

/**
 * 登录凭证（TGT）管理抽象
 * 
 * @author Joe
 */
public interface TicketGrantingTicketManager extends Expiration {
	
    /**
     * 登录成功后，根据用户信息生成令牌
     * 
     * @param UserInfoEntity
     * @return
     */
	default String generate(UserInfoEntity UserInfoEntity) {
		String tgt = "TGT-" + UUID.randomUUID().toString().replaceAll("-", "");
		//create(tgt, UserInfoEntity);
		return tgt;
	}
    
    /**
     * 登录成功后，根据用户信息生成令牌
     * 
     * @param UserInfoEntity
     * @return
     */
    void create(String tgt, UserInfoEntity UserInfoEntity);
    
    /**
     * 验证st是否存在且在有效期内，并更新过期时间戳
     * 
     * @param tgt
     * @return
     */
    UserInfoEntity getAndRefresh(String tgt);
    
    /**
     * 设置新的用户信息
     * 
     * @param UserInfoEntity
     * @return
     */
    void set(String tgt, UserInfoEntity UserInfoEntity);
    
    /**
     * 移除
     * 
     * @param tgt
     */
    void remove(String tgt);
}
