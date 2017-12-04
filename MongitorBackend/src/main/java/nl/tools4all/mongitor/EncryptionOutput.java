package nl.tools4all.mongitor;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;

/**
 * 
 */

/**
 * @author Aise
 *
 */
public class EncryptionOutput
{
  private byte[] initializationVector;
  private byte[] cipherText;

  public EncryptionOutput()
  {
  }

  /**
   * @param initializationVector
   * @param cipherText
   */
  public EncryptionOutput(byte[] initializationVector, byte[] cipherText)
  {
    this.initializationVector = initializationVector;
    this.cipherText = cipherText;
  }

  public String writeToFile(String filename)
  {
    try
    {
      FileWriter writer = new FileWriter(filename);
      byte[] encodedBytes = Base64.getEncoder().encode(initializationVector);
      writer.write(new String(encodedBytes));
      writer.write("\n");
      encodedBytes = Base64.getEncoder().encode(cipherText);
      writer.write(new String(encodedBytes));
      writer.write("\n");
      writer.close();
    }
    catch (IOException e)
    {
      return e.getMessage();
    }

    return "OK";
  }

  public static EncryptionOutput readFromFile(String filename)
  {
    EncryptionOutput encryptionOutput = null;
    byte[] initializationVector;
    byte[] cipherText;

    try
    {
      FileReader reader = new FileReader(filename);
      BufferedReader bufferedReader = new BufferedReader(reader);

      String line;

      line = bufferedReader.readLine();
      if (line != null)
      {
        initializationVector = Base64.getDecoder().decode(line);
        line = bufferedReader.readLine();
        if (line != null)
        {
          cipherText = Base64.getDecoder().decode(line);
          encryptionOutput = new EncryptionOutput(initializationVector, cipherText);
        }
      }

      bufferedReader.close();
      reader.close();

    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    return encryptionOutput;
  }

  /**
   * @return the initializationVector
   */
  public byte[] getInitializationVector()
  {
    return initializationVector;
  }

  /**
   * @param initializationVector
   *          the initializationVector to set
   */
  public void setInitializationVector(byte[] initializationVector)
  {
    this.initializationVector = initializationVector;
  }

  /**
   * @return the cipherText
   */
  public byte[] getCipherText()
  {
    return cipherText;
  }

  /**
   * @param cipherText
   *          the cipherText to set
   */
  public void setCipherText(byte[] cipherText)
  {
    this.cipherText = cipherText;
  }

}
