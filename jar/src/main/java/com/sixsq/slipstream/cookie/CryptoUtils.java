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
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.logging.Logger;

/**
 * Utilities for handling signing and validating signatures of strings. Used
 * mainly for cookie validation.
 * 
 * @author loomis
 * 
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
	// signed by a recognized external authority. The key pair will be
	// regenerated when the service restarts invalidating all existing cookies.
	// This is probably the correct behavior.
	public static final PrivateKey privateKey;
	public static final PublicKey publicKey;
	static {
		try {

			// Create a key pair using the named algorithm.
			KeyPairGenerator generator = KeyPairGenerator
					.getInstance(keyPairAlgorithm);
			generator.initialize(keySize);
			KeyPair keyPair = generator.generateKeyPair();

			// Strip out the public and private key and set static fields.
			publicKey = keyPair.getPublic();
			privateKey = keyPair.getPrivate();

		} catch (NoSuchAlgorithmException nsae) {
			throw new RuntimeException(nsae.getMessage());
		}
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
