/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/
/*
 * Server data used to initiate the registration request
 */

package nl.arietimmerman.u2f.server.message;

/**
 * RegisterRequest as defined in https://fidoalliance.org/specs/fido-u2f-v1.0-nfc-bt-amendment-20150514/fido-u2f-javascript-api.html#idl-def-RegisterRequest
 */
public class RegistrationSessionData  {
	
	public String version = "U2F_V2";
	public  String accountName;
	public  byte[] challenge;
	public  String appId;
	public  String sessionId;

	public RegistrationSessionData() {
		
	}
	
	public RegistrationSessionData(String sessionId, String accountName, String appId, byte[] challenge) {
		this.sessionId = sessionId;
		this.accountName = accountName;
		this.challenge = challenge;
		this.appId = appId;
	}
	
	public String getAccountName() {
		return accountName;
	}

	public byte[] getChallenge() {
		return challenge;
	}

	public String getAppId() {
		return appId;
	}

	public String getSessionId() {
		return sessionId;
	}

}
