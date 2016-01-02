/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/
package nl.arietimmerman.openam.u2f;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang.ArrayUtils;

import nl.arietimmerman.openam.u2f.datastore.DataStoreElement;
import nl.arietimmerman.openam.u2f.datastore.DataStoreHelper;
import nl.arietimmerman.openam.u2f.exception.VerificationException;
import nl.arietimmerman.u2f.server.message.RegistrationResponse;
import nl.arietimmerman.u2f.server.message.SignResponse;

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;

/**
 * FIXME: show not-configured warning. FIXME: show LDIF import needed warning /
 * attribute missing
 * 
 * @author atimmerman
 *
 */

public class U2F extends U2FAMLoginModule {

	// Name for the debug-log
	private final static String DEBUG_NAME = "U2F";

	// User names for authentication logic
	private String username;

	// Orders defined in the callbacks file
	private final static int STATE_BEGIN = 1;
	private final static int STATE_LOGIN = 2;
	private final static int STATE_MANAGE_DEVICES = 3;
	private final static int STATE_NEW_DEVICE = 4;
	private final static int STATE_ERROR = 5;

	public U2F() {
		super();
	}

	@Override
	public CallbackHandler getCallbackHandler() {
		return super.getCallbackHandler();
	}
	
	private int getStateManageDevices(String successMessage) throws AuthLoginException {
		
		if (successMessage != null) {
			replaceCallback(STATE_MANAGE_DEVICES, 0, new TextOutputCallback(TextOutputCallback.INFORMATION, successMessage));
		}

		List<String> devices = new ArrayList<String>();
		
		for (DataStoreElement dataStoreElement : getDataStore().getRegistrationData(getIdentity())) {
			devices.add(dataStoreElement.getDeviceName());
		}
		
		ChoiceCallback choiceCallback = new ChoiceCallback(bundle.getString("u2f-register-choice-header"), devices.toArray(new String[] {}), 0, true);
		
		replaceCallback(STATE_MANAGE_DEVICES, 1, choiceCallback);
		
		return STATE_MANAGE_DEVICES;
	}

	@Override
	public int process(Callback[] callbacks, int state) throws LoginException {

		substituteUIStrings();
		
		AMIdentity amIdentity = getIdentity();

		if (amIdentity != null) {
			username = amIdentity.getName();
		}

		switch (state) {
		case STATE_BEGIN:
			
			if (amIdentity == null) {
				setErrorHeader(bundle.getString("u2f-error-second-factor"));

				return STATE_ERROR;
			} else if (!getDataStore().isRegistered(getIdentity()) && isManageAllowed()) {
				
				replaceCallback(STATE_NEW_DEVICE, 1, createRegisterScriptCallback(getIdentity()));

				return STATE_NEW_DEVICE;

			} else if (getDataStore().isRegistered(getIdentity()) && isLoginAllowed()) {
				
				replaceCallback(STATE_LOGIN, 1, createSignScriptCallback(amIdentity));
				
				return STATE_LOGIN;

			} else {
				setErrorHeader(bundle.getString("u2f-error-device-required"));

				return STATE_ERROR;
			}

		case STATE_NEW_DEVICE:
			
			//FIXME: ScriptCallBacks do not seem to support button
			Integer cancelIndex = ((ConfirmationCallback) callbacks[2]).getSelectedIndex();
			
			String registrationResponse = ((HiddenValueCallback) callbacks[0]).getValue();

			try {
				getDataStore().verifyRegistrationResponse(getIdentity(),DataStoreHelper.deserialize(registrationResponse, RegistrationResponse.class));
			} catch (VerificationException e) {
				e.printStackTrace();
				setErrorHeader(bundle.getString("u2f-error-register"));
				return STATE_ERROR;
			}
			
			return getStateManageDevices(bundle.getString("u2f-register-new-device"));

		case STATE_LOGIN:
			
			String signResponse = ((HiddenValueCallback) callbacks[0]).getValue();
			
			try {
				getDataStore().verifySignResponse(getIdentity(),DataStoreHelper.deserialize(signResponse, SignResponse.class));
				
			} catch (VerificationException e) {
				setErrorHeader(bundle.getString("u2f-error-login"));
				return STATE_ERROR;
			}
			
			String manage = ((HiddenValueCallback) callbacks[3]).getValue();

			if (("yes".equalsIgnoreCase(manage) || isManageOnly()) && isManageAllowed()) {
				return getStateManageDevices(bundle.getString("u2f-sign-login-succesfull"));
			}
			
			return ISAuthConstants.LOGIN_SUCCEED;

		case STATE_MANAGE_DEVICES:
			
			Integer selectedIndex = ((ConfirmationCallback) callbacks[2]).getSelectedIndex();
			
			switch (selectedIndex) {
			case 0:
				
				int[] devicesSelected = ((ChoiceCallback) callbacks[1]).getSelectedIndexes();
				
				Set<DataStoreElement> registrationData = getDataStore().getRegistrationData(getIdentity());
				Iterator<DataStoreElement> iterator = registrationData.iterator();
				
				for (Integer i = 0; iterator.hasNext(); i++) {
					DataStoreElement dataStoreElement = iterator.next();

					if (ArrayUtils.contains(devicesSelected, i)) {
						iterator.remove();
						
						getDataStore().removeRegistrationData(getIdentity(),dataStoreElement);
					}
				}
				
				return getStateManageDevices(bundle.getString("u2f-register-removed-device"));
			case 1:
				
				replaceCallback(STATE_NEW_DEVICE, 1, createRegisterScriptCallback(getIdentity()));

				return STATE_NEW_DEVICE;
			default:
				return ISAuthConstants.LOGIN_SUCCEED;
			}

		case STATE_ERROR:

			return STATE_ERROR;
		default:
			throw new AuthLoginException("invalid state");
		}
	}

	/**
	 * For session upgrading, it is essential to return the same username
	 */
	@Override
	public Principal getPrincipal() {
		return new U2FPrincipal(username);
	}

	private void setErrorHeader(String header) throws AuthLoginException {
		substituteHeader(STATE_ERROR, header);
	}

	private Callback createSignScriptCallback(AMIdentity amIdentity) {
		
		String script = getLoginJavaScript(getDataStore().generateSignSessionData(getIdentity()));
		
		return new ScriptTextOutputCallback(script);
	}

	private Callback createRegisterScriptCallback(AMIdentity identity) {

		String script = getRegistrationJavaScript(getDataStore().generateRegistrationSessionData(getIdentity()));

		return new ScriptTextOutputCallback(script);
	}

	private void substituteUIStrings() throws AuthLoginException {
		substituteHeader(STATE_LOGIN, bundle.getString("u2f-header"));
		substituteHeader(STATE_MANAGE_DEVICES, "Manage your devices");
		substituteHeader(STATE_NEW_DEVICE, bundle.getString("u2fregister-header"));
	}

}
