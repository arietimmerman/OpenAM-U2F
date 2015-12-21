/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/
/**
 * Storage of registration data
 */

package nl.arietimmerman.openam.u2f.datastore;

import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class DataStoreElement {
	private final String accountName;
	private final String deviceName;
	private final byte[] publicKey;
	private final byte[] keyHandle;
	private X509Certificate attestationCertificate;
	private final Date registrationDate;
	private int counter;
	
	public DataStoreElement(String accountName, byte[] keyHandle, byte[] publicKey, X509Certificate attestationCertificate) {
		this.accountName = accountName;
		this.publicKey = publicKey;
		this.keyHandle = keyHandle;
		this.attestationCertificate = attestationCertificate;
		this.deviceName = null;
		this.registrationDate = new Date();
		this.counter = 0;
	}
	
	public String getDeviceName(){
		String result = this.deviceName;
		
		if(result == null){
			//FIXME: make something better
			result = getAttestationCertificate().getSubjectX500Principal().getName().replaceAll("^CN=", "");
			
			String[] words = result.split(" ");
			
			if(words != null && words.length > 0){
				result = String.format("%s - %s",words[0],new SimpleDateFormat("yyyy-MM-dd HH:mm").format(getRegistrationDate()));
			}
			
		}
		
		return result;
	}
	
	public String getAccountName(){
		return accountName;
	}
	
	public byte[] getPublicKey() {
		return publicKey;
	}

	public byte[] getKeyHandle() {
		return keyHandle;
	}
	
	public X509Certificate getAttestationCertificate() {
		return attestationCertificate;
	}
	
	public void setAttestationCertificate(X509Certificate attestationCertificate) {
		this.attestationCertificate = attestationCertificate;
	}
	
	//Calculated when creating
	public Date getRegistrationDate() {
		return registrationDate;
	}
	
	public int getCounter() {
		return counter;
	}
	
	public void setCounter(int counter) {
		this.counter = counter;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(this.getAccountName()).append(this.getPublicKey()).toHashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof DataStoreElement))
            return false;
        if (other == this)
            return true;
        
        DataStoreElement otherDataStoreElement = (DataStoreElement) other;
        
        return new EqualsBuilder().append(this.getAccountName(), otherDataStoreElement.getAccountName()).append(this.getPublicKey(), otherDataStoreElement.getPublicKey()).isEquals();
	}
	
}
