// Copyright 2014 Google Inc. All rights reserved.
// Copyright 2015 Arie Timmerman. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package nl.arietimmerman.u2f.server;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

/**
 * Helps to verify signatures present in all FIDO U2F responses
 */
public class CryptoHelper {

	private static final Logger Log = Logger.getLogger(CryptoHelper.class.getName());
	
	private CryptoHelper() {
		
	}
	
	/**
	 * Verifies the signature, as set on the registration response and authentication response messages
	 * @param publicKey The public key
	 * @param bytesSigned The bytes to be signed
	 * @param signature See "FIDO U2F Raw Message Formats" for requirements regarding signatures
	 * @return
	 */
	public static boolean verifySignature(PublicKey publicKey, byte[] bytesSigned, byte[] signature)  {
		Log.info("start verifySignature");
		try {
			Signature signatureObject = Signature.getInstance("SHA256withECDSA", new BouncyCastleProvider());
			Log.info("Initialized SHA256withECDSA");
			signatureObject.initVerify(publicKey);
			Log.info("executed initVerify");
			signatureObject.update(bytesSigned);
			Log.info("start verifying");
			return signatureObject.verify(signature);
		} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static byte[] sign(PrivateKey privateKey, byte[] message)  {
		
		try {
			Signature signatureObject = Signature.getInstance("SHA256withECDSA", new BouncyCastleProvider());
			signatureObject.initSign(privateKey);
			signatureObject.update(message);
			return signatureObject.sign();
		} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	/**
	 * 
	 * @param encodedPublicKey This is the (uncompressed) x,y-representation of a curve point on the P-256 NIST elliptic curve.
	 * @return
	 */
	public static PublicKey decodePublicKey(byte[] encodedPublicKey) {
		PublicKey result = null;
		
		try {
			
			X9ECParameters curve = SECNamedCurves.getByName("secp256r1");
			ECPoint point = curve.getCurve().decodePoint(encodedPublicKey);
			
			result = KeyFactory.getInstance("ECDSA",new BouncyCastleProvider()).generatePublic(new ECPublicKeySpec(point, new ECParameterSpec(curve.getCurve(), curve.getG(), curve.getN(), curve.getH())));
			
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
				e.printStackTrace();
		}
		
		return result;
	}
	
	public static byte[] sha256(byte[] bytes)  {
		return DigestUtils.sha256(bytes);
	}
}
