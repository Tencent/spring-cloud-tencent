package com.tencent.cloud.polaris.circuitbreaker.exception;

public class FallbackWrapperException extends RuntimeException {

	public FallbackWrapperException(Throwable cause) {
		super(cause);
	}

}

