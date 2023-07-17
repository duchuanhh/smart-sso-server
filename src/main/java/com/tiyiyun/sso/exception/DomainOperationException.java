package com.tiyiyun.sso.exception;


public class DomainOperationException extends Exception {

	private static final long serialVersionUID = 1L;

	private String code;
	private String message;

	public DomainOperationException(String code, String message) {
		super();
		this.code = code;
		this.message = message;
	}
	
	public DomainOperationException(String message) {
		super();
		this.code = "-1";
		this.message = message;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
