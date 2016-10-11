/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/
package nl.arietimmerman.openam.u2f.datastore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import nl.arietimmerman.u2f.server.message.ClientData;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import com.owlike.genson.Context;
import com.owlike.genson.Converter;
import com.owlike.genson.GenericType;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import com.owlike.genson.stream.JsonType;
import com.owlike.genson.stream.ObjectReader;
import com.owlike.genson.stream.ObjectWriter;

/**
 * Implements methods to serialize and deserialize objects to JSON objects represented as strings.  
 */
public class DataStoreHelper {

	private final static Genson genson = new GensonBuilder().useConstructorWithArguments(true).withConverter(new ClientDataConverter(), ClientData.class).withConverter(new ByteConverter(), byte[].class).withConverter(new X509Converter(), X509Certificate.class).create();

	public static class ByteConverter implements Converter<byte[]> {

		@Override
		public byte[] deserialize(ObjectReader objectReader, Context context) throws Exception {
			return Base64.decodeBase64(objectReader.valueAsString());
		}

		@Override
		public void serialize(byte[] bytes, ObjectWriter writer, Context context) throws Exception {
			writer.writeString(Base64.encodeBase64URLSafeString(bytes));
		}

	}
	
	public static class ClientDataConverter implements Converter<ClientData> {

		@Override
		public ClientData deserialize(ObjectReader objectReader, Context context) throws Exception {
			String base64 = objectReader.valueAsString();
			
			JSONObject json = new JSONObject(new String(Base64.decodeBase64(base64)));
			
			ClientData clientData = new ClientData();
			
			if(json.has(ClientData.CHALLENGE_PARAM)){
				clientData.setChallenge(Base64.decodeBase64(json.getString(ClientData.CHALLENGE_PARAM)));
			}
			
			if(json.has(ClientData.ORIGIN_PARAM)){
				clientData.setOrigin(json.getString(ClientData.ORIGIN_PARAM));
			}
			
			if(json.has(ClientData.TYPE_PARAM)){
				clientData.setTyp(json.getString(ClientData.TYPE_PARAM));
			}
			
			return clientData;
		}

		@Override
		public void serialize(ClientData object, ObjectWriter writer, Context ctx) throws Exception {
			writer.writeString(Base64.encodeBase64URLSafeString(String.format("{\"typ\":\"%s\",\"challenge\":\"%s\",\"origin\":\"%s\"}",object.getTyp(),DataStoreHelper.serializeString(object.getChallenge()),object.getOrigin()).getBytes()));
		}

	}

	public static class X509Converter implements Converter<X509Certificate> {
		
		@Override
		public X509Certificate deserialize(ObjectReader objectReader, Context context) throws Exception {
			return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(Base64.decodeBase64(objectReader.valueAsString())));
		}

		@Override
		public void serialize(X509Certificate certificate, ObjectWriter writer, Context context) throws Exception {
			writer.writeString(Base64.encodeBase64URLSafeString(certificate.getEncoded()));
		}

	}
	
	private static Genson getGenson() {
		return genson;
	}
	
	public static String serializeString(Object object){
		String temp = "[" + getGenson().serialize(object) + "]";
		String result = null;
		try {
			result = new JSONArray(temp).getString(0);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static String serialize(Object object){
		return getGenson().serialize(object);
	}
	
	public static <T> T deserialize(String serialized, Class<T> c){
		return getGenson().deserialize(serialized, c);
	}
	
	public static <T> T deserialize(String serialized, GenericType<T> c){
		return getGenson().deserialize(serialized, c);
	}
	
}
