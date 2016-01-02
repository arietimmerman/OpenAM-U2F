/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/
/*
 * Response of the client to the server when registering
 */

package nl.arietimmerman.u2f.server.message;

/**
 * RegistrationResponse as defined in https://fidoalliance.org/specs/fido-u2f-v1.0-nfc-bt-amendment-20150514/fido-u2f-javascript-api.html#idl-def-RegisterResponse
 */
public class RegistrationResponse {

	private String sessionId;
	private byte[] clientData;
	private byte[] registrationData;
	
	public RegistrationResponse() {
		
	}
	
	public void setClientData(byte[] clientData) {
		this.clientData = clientData;
	}
	
	public void setRegistrationData(byte[] registrationData) {
		this.registrationData = registrationData;
	}
	
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	public byte[] getClientData() {
		return clientData;
	}
	
	public byte[] getRegistrationData() {
		return registrationData;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
}
