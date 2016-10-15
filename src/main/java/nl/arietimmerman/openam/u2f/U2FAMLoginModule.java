/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/
package nl.arietimmerman.openam.u2f;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.Subject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import nl.arietimmerman.openam.u2f.datastore.DataStore;
import nl.arietimmerman.openam.u2f.datastore.DataStoreHelper;
import nl.arietimmerman.openam.u2f.datastore.LocalDataStore;
import nl.arietimmerman.openam.u2f.datastore.MemoryDataStore;
import nl.arietimmerman.u2f.server.message.RegistrationSessionData;
import nl.arietimmerman.u2f.server.message.SignSessionData;

import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

abstract class U2FAMLoginModule extends AMLoginModule {

	// Name for the debug-log
	protected final static String DEBUG_NAME = "U2F";
	protected final static Debug debug = Debug.getInstance(DEBUG_NAME);

	@SuppressWarnings("rawtypes")
	protected Map options;
	protected ResourceBundle bundle;
	protected Map sharedState;

	// Name of the resource bundle and bundle keys
	private final static String bundleName = "u2f";
	private final static String KEY_APP_ID = "nl-arietimmerman-openam-u2f-appid";
	private final static String KEY_TRUSTED_CERTIFICATES = "nl-arietimmerman-openam-u2f-trustedCertificates";
	private final static String KEY_TRUST_ALL = "nl-arietimmerman-openam-u2f-verify";
	private final static String KEY_USAGE = "nl-arietimmerman-openam-u2f-usage";
	private final static String KEY_DATASTORE = "nl-arietimmerman-openam-u2f-datastore";
	private final static String KEY_GOOGLEAPI = "nl-arietimmerman-openam-u2f-googleapi";

	private final static String DATASTORE_MEMORY = "memory";
	private final static String DATASTORE_LOCAL = "local";

	// Javascript resource locations
	private final static String u2fAPIResource = "/nl/arietimmerman/openam/u2f/u2f-api.js";
	private final static String u2fAPISignResource = "/nl/arietimmerman/openam/u2f/sign.js";
	private final static String u2fAPIRegisterResouce = "/nl/arietimmerman/openam/u2f/register.js";

	private DataStore datastore = null;

	@SuppressWarnings("rawtypes")
	@Override
	public void init(Subject subject, Map sharedState, Map options) {
		this.options = options;
		this.sharedState = sharedState;

		bundle = amCache.getResBundle(bundleName, getLoginLocale());

		if (DATASTORE_MEMORY.equals(CollectionHelper.getMapAttr(options, KEY_DATASTORE))) {
			datastore = new MemoryDataStore(getAppId(), getTrustedCertificates(), getTrustAll());
		} else {
			datastore = new LocalDataStore(getAppId(), getTrustedCertificates(), getTrustAll());
		}

	}

