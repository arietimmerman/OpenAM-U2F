/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/
package nl.arietimmerman.openam.u2f.datastore;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.sun.identity.idm.AMIdentity;

public class MemoryDataStore extends DataStore {

	private static HashMap<AMIdentity, Set<DataStoreElement>> store = new HashMap<AMIdentity, Set<DataStoreElement>>();
	
	public MemoryDataStore(String requiredOrigin, Set<X509Certificate> trustedCertificates, Boolean trustAll) {
		super(requiredOrigin, trustedCertificates, trustAll);
	}
	
	@Override
	public void storeRegistrationData(AMIdentity identity, Set<DataStoreElement> registrationData) {
		store.put(identity, registrationData);
	}

	@Override
	public Set<DataStoreElement> getRegistrationData(AMIdentity identity) {
		
		Set<DataStoreElement> result = store.get(identity);
		
		if(result == null){
			result = new HashSet<DataStoreElement>();
		}
		
		return result;
	}
	
}
