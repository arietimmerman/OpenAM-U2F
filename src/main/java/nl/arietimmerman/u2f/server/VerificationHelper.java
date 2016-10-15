/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/
package nl.arietimmerman.u2f.server;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

import nl.arietimmerman.openam.u2f.datastore.DataStoreElement;
import nl.arietimmerman.openam.u2f.datastore.DataStoreHelper;
import nl.arietimmerman.openam.u2f.exception.VerificationException;
import nl.arietimmerman.u2f.server.message.ClientData;
import nl.arietimmerman.u2f.server.message.RegistrationResponse;
import nl.arietimmerman.u2f.server.message.RegistrationSessionData;
import nl.arietimmerman.u2f.server.message.SignResponse;
import nl.arietimmerman.u2f.server.message.SignSessionData;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class contains all methods to verify RegistrationResponse, ClientData and SignResponse objects, i.e. the verify all FIDO U2F messages.
 */
public class VerificationHelper {

	private static final Logger Log = Logger.getLogger(VerificationHelper.class.getName());

	

	private static VerificationHelper instance;
	public static final byte USER_PRESENT_FLAG = 0x01;

	/**
	 * Generates a session identifier
	 * 
	 * @return web-safe Base64 encoded session identifier
	 */
	public static String generateSessionId() {
		// TODO: check specs on session id requirements

		byte[] s = new byte[32];
		new SecureRandom().nextBytes(s);
		return Base64.encodeBase64URLSafeString(s);
	}

	// A challenge is 32 bytes
	public static byte[] generateChallenge() {
		byte[] s = new byte[32];
		new SecureRandom().nextBytes(s);
		return s;
	}

	/**
	 * Client Data contains the following type (the constant 'navigator.id.getAssertion' for authentication, and 'navigator.id.finishEnrollment' for registration) challenge (the websafe-base64-encoded challenge provided by the relying party) origin (the facet id of the caller, i.e., the web origin of the relying party)
	 * 
	 * @param clientData
	 * @param messageType
	 * @param sessionData
	 * @throws IOException
	 * @throws JSONException
	 */
	public static void verifyClientData(ClientData clientData, String messageType, RegistrationSessionData sessionData, String origin) throws IOException, JSONException {
		
		if (clientData.getTyp() == null) {
			throw new IOException("bad browserdata: missing 'typ' param");
		}

		String type = clientData.getTyp();
		if (!messageType.equals(type)) {
			throw new IOException("bad browserdata: bad type " + type);
		}

		// check that the right challenge is in the clientData
		if (clientData.getChallenge() == null) {
			throw new IOException("bad browserdata: missing 'challenge' param");
		}

		if (clientData.getOrigin() != null) {
			verifyOrigin(origin, clientData.getOrigin());
		}

		byte[] challengeFromClientData = clientData.getChallenge();

		if (!Arrays.equals(challengeFromClientData, sessionData.getChallenge())) {
			throw new IOException("wrong challenge signed in browserdata");
		}

		// TODO: Deal with ChannelID
	}

	private static void verifyOrigin(String origin, String originFound) throws IOException {
		if (!StringUtils.equals(origin, originFound)) {
			throw new IOException(String.format("Incorrect origin. Expected %s but found %s", origin, originFound));
		}
	}

	public static DataStoreElement verifyRegistrationResponse(RegistrationResponse registrationResponse, RegistrationSessionData sessionData, Set<X509Certificate> trustedCertificates, Boolean trustAll, String origin) throws VerificationException {

		ClientData clientData = registrationResponse.getClientData();
		byte[] bytesRegistrationData = registrationResponse.getRegistrationData();

		// Find the session data used to start enrolling the U2F device. This is
		// only used to retrieve the original account name
		String appId = sessionData.getAppId();
		
		Log.info(String.format("Received clientData: %s",clientData));

		// 1. Verify the clientData
		try {
			VerificationHelper.verifyClientData(clientData, ClientData.MESSAGETYPE_FINISH_ENROLLMENT, sessionData, origin);
		} catch (IOException | JSONException e) {
			throw new VerificationException(e);
		}

		//
		// registrationData is a base64-encoded byte string containing the
		// following data
		// +----------------+-----------------------+----------------------------+----------------+---------------------------------+------------+
		// |1 (always 0x05) | 64 (user public key) | 1 (length L for next part)
		// | L (key handle) | X.509 certificate in DER format | signature |
		// +----------------+-----------------------+----------------------------+----------------+---------------------------------+------------+
		//

		// Wrap the byte array in a DataInputStream for easy processing
		DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(bytesRegistrationData));

		// 2. Read the reserved byte
		byte reservedByte;
		try {
			reservedByte = inputStream.readByte();
		} catch (IOException e) {
			throw new VerificationException(e);
		}

		if (reservedByte != (byte) 0x05) {
			throw new VerificationException(String.format("Incorrect value of reserved byte. Expected: %d. Was: %d", (byte) 0x05, reservedByte));
		}

		// 3. Read the user's public key
		byte[] userPublicKey = new byte[65];
		try {
			inputStream.readFully(userPublicKey);
		} catch (IOException e) {
			throw new VerificationException(e);
		}

		// 4. Read the length of the key handle, and the key handle itself
		byte[] keyHandle;
		try {
			int length = inputStream.readUnsignedByte();

			System.out.println("Length is now: " + length);
			keyHandle = new byte[length];
			inputStream.readFully(keyHandle);
		} catch (IOException e) {
			throw new VerificationException(e);
		}

		System.out.println("keyHandle: " + Base64.encodeBase64String(keyHandle));

