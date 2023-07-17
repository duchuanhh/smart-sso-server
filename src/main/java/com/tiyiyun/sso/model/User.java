package com.tiyiyun.sso.model;

import java.io.Serializable;

/**
 * 用户
 */
public class User implements Serializable {

	private static final long serialVersionUID = 10125567610925057L;

	/** ID */
	private Integer id;
	/** 姓名 */
	private String name;
	/** 登录名 */
	private String UserInfoEntityname;
	/** 密码 */
	private String password;
	
	public User() {
		super();
	}

	public User(Integer id, String name, String UserInfoEntityname, String password) {
		super();
		this.id = id;
		this.name = name;
		this.UserInfoEntityname = UserInfoEntityname;
		this.password = password;
	}
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUserInfoEntityname() {
		return UserInfoEntityname;
	}

	public void setUserInfoEntityname(String UserInfoEntityname) {
		this.UserInfoEntityname = UserInfoEntityname;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
