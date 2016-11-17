package nl.arietimmerman.openam.u2f.datastore;

import javax.inject.Inject;

import org.forgerock.openam.core.rest.devices.UserDevicesDao;

public class U2FDevicesDao extends UserDevicesDao {

	@Inject
	public U2FDevicesDao(AuthenticatorU2FhServiceFactory serviceFactory) {
		super(serviceFactory);
	}
	
	
	
}
