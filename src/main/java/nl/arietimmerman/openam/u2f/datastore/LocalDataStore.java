/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/
package nl.arietimmerman.openam.u2f.datastore;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.owlike.genson.GenericType;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

public class LocalDataStore extends DataStore {

	private final static String ATTRIBUTE_NAME = "u2fdevices";
	
	public LocalDataStore(String requiredOrigin, Set<X509Certificate> trustedCertificates, Boolean trustAll){ 
		super(requiredOrigin, trustedCertificates, trustAll);
	}

	@Override
	public void storeRegistrationData(AMIdentity identity, Set<DataStoreElement> registrationData) {
		@SuppressWarnings("rawtypes")
		Map<String, Set> attributes = new HashMap<String, Set>();
		Set<String> values = new HashSet<String>();
		
		values.add(DataStoreHelper.serialize(registrationData));

		attributes.put(ATTRIBUTE_NAME, values);
		
		try {
			identity.setAttributes(attributes);
			identity.store();
		} catch (SSOException | IdRepoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Retrieve all RegistrationData objects for the specified account
	 * 
	 * @param accountName
	 * @return
	 */
	public Set<DataStoreElement> getRegistrationData(AMIdentity identity) {

		Set<DataStoreElement> result = new HashSet<DataStoreElement>();

		try {
			Set<?> values = identity.getAttribute(ATTRIBUTE_NAME);

			if (values != null) {
				for (Object value : values) {
					String valueString = (String) value;
					
					if (valueString != null && valueString.trim().length() > 0) {
						result = DataStoreHelper.deserialize(valueString, new GenericType<Set<DataStoreElement>>() {
						});
					}
				}
			}

		} catch (SSOException | IdRepoException e) {
			//TODO: handler errors
			e.printStackTrace();
		} catch (Exception e) {
			//TODO: handler errors
			e.printStackTrace();
		}

		return result;
	}
}
