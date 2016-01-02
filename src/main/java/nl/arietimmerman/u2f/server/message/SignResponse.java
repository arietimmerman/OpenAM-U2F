/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/

package nl.arietimmerman.u2f.server.message;

/**
 * SignResponse as defined in https://fidoalliance.org/specs/fido-u2f-v1.0-nfc-bt-amendment-20150514/fido-u2f-javascript-api.html#idl-def-SignResponse
 */
public class SignResponse {

	private String sessionId;
	private byte[] clientData;
	private byte[] signatureData;
	private String appId;
	
	public SignResponse() {
		// TODO Auto-generated constructor stub
	}
	
	public void setClientData(byte[] clientData) {
		this.clientData = clientData;
	}
	
	public void setSignatureData(byte[] signatureData) {
		this.signatureData = signatureData;
	}
	
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	public byte[] getClientData() {
		return clientData;
	}
	
	public byte[] getSignatureData() {
		return signatureData;
	}
	
	public String getAppId() {
		return appId;
	}
	
	public void setAppId(String appId) {
		this.appId = appId;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
}
