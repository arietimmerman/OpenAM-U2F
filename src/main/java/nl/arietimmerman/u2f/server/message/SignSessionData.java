/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/

package nl.arietimmerman.u2f.server.message;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Base64;

import com.owlike.genson.annotation.JsonIgnore;

import nl.arietimmerman.openam.u2f.datastore.DataStoreHelper;
import nl.arietimmerman.u2f.server.CryptoHelper;
import nl.arietimmerman.u2f.server.message.RegistrationSessionData;

/**
 * SignSessionData as defined in https://fidoalliance.org/specs/fido-u2f-v1.0-nfc-bt-amendment-20150514/fido-u2f-javascript-api.html#idl-def-SignRequest
 */
public class SignSessionData extends RegistrationSessionData {
	
	private final byte[] publicKey;
	
	public final byte[] keyHandle;
	
	public byte[] signedBytes;
	
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
	
	/*
    The application parameter [32 bytes] from the authentication request message.

    The above user presence byte [1 byte].

    The above counter [4 bytes].

    The hash of the [32 bytes] client data.
    
    //not included in SignSessionData normally
	 */
	public byte[] getSignedBytes(){
		return signedBytes;
	}
	
	public void updateSignedBytes(Integer counter){
		byte[] signedBytes = new byte[32 + 1 + 4 + 32];
		
		ClientData clientData = new ClientData(this.getChallenge(), this.getAppId(), ClientData.MESSAGETYPE_GET_ASSERTION);
		byte[] clientDataHash = CryptoHelper.sha256( Base64.decodeBase64(DataStoreHelper.serializeString(clientData).getBytes()));
		
		ByteBuffer.wrap(signedBytes).put(this.getAppIdHash()).put((byte)0x01).putInt(counter).put(clientDataHash);
		
		this.signedBytes = signedBytes;
	}
	
}
