package nl.arietimmerman.openam.u2f.datastore;

import org.forgerock.openam.core.rest.devices.DeviceSerialisation;
import org.forgerock.openam.core.rest.devices.JsonDeviceSerialisation;
import org.forgerock.openam.core.rest.devices.services.DeviceService;

/**
 * Implementation of the Trusted Device (Device Print) Service. Provides all necessary configuration information
 * at a realm-wide level to Trusted Device (Device Print) authentication modules underneath it.
 */
public class U2FDeviceService implements DeviceService {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConfigStorageAttributeName() {
        return "devicePrintProfiles";
    }

    @Override
    public DeviceSerialisation getDeviceSerialisationStrategy() {
        return new JsonDeviceSerialisation();
    }

}
