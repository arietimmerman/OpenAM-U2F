package nl.arietimmerman.openam.u2f.datastore;

import org.forgerock.openam.core.rest.devices.services.DeviceService;
import org.forgerock.openam.core.rest.devices.services.DeviceServiceFactory;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

public class AuthenticatorU2FhServiceFactory implements DeviceServiceFactory {
	
	@Override
	public U2FDeviceService create(String realm) throws SSOException, SMSException {
		return new U2FDeviceService();
	}

}
