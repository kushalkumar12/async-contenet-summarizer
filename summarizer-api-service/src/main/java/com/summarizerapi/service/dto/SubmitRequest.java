package com.summarizerapi.service.dto;

import lombok.Data;

@Data
public class SubmitRequest {
	
	//User sends either URI or Raw Text Not both at a time.
	private String url;
	
    private String text;
}
