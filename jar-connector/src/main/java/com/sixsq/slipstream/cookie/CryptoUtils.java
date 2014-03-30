package com.sixsq.slipstream.cookie;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
 * =====
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -=================================================================-
 */

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;

import com.sixsq.slipstream.persistence.CookieKeyPair;

/**
 * Utilities for handling signing and validating signatures of strings. Used
 * mainly for cookie validation.
 */
public class CryptoUtils {

	protected static final Logger log = Logger.getLogger(CryptoUtils.class
			.toString());

	// Parameter to use for generating keys and signing cookies.
	public static final int keySize = 512;
	public static final String keyPairAlgorithm = "RSA";
	public static final String signatureAlgorithm = "SHA1withRSA";

	// The radix used to transform a signature into a readable/serializable
	// form.
	public static final int radix = Character.MAX_RADIX;

	// Generate a key pair to use for cookie signing and validation. As this is
	// entirely internal to the application there is no need to have a pair
	// signed by a recognized external authority. To avoid the key pair from
	// being invalidating on server restart, store it on the db.
	private static PrivateKey privateKey;
	private static PublicKey publicKey;
	static {
		try {
			setKeyPairFromDb();
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e.getMessage());
		} catch (CertificateException e) {
			throw new RuntimeException(e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e.getMessage());
		}

		if (!isKeyPairSet()) {
			// Create a key pair using the named algorithm.
			KeyPairGenerator generator = null;
			try {
				generator = KeyPairGenerator.getInstance(keyPairAlgorithm);
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e.getMessage());
			}
			generator.initialize(keySize);
			KeyPair keyPair = generator.generateKeyPair();

			// Extract the public and private keys and set static fields.
			publicKey = keyPair.getPublic();
			privateKey = keyPair.getPrivate();

			// Store the key pair in the db
			CookieKeyPair ckp = null;
			try {
				ckp = new CookieKeyPair(savePrivateKey(privateKey),
						savePublicKey(publicKey));
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e.getMessage());
			}

			ckp.store();
		}
	}

	static private String savePrivateKey(PrivateKey privateKey)
			throws GeneralSecurityException {
		KeyFactory fact = KeyFactory.getInstance(keyPairAlgorithm);
		PKCS8EncodedKeySpec spec = fact.getKeySpec(privateKey,
				PKCS8EncodedKeySpec.class);
		byte[] encoded = spec.getEncoded();
		String privateKeyStr = new Base64().encodeToString(encoded);
		return privateKeyStr;
	}

	static private String savePublicKey(PublicKey publicKey)
			throws GeneralSecurityException {
		byte[] publicKeyBytes = publicKey.getEncoded();
		String publicKeyStr = new Base64().encodeToString(publicKeyBytes);
		return publicKeyStr;
	}

	static private void setKeyPairFromDb() throws NoSuchAlgorithmException,
			InvalidKeySpecException, CertificateException {
		CookieKeyPair ckp = CookieKeyPair.load();
		if (ckp == null) {
			return;
		}
		String privateKeyBase64 = ckp.getPrivateKey();
		String publicKeyBase64 = ckp.getPublicKey();
		if (privateKeyBase64 == null || publicKeyBase64 == null) {
			return;
		}

		byte[] privateKeyBytes = new Base64().decode(privateKeyBase64);
		KeyFactory keyFactory = KeyFactory.getInstance(keyPairAlgorithm);
		KeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
		privateKey = keyFactory.generatePrivate(privateKeySpec);

		byte[] publicKeyBytes = new Base64().decode(publicKeyBase64);
		X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyBytes);
		keyFactory = KeyFactory.getInstance(keyPairAlgorithm);
		publicKey = keyFactory.generatePublic(x509KeySpec);
	}

	private static boolean isKeyPairSet() {
		return privateKey != null && publicKey != null;
	}

	// Actually try to generate a signature so that the failure is caught during
	// initialization rather than after the service has started.
	static {
		try {
			Signature signature = Signature.getInstance(signatureAlgorithm);
			signature.initSign(privateKey);
			signature.initVerify(publicKey);
		} catch (NoSuchAlgorithmException nsae) {
			throw new RuntimeException(nsae.getMessage());
		} catch (InvalidKeyException ike) {
			throw new RuntimeException(ike.getMessage());
		}
	}

	/**
	 * Sign the given data and return a String representation of the signature.
	 * The argument may not be null.
	 *
	 * @param data
	 *            information to sign
	 *
	 * @return String representation of the signature.
	 */
	public static String sign(String data) {

		try {

			Signature signature = Signature.getInstance(signatureAlgorithm);
			signature.initSign(privateKey);

			signature.update(data.getBytes());
			BigInteger biSignature = new BigInteger(signature.sign());
			return biSignature.toString(radix);

		} catch (NoSuchAlgorithmException nsae) {
			return null;
		} catch (InvalidKeyException ike) {
			return null;
		} catch (SignatureException se) {
			return null;
		}
	}

	/**
	 * Determine if the given signature matches the given data.
	 *
	 * @param signed
	 *            String representation of signature
	 * @param data
	 *            information to check
	 *
	 * @return true if the signature matches the given data, false otherwise
	 */
	public static boolean verify(String signed, String data) {

		boolean valid = false;

		try {

			Signature signature = Signature.getInstance(signatureAlgorithm);
			signature.initVerify(publicKey);

			signature.update(data.getBytes());

			byte[] signBytes = (new BigInteger(signed, radix)).toByteArray();
			valid = signature.verify(signBytes);

		} catch (NoSuchAlgorithmException e) {
			log.warning("Algorithm not recognized: " + signatureAlgorithm
					+ " with details: " + e.getMessage());
		} catch (InvalidKeyException e) {
			log.warning(e.toString());
		} catch (SignatureException e) {
			log.warning(e.toString());
		}

		return valid;
	}

}