	public SSOToken getToken() {

		SSOToken token = null;
		try {
			SSOTokenManager tokenManager = SSOTokenManager.getInstance();

			InternalSession session = getLoginState("u2fregister").getOldSession();

			if (session != null) {
				token = tokenManager.createSSOToken(session.getID().toString());
			}
		} catch (SSOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthLoginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return token;
	}
	
	/**
	 * Returns an {@link AMIdentity} object of the logged in user. 
	 * @return
	 */
	public AMIdentity getIdentity() {

		String userName = null;
		AMIdentity identity = null;

		SSOToken token = getToken();

		if (token != null) {
			try {
				userName = token.getProperty("UserToken");
			} catch (SSOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			userName = (String) sharedState.get(getUserKey());
		}

		if (userName != null) {
			identity = getIdentity(userName);
		}

		return identity;
	}

	/**
	 * Returns an {@link AMIdentity} object based on the userName
	 * @param userName
	 * @return
	 */
	public AMIdentity getIdentity(String userName) {
		AMIdentity amIdentity = null;
		AMIdentityRepository amIdRepo = getAMIdentityRepository(getRequestOrg());

		IdSearchControl idsc = new IdSearchControl();
		idsc.setAllReturnAttributes(true);
		Set<AMIdentity> results = Collections.emptySet();

		try {
			idsc.setMaxResults(0);
			IdSearchResults searchResults = amIdRepo.searchIdentities(IdType.USER, userName, idsc);
			if (searchResults != null) {
				results = searchResults.getSearchResults();
			}

			if (results.isEmpty()) {
				debug.error("DevicePrintModule.getIdentity : User " + userName + " is not found");
			} else if (results.size() > 1) {
				debug.error("DevicePrintModule.getIdentity : More than one user found for the userName " + userName);
			} else {
				amIdentity = results.iterator().next();
			}

		} catch (IdRepoException e) {
			debug.error("DevicePrintModule.getIdentity : Error searching Identities with username : " + userName, e);
		} catch (SSOException e) {
			debug.error("DevicePrintModule.getIdentity : Module exception : ", e);
		}

		return amIdentity;
	}

	/**
	 * Returns a listed of trusted attestation certificates.
	 * @return
	 */
	private Set<X509Certificate> getTrustedCertificates() {
		Set<X509Certificate> trustedCertificates = new HashSet<X509Certificate>();

		String trustedCertificatesRaw = StringUtils.defaultString(CollectionHelper.getMapAttr(options, KEY_TRUSTED_CERTIFICATES));

		Matcher matcher = Pattern.compile("-----BEGIN CERTIFICATE-----(.*?)-----END CERTIFICATE-----", Pattern.MULTILINE | Pattern.DOTALL).matcher(trustedCertificatesRaw);

		while (matcher.find()) {
			String match = matcher.group(1);

			try {
				X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(Base64.decodeBase64(match)));
				trustedCertificates.add(certificate);
			} catch (CertificateException e) {
				e.printStackTrace();
			}

		}

		return trustedCertificates;
	}
	
	/**
	 * Returns whether or not attestation certificates should be checked, or if all attestation certificates - and thus all U2F devices - are allowed.
	 * @return
	 */
	private Boolean getTrustAll() {
		return Boolean.valueOf(CollectionHelper.getMapAttr(options, KEY_TRUST_ALL));
	}
	
	/**
	 * Check whether or not to include Google's u2f-api.js, see https://github.com/google/u2f-ref-code/blob/master/u2f-gae-demo/war/js/u2f-api.js
	 * @return
	 */
	private Boolean getUseGoogleApi(){
		return Boolean.valueOf(CollectionHelper.getMapAttr(options, KEY_GOOGLEAPI));
	}
	
	protected DataStore getDataStore() {
		return this.datastore;
	}

	/**
	 * Returns the JavaScript used to communicate with the U2F device for signin in
	 * @param signData
	 * @return
	 */
	protected String getLoginJavaScript(HashMap<String, SignSessionData> signData) {

		String script = String.format("var incomingSignData = %s;", DataStoreHelper.serialize(signData));

		try {
			InputStream inputStream = null;
			
			if(getUseGoogleApi()){
				inputStream = U2FAMLoginModule.class.getResourceAsStream(u2fAPIResource);
				script += "\n" + IOUtils.toString(inputStream, "UTF-8");
			}
			
			inputStream = this.getClass().getResourceAsStream(u2fAPISignResource);
			script += "\n" + IOUtils.toString(inputStream, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}

		return script;

	}

	/**
	 * Returns the JavaScript used to communicate with the U2F device for registering
	 * @param registrationSessionData
	 * @return
	 */
	protected String getRegistrationJavaScript(RegistrationSessionData registrationSessionData) {

		String script = String.format("var registrationData = %s;", DataStoreHelper.serialize(registrationSessionData));

		try {
			InputStream inputStream = null;
			
			if(getUseGoogleApi()){
				inputStream = this.getClass().getResourceAsStream(u2fAPIResource);
				script += "\n" + IOUtils.toString(inputStream, "UTF-8");
			}
			
			inputStream = this.getClass().getResourceAsStream(u2fAPIRegisterResouce);
			script += "\n" + IOUtils.toString(inputStream, "UTF-8");
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		return script;
	}

	/**
	 * Returns the configured Application Identifier
	 * @return
	 */
	protected String getAppId() {
		return CollectionHelper.getMapAttr(options, KEY_APP_ID);
	}

	/**
	 * Returns whether or not this plugin is configured to allow management of devices ONLY, and thus login is not allowed
	 * @return
	 */
	protected Boolean isManageOnly() {
		return StringUtils.equalsIgnoreCase(CollectionHelper.getMapAttr(options, KEY_USAGE), "manage");
	}

	/**
	 * Returns whether or not this plugin is configured to allow management of devices
	 * @return
	 */
	protected Boolean isManageAllowed() {
		return StringUtils.containsIgnoreCase(CollectionHelper.getMapAttr(options, KEY_USAGE), "manage");
	}

	/**
	 * Returns whether or not this plugin is configured to allow log in
	 * @return
	 */
	protected Boolean isLoginAllowed() {
		return isManageOnly() || StringUtils.containsIgnoreCase(CollectionHelper.getMapAttr(options, KEY_USAGE), "login");
	}

}
