package com.evolvus.sandstorm.csv.bean;

import lombok.Data;

@Data
public class Response {

	private String status;

	private String description;

	private Object data;
	
	public Response() {}

	public Response(String status) {
		this.status = status;
	}

}
