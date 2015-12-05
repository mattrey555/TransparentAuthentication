package com.visibleautomation.verify;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.xmlpull.v1.XmlPullParser;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
/**
 * Servlet to process the verification request from login
 */
public class CipherUtil {
	public static class Token {
		private final long token;
	    private final String encryptedToken;

		public Token(long token, String encryptedToken) {
			this.token = token;
			this.encryptedToken = encryptedToken;
		}

		public long getToken() {
			return this.token;
		}

		public String getEncryptedToken() {
			return this.encryptedToken;
		}
    }

	public static Token getEncryptedToken(String publicKeyString) throws Exception {
		// generate and encode a random number to send to the client, which it will decrypt with
		// its private key, then request a verification from the handset.
		Random random = new Random();
		long randVal = random.nextLong();
		String encryptedToken = null;
		Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		byte[] keyBytes = Base64.getDecoder().decode(publicKeyString);
		PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
		c.init(Cipher.ENCRYPT_MODE, publicKey);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, c);
		cipherOutputStream.write(Long.toString(randVal).getBytes());
		cipherOutputStream.flush();
		cipherOutputStream.close();
		byte[] encryptedBytes = outputStream.toByteArray();
		System.out.println("base64 Encoded Encrypted bytes");
		for (int i = 0; i < encryptedBytes.length; i++) {
			System.out.print(String.format("%02X ", encryptedBytes[i]));
		}
		System.out.println("");
		encryptedToken = Base64.getEncoder().encodeToString(encryptedBytes);
	    return new Token(randVal, encryptedToken);
	}
} 