		// 5. Read an X.509 certificate from the inputstream
		X509Certificate attestationCertificate = null;

		try {

			attestationCertificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(inputStream);
		} catch (CertificateException e) {
			e.printStackTrace();
		}

		if (attestationCertificate == null) {
			Log.info("attestationCertificate is null");
		}

		// 6. Read the signature from the remaining bytes
		byte[] signature;
		try {
			signature = new byte[inputStream.available()];
			inputStream.readFully(signature);
		} catch (IOException e) {
			throw new VerificationException(e);
		}

		// 7. Verify the signature
		byte[] appIdSha256 = CryptoHelper.sha256(appId.getBytes());

		
		
		Log.fine("Challenge: " + new String(Base64.encodeBase64URLSafe(clientData.getChallenge())));		
		Log.fine("client data string: " + new String(Base64.decodeBase64(DataStoreHelper.serializeString(clientData))));
		
		 byte[] clientDataHash = CryptoHelper.sha256(Base64.decodeBase64(DataStoreHelper.serializeString(clientData).getBytes()));		
		byte[] signedBytes = new byte[1 + appIdSha256.length + clientDataHash.length + keyHandle.length + userPublicKey.length];
		
		ByteBuffer.wrap(signedBytes).put((byte) 0x00).put(appIdSha256).put(clientDataHash).put(keyHandle).put(userPublicKey);

		Log.info("Verifying signature of bytes (1) " + Hex.encodeHexString(signedBytes));

		if (!CryptoHelper.verifySignature(attestationCertificate.getPublicKey(), signedBytes, signature)) {
			Log.info("Signature is invalid!!!");
			throw new VerificationException("Signature is invalid");
		} else {
			Log.info("Signature is valid!!!");
		}

		// 8. Verify attestation certificate
		if (trustedCertificates == null || !trustedCertificates.contains(attestationCertificate)) {
			if (!trustAll) {
				try {
					Log.info("Not trusted: " + Base64.encodeBase64String(attestationCertificate.getEncoded()));
				} catch (CertificateEncodingException ignore) {
				}

				throw new VerificationException("Attestion certificate is not trusted");
			} else {
				Log.info("Attestion certificate is not trusted");
			}
		} else {
			Log.info("Attestion certificate IS trusted");
		}

		return new DataStoreElement(sessionData.getAccountName(), keyHandle, userPublicKey, attestationCertificate);
	}

	// FIXME: check for trustedCertificates here as well. Could be that
	// something has been removed from the white list after registering
	public static DataStoreElement verifySignData(SignResponse signResponse, SignSessionData signSessionData, Set<DataStoreElement> registrationDataList, String requiredOrigin) throws VerificationException {

		ClientData clientData = signResponse.getClientData();
		byte[] signatureData = signResponse.getSignatureData();
		String appId = signResponse.getAppId();

		if (signSessionData == null) {
			throw new VerificationException("Unknown session_id");
		}
		
		// 1. Verify the clientData
		try {
			VerificationHelper.verifyClientData(clientData, ClientData.MESSAGETYPE_GET_ASSERTION, (RegistrationSessionData) signSessionData, requiredOrigin);
		} catch (IOException | JSONException e) {
			throw new VerificationException(e);
		}

		// 2. Find the stored security key data
		// FIXME: should match appId, String appId = signSessionData.getAppId();
		DataStoreElement registrationData = null;

		// Find the stored registrationData for the provided public key.
		// Multiple devices could be registered. One should match
		// Note that for each key a unique session id is used
		for (DataStoreElement temp : registrationDataList) {
			if (Arrays.equals(signSessionData.getPublicKey(), temp.getPublicKey())) {
				registrationData = temp;
				break;
			}
		}

		if (registrationData == null) {
			throw new VerificationException("No security keys registered for this user");
		}

		//
		// signatureData is a base64-encoded byte string containing the
		// following data
		// +------------------+---------------------+-----+
		// |1 (user presence) | 4 (counter) | signature |
		// +------------------+--------------+------+-----+
		//
		DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(signatureData));
		byte userPresence;
		int counter;
		byte[] signature;

		try {
			// 2. Read the user presence byte
			userPresence = inputStream.readByte();

			if (userPresence != USER_PRESENT_FLAG) {
				throw new VerificationException("No user presence shown");
			}

			// 3. Read the counter
			counter = inputStream.readInt(); // i.e. 4 bytes

			if (counter <= registrationData.getCounter()) {
				throw new VerificationException(String.format("Counter value smaller than expected! Got %d, but expected a value larger than %d", counter, registrationData.getCounter()));
			}

			// 4. Read the signature
			signature = new byte[inputStream.available()];
			inputStream.readFully(signature);
		} catch (IOException e) {
			throw new VerificationException(e);
		}

		// 5. Validate the signature
		// A signature should be set for the app id, user presence, counter and
		// the client data
		byte[] appIdHash = CryptoHelper.sha256(appId.getBytes());
		byte[] clientDataHash = CryptoHelper.sha256(Base64.decodeBase64(DataStoreHelper.serializeString(clientData)));
		
		byte[] signedBytes = new byte[appIdHash.length + 1 + 4 + clientDataHash.length];
		
		
		ByteBuffer.wrap(signedBytes).put(appIdHash).put(userPresence).putInt(counter).put(clientDataHash);
		
		if (!CryptoHelper.verifySignature(CryptoHelper.decodePublicKey(registrationData.getPublicKey()), signedBytes, signature)) {
			throw new VerificationException("Signature is invalid");
		}

		registrationData.setCounter(counter);

		// return the registrationData with the updated counter
		return registrationData;
	}

}
