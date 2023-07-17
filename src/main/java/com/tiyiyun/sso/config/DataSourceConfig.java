package com.tiyiyun.sso.config;

import lombok.Data;

@Data
public class DataSourceConfig {
	private String url;
	private String type;
	private String loginUserInfoEntityname;
	private String loginPassword;
	private String UserInfoEntityname;
	private String password;
	private String driverClassName;
	private int initialSize;
	private int minIdle;
	private int maxActive;
	private int maxWait;
	private int timeBetweenEvictionRunsMillis;
	private int minEvictableIdleTimeMillis;
	private String validationQuery;
	private boolean testWhileIdle;
	private boolean testOnBorrow;
	private boolean testOnReturn;
	private String filters;
	private String logSlowSql;
	private String cryptid;
	private boolean poolPreparedStatements;
	private int maxOpenPreparedStatements;
}
