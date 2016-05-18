/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/

package nl.arietimmerman.u2f.server.message;

/**
 * SignResponse as defined in https://fidoalliance.org/specs/fido-u2f-v1.0-nfc-bt-amendment-20150514/fido-u2f-javascript-api.html#idl-def-SignResponse
 */
public class SignResponse {

	private String sessionId;
	private ClientData clientData;
	private byte[] signatureData;
	private String appId;
	private byte[] keyHandle;
	
	public SignResponse() {
		// TODO Auto-generated constructor stub
	}
	
	public void setClientData(ClientData clientData) {
		this.clientData = clientData;
	}
	
	public void setSignatureData(byte[] signatureData) {
		this.signatureData = signatureData;
	}
	
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	public ClientData getClientData() {
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

	public byte[] getKeyHandle() {
		return keyHandle;
	}

	public void setKeyHandle(byte[] keyHandle) {
		this.keyHandle = keyHandle;
	}
	
}
