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

public abstract class DataStore {
	
	protected String requiredOrigin;
	protected Set<X509Certificate> trustedCertificates = new HashSet<>();
	protected Boolean trustAll;

	private static ExpiringMap<String, RegistrationSessionData> sessionData = ExpiringMap.builder().expiration(240, TimeUnit.SECONDS).build();
	
	public DataStore(String requiredOrigin, Set<X509Certificate> trustedCertificates, Boolean trustAll) {
		this.trustedCertificates = trustedCertificates;
		this.requiredOrigin = requiredOrigin;
		this.trustAll = trustAll;
	}

	protected void storeSessionData(RegistrationSessionData sessionData) {
		DataStore.sessionData.put(sessionData.getSessionId(), sessionData);
	}

	protected RegistrationSessionData getEnrollSessionData(String sessionId) {
		return DataStore.sessionData.get(sessionId);
	}

	public SignSessionData getSignSessionDataUsed(String sessionId) {
		return (SignSessionData) DataStore.sessionData.get(sessionId);
	}
	
	public Boolean isRegistered(AMIdentity identity) {
		return !getRegistrationData(identity).isEmpty();
	}

	public HashMap<String, SignSessionData> getSignSessionData(AMIdentity identity) {

		String accountName = identity.getUniversalId();

		HashMap<String, SignSessionData> result = new HashMap<String, SignSessionData>();

		for (DataStoreElement securityKeyData : getRegistrationData(identity)) {
			SignSessionData signSessionData = new SignSessionData(VerificationHelper.generateSessionId(), accountName, getRequiredOrigin(), VerificationHelper.generateChallenge(), securityKeyData.getPublicKey(), securityKeyData.getKeyHandle());
			
			storeSessionData(signSessionData);
			
			result.put(Base64.encodeBase64URLSafeString(signSessionData.getKeyHandle()), signSessionData);
		}

		return result;
	}
	
	public RegistrationSessionData getRegistrationRequestData(AMIdentity identity) {

		RegistrationSessionData enrollSessionData = new RegistrationSessionData(VerificationHelper.generateSessionId(), identity.getUniversalId(), getRequiredOrigin(), VerificationHelper.generateChallenge());

		storeSessionData(enrollSessionData);

		return enrollSessionData;
	}

	public void verifyRegistrationResponse(AMIdentity identity, RegistrationResponse registrationResponse) throws VerificationException {
		
		DataStoreElement registrationData = VerificationHelper.verifyRegistrationResponse(registrationResponse, getEnrollSessionData(registrationResponse.getSessionId()), getTrustedCertificates(), getTrustAll(), getRequiredOrigin());
		storeRegistrationData(identity,registrationData);
		
	}

	public void verifySignResponse(AMIdentity identity, SignResponse signResponse) throws VerificationException {
		
		DataStoreElement registrationData = VerificationHelper.verifySignData(signResponse, getSignSessionDataUsed(signResponse.getSessionId()), getRegistrationData(identity), getRequiredOrigin());
		storeRegistrationData(identity, registrationData);
		
	}

	protected void addTrustedCertificates(Set<X509Certificate> trustedCertificates) {
		this.trustedCertificates.addAll(trustedCertificates);
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

	protected void storeRegistrationData(AMIdentity identity, DataStoreElement registrationData) {
		// in the attributes a serialized List<RegistrationData> is stored
		Set<DataStoreElement> registrationDataList = getRegistrationData(identity);
		
		registrationDataList.remove(registrationData);
		registrationDataList.add(registrationData);

		storeRegistrationData(identity, registrationDataList);
	}
	
	public void removeRegistrationData(AMIdentity identity, DataStoreElement dataStoreElement){
		Set<DataStoreElement> registrationDataList = getRegistrationData(identity);
		registrationDataList.remove(dataStoreElement);
		storeRegistrationData(identity, registrationDataList);
	}

	abstract protected void storeRegistrationData(AMIdentity identity, Set<DataStoreElement> registrationData);
	abstract public Set<DataStoreElement> getRegistrationData(AMIdentity identity);

}
