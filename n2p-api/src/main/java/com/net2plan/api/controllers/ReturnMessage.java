package com.net2plan.api.controllers;

import org.springframework.http.HttpStatus;

public class ReturnMessage {

	private String message;
	
	public ReturnMessage ()
	{
		this.message = "Generic Error";
	}

	public ReturnMessage (String message)
	{
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	
}
