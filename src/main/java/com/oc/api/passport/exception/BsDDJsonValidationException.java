package com.oc.api.passport.exception;

public class BsDDJsonValidationException extends Exception{

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BsDDJsonValidationException(String message) {
        super(message);
    }

    public BsDDJsonValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}
