/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/

package nl.arietimmerman.u2f.server.message;

import nl.arietimmerman.u2f.server.message.RegistrationSessionData;

/**
 * SignSessionData as defined in https://fidoalliance.org/specs/fido-u2f-v1.0-nfc-bt-amendment-20150514/fido-u2f-javascript-api.html#idl-def-SignRequest
 */
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
