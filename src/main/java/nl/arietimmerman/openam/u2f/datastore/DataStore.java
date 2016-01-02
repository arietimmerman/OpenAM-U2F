/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/

package nl.arietimmerman.openam.u2f.datastore;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;

import nl.arietimmerman.openam.u2f.exception.VerificationException;
import nl.arietimmerman.u2f.server.VerificationHelper;
import nl.arietimmerman.u2f.server.message.RegistrationResponse;
import nl.arietimmerman.u2f.server.message.RegistrationSessionData;
import nl.arietimmerman.u2f.server.message.SignResponse;
import nl.arietimmerman.u2f.server.message.SignSessionData;

import com.sun.identity.idm.AMIdentity;

import net.jodah.expiringmap.ExpiringMap;

/**
 * 
 * Abstract class that helps storing {@link DataStoreElement} objects.
 * Moreover, it provides all methods to generate FIDO U2F RegistrationSessionData elements - for new device registrations - and SignSessionData element - for new log in requests.
 * Also, it provides a basic implementation for storing session data and trusted certificates. 
 */
public abstract class DataStore {
	
	protected String requiredOrigin;
	protected Set<X509Certificate> trustedCertificates = new HashSet<>();
	protected Boolean trustAll;

	private static ExpiringMap<String, RegistrationSessionData> sessionData = ExpiringMap.builder().expiration(240, TimeUnit.SECONDS).build();
	
	/**
	 * This constructor accepts several arguments that MAY be used by implementations of this datastore.
	 * These parameters are configured using the settings page of the module in OpenAM.
	 * @param requiredOrigin
	 * @param trustedCertificates
	 * @param trustAll
	 */
	public DataStore(String requiredOrigin, Set<X509Certificate> trustedCertificates, Boolean trustAll) {
		this.trustedCertificates = trustedCertificates;
		this.requiredOrigin = requiredOrigin;
		this.trustAll = trustAll;
	}
	
	/**
	 * Check if the the specified identity has one or more registered devices.
	 * @param identity
	 * @return
	 */
	public Boolean isRegistered(AMIdentity identity) {
		return !getRegistrationData(identity).isEmpty();
	}

	/**
	 * Store the session data.
	 * @param sessionData
	 */
	protected void storeSessionData(RegistrationSessionData sessionData) {
		DataStore.sessionData.put(sessionData.getSessionId(), sessionData);
	}

	/**
	 * Returns the stored {@link RegistrationSessionData} from the session store.
	 * @param sessionId
	 * @return The stored registration data
	 */
	protected RegistrationSessionData getRegistrationSessionData(String sessionId) {
		return DataStore.sessionData.get(sessionId);
	}

	/**
	 * Returns the stored {@link SignSessionData} from the session store.
	 * @param sessionId
	 * @return
	 */
	public SignSessionData getSignSessionData(String sessionId) {
		return (SignSessionData) DataStore.sessionData.get(sessionId);
	}

	/**
	 * For a registered identity, returns a HashMap that contains a KeyHandle as a Key and a {@link SignSessionData} as a value.
	 * A JSON representation of this object is used as the first argument in the JavaScript window.u2f.sign call.
	 * @param identity
	 * @return
	 */
	public HashMap<String, SignSessionData> generateSignSessionData(AMIdentity identity) {

		String accountName = identity.getUniversalId();

		HashMap<String, SignSessionData> result = new HashMap<String, SignSessionData>();

		for (DataStoreElement securityKeyData : getRegistrationData(identity)) {
			
			SignSessionData signSessionData = new SignSessionData(VerificationHelper.generateSessionId(), accountName, getRequiredOrigin(), VerificationHelper.generateChallenge(), securityKeyData.getPublicKey(), securityKeyData.getKeyHandle());
			
			storeSessionData(signSessionData);
			
			result.put(Base64.encodeBase64URLSafeString(signSessionData.getKeyHandle()), signSessionData);
		}

		return result;
	}
	
	/**
	 * Returns a {@link RegistrationSessionData} element. A JSON representation of this object is used as the first argument in the JavaScript window.u2f.register call. 
	 * @param identity
	 * @return
	 */
	public RegistrationSessionData generateRegistrationSessionData(AMIdentity identity) {

		RegistrationSessionData enrollSessionData = new RegistrationSessionData(VerificationHelper.generateSessionId(), identity.getUniversalId(), getRequiredOrigin(), VerificationHelper.generateChallenge());

		storeSessionData(enrollSessionData);

		return enrollSessionData;
	}
	
	/**
	 * Verifies a {@link RegistrationResponse} and stores it if verification succeeds.
	 * @param identity
	 * @param registrationResponse
	 * @throws VerificationException
	 */
	public void verifyRegistrationResponse(AMIdentity identity, RegistrationResponse registrationResponse) throws VerificationException {
		
		DataStoreElement registrationData = VerificationHelper.verifyRegistrationResponse(registrationResponse, getRegistrationSessionData(registrationResponse.getSessionId()), getTrustedCertificates(), getTrustAll(), getRequiredOrigin());
		storeRegistrationData(identity,registrationData);
		
	}

	/**
	 * Verifies a {@link SignResponse} and stores updated registration data - with an updated counter - if verification succeeds.
	 * @param identity
	 * @param signResponse
	 * @throws VerificationException
	 */
	public void verifySignResponse(AMIdentity identity, SignResponse signResponse) throws VerificationException {
		
		DataStoreElement registrationData = VerificationHelper.verifySignData(signResponse, getSignSessionData(signResponse.getSessionId()), getRegistrationData(identity), getRequiredOrigin());
		storeRegistrationData(identity, registrationData);
		
	}

	protected Set<X509Certificate> getTrustedCertificates() {
		return trustedCertificates;
	}

	protected Boolean getTrustAll() {
		return trustAll;
	}

	protected String getRequiredOrigin() {
		return this.requiredOrigin;
	}
	
	protected void setRequiredOrigin(String requiredOrigin) {
		this.requiredOrigin = requiredOrigin;
	}

	/**
	 * Stores a single {@link DataStoreElement} for an identity.
	 * @param identity
	 * @param registrationData
	 */
	protected void storeRegistrationData(AMIdentity identity, DataStoreElement registrationData) {
		// in the attributes a serialized List<RegistrationData> is stored
		Set<DataStoreElement> registrationDataList = getRegistrationData(identity);
		
		registrationDataList.remove(registrationData);
		registrationDataList.add(registrationData);

		storeRegistrationData(identity, registrationDataList);
	}
	
	/**
	 * Removes a {@link DataStoreElement} for the specified identity, i.e. unregisters a device.
	 * @param identity
	 * @param dataStoreElement
	 */
	public void removeRegistrationData(AMIdentity identity, DataStoreElement dataStoreElement){
		Set<DataStoreElement> registrationDataList = getRegistrationData(identity);
		registrationDataList.remove(dataStoreElement);
		storeRegistrationData(identity, registrationDataList);
	}

	/**
	 * Stores registration data. Every time this method is called, the element registrationData should contain all DataStoreElement objects for this user.
	 * If registrationData is null or empty, the identity becomes unregistered.
	 * @param identity
	 * @param registrationData
	 */
	abstract protected void storeRegistrationData(AMIdentity identity, Set<DataStoreElement> registrationData);
	
	/**
	 * Returns a collection of {@link DataStoreElement} elements for the specified identity.
	 * @param identity
	 * @return
	 */
	abstract public Set<DataStoreElement> getRegistrationData(AMIdentity identity);

}
