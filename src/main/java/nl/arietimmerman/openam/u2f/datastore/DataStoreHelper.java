/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/
package nl.arietimmerman.openam.u2f.datastore;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.commons.codec.binary.Base64;

import com.owlike.genson.Context;
import com.owlike.genson.Converter;
import com.owlike.genson.GenericType;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import com.owlike.genson.stream.ObjectReader;
import com.owlike.genson.stream.ObjectWriter;

public class DataStoreHelper {

	private final static Genson genson = new GensonBuilder().useConstructorWithArguments(true).withConverter(new ByteConverter(), byte[].class).withConverter(new X509Converter(), X509Certificate.class).create();

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
