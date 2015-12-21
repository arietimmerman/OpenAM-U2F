/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/
package nl.arietimmerman.openam.u2f.exception;

/**
 * Wrapper for the Exception class
 */

public class VerificationException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = -358663719218473043L;

	public VerificationException(String message) {
		super(message);
	}
	
	public VerificationException(Throwable e){
		super(e);
	}
	
}
