package nl.tools4all.mongitor;
import java.security.AlgorithmParameters;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 
 */

/**
 * @author Aise
 *
 */
public class Encryption
{
  private static SecretKey secret = null;
  private static final byte[] salt = Base64.getDecoder().decode("3fa19b748ec05d26");
  private static String plaintext = null;
  
  private Encryption()
  {
  }
  
  public static String initialize(String password)
  {
    String result = "OK";
    /* Derive the key, given password and salt. */
    try
    {
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
      SecretKey tmp = factory.generateSecret(spec);
      secret = new SecretKeySpec(tmp.getEncoded(), "AES");
    }
    catch (Exception e)
    {
      result = e.getMessage();
    } 
    return result;
  }
  
  public static EncryptionOutput encrypt(String plaintext)
  {
    if (secret == null)
    {  
      return null;
    }  
   
    EncryptionOutput encryptionOutput = new EncryptionOutput();
    try
    {
      /* Encrypt the message. */
      Cipher cipher;
      cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, secret);
      AlgorithmParameters params = cipher.getParameters();
      encryptionOutput.setInitializationVector(params.getParameterSpec(IvParameterSpec.class).getIV());
      encryptionOutput.setCipherText(cipher.doFinal(plaintext.getBytes("UTF-8")));
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return null;
    }
    return encryptionOutput;
  }
  
  public static String decrypt(EncryptionOutput encryptionOutput)
  {
    try
    {
      /* Decrypt the message, given derived key and initialization vector. */
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(encryptionOutput.getInitializationVector()));
      plaintext = new String(cipher.doFinal(encryptionOutput.getCipherText()), "UTF-8");
    }
    catch (Exception e)
    {
      return e.getMessage();
    }
    return "OK";
  }

  /**
   * @return the plaintext
   */
  public static String getPlaintext()
  {
    return plaintext;
  }
}
