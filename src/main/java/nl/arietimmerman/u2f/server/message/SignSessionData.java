/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/
/*
 * Server data to initiate login request 
 */

package nl.arietimmerman.u2f.server.message;

import nl.arietimmerman.u2f.server.message.RegistrationSessionData;

public class SignSessionData extends RegistrationSessionData {
	
	private final byte[] publicKey;
	
	public final byte[] keyHandle;
	
	public SignSessionData(String sessionId, String accountName, String appId, byte[] challenge, byte[] publicKey, byte[] keyHandle) {
		super(sessionId, accountName, appId, challenge);
		this.publicKey = publicKey;
		this.keyHandle = keyHandle;
	}
	
	public byte[] getPublicKey() {
		return publicKey;
	}
	
	public byte[] getKeyHandle() {
		return keyHandle;
	}
}
