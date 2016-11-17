/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package nl.arietimmerman.openam.u2f.datastore;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;

import nl.arietimmerman.openam.u2f.datastore.DataStoreElement;
import nl.arietimmerman.openam.u2f.datastore.DataStoreHelper;

import org.forgerock.http.util.Json;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openam.authentication.modules.fr.oath.JsonConversionUtils;
import org.forgerock.openam.core.rest.devices.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.OathDevicesDao;
import org.forgerock.util.Reject;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


final class U2FMaker {
	
    
    private final U2FDevicesDao devicesDao;

    @Inject
    U2FMaker(final @Nonnull U2FDevicesDao devicesDao
              ) {
        Reject.ifNull(devicesDao);
        this.devicesDao = devicesDao;
        
//        this.debug = debug;
    }
    
    public void saveDeviceProfile(@Nonnull String user, @Nonnull String realm, @Nonnull DataStoreElement dataStoreElement)
            throws AuthLoginException {
    	
    	System.out.println(String.format("user: %s, realm: %s", user,realm));
    	
    	if(dataStoreElement == null){
    		System.out.println("dataStoreElement is null");
    	}else{
    		System.out.println("dataStoreElement is NOT null");
    	}
    	
        Reject.ifNull(user, realm, dataStoreElement);
        try {
        	System.out.println("saveDeviceProfile");
        	JsonValue jsonValue = new JsonValue(Json.readJson(DataStoreHelper.serialize(dataStoreElement)));
        	System.out.println("saveDeviceProfile 2");
        	List<JsonValue> settings = new ArrayList<JsonValue>();
        	System.out.println("saveDeviceProfile 3");
        	settings.add(jsonValue);
        	System.out.println("saveDeviceProfile 4");
        	
        	
        	
            devicesDao.saveDeviceProfiles(user, realm, settings);
        } catch (Exception e) {
//            debug.error("OathMaker.createDeviceProfile(): Unable to save device profile for user {} in realm {}",
//                    user, realm, e);
        	e.printStackTrace();
        	
            throw new AuthLoginException(e);
        }
    }
    
    List<OathDeviceSettings> getDeviceProfiles(@Nonnull String username, @Nonnull String realm)
            throws IOException {
        
    	try {
			List<JsonValue> deviceProfiles = devicesDao.getDeviceProfiles(username, realm);
		} catch (InternalServerErrorException e) {
			throw new IOException(e);
		}
    	
    	return null;
    	
    }

}
