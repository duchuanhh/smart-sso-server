package com.tiyiyun.sso.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlipayAuthUserDto {
	/** 支付宝用户的userId */
    private String userId;

    /** 系统用户ID */
    private Long sysUserId;

    /** 用户头像地址 */
    private String avatar;

    /** 性别(F:女 M:男) */
    private String gender;

    /** 昵称 */
    private String nickName;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 创建时间 */
    private Date createTime;
}
