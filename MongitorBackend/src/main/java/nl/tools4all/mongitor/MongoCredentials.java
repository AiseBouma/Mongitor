package nl.tools4all.mongitor;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.MongoCredential;

public class MongoCredentials
{
  private static HashMap<String, CredentialsPair> credentialsMap = null;
  private static String credentialsFilename;
  
  //no instances needed
  private MongoCredentials()
  {
  }
  
  private static String writeCredentialsFile()
  {
    String jsonString;
    try
    {
      jsonString = Json.getObjectMapper().writeValueAsString(credentialsMap);
    }
    catch (JsonProcessingException e)
    {
      return e.getMessage();
    }
    EncryptionOutput encryptionOutput = Encryption.encrypt(jsonString);
    if (encryptionOutput == null)
    {
      return "Could not encrypt credentials.";
    }  
    return encryptionOutput.writeToFile(credentialsFilename);
  }
  
  public static boolean fileExists()
  {
    File credentialsFile = new File(credentialsFilename);
    return credentialsFile.exists();
  }

  public static String credentialExists(String credential_id)
  {
    String result = "No credentials found";
    if (credentialsMap != null)
    {  
      if (credentialsMap.containsKey(credential_id))
      {
        result = "yes";
      }
      else
      {
        result = "no";
      }
    }
    return result;
  }
  
  /**
   * @param mongitorPassword the mongitorPassword to set
   */
  public static String setMongitorPassword(String mongitorPassword)
  {
    String result;
    
    result = Encryption.initialize(mongitorPassword);
    if ("OK".equals(result))
    {  
      credentialsMap = new HashMap<String, CredentialsPair>();
      result = writeCredentialsFile();
    }
    return result;
  }
  
  public static String checkMongitorPassword(String mongitorPassword)
  {
    Encryption.initialize(mongitorPassword);
    EncryptionOutput encryptionOutput = EncryptionOutput.readFromFile(credentialsFilename);
    if (encryptionOutput == null)
    {
      return "Could not read credentials file.";
    }
    if (!"OK".equals(Encryption.decrypt(encryptionOutput)))
    { 
      return "Wrong password.";
    }
    String plaintext = Encryption.getPlaintext();
    System.out.println("pt: "+ plaintext);
    try
    {
      credentialsMap = Json.getObjectMapper().readValue(plaintext, new TypeReference<HashMap<String,CredentialsPair>>() {});
    }
    catch (Exception e)
    {
      e.printStackTrace(Debug.getPrintStream());
      return e.getMessage();
    }  
    return "OK";
  }
  
  public static boolean addCredentials(String id, String username, String password)
  {
    try
    {
      System.out.println(Json.getObjectMapper().writeValueAsString(credentialsMap));
    }
    catch (JsonProcessingException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if (id == null || username == null || password == null)
    {
      return false;
    }
    credentialsMap.put(id, new CredentialsPair(username, password));
    try
    {
      System.out.println(Json.getObjectMapper().writeValueAsString(credentialsMap));
    }
    catch (JsonProcessingException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    writeCredentialsFile();
    return true;
  }
  
  @SuppressWarnings("unchecked")
  public static boolean updateOtherCredentials(String othercredentialsstring)
  {
    // save default credentials
    CredentialsPair credentialsPair = credentialsMap.get("default");
    
    // clear list
    credentialsMap.clear();
    try
    {
      System.out.println(Json.getObjectMapper().writeValueAsString(credentialsMap));
    }
    catch (JsonProcessingException e1)
    {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    
    // add default credentials
    credentialsMap.put("default", credentialsPair);
    
    // add the other credentials
    ArrayList<Object> othercredentials = null;
    try
    {
      if (othercredentialsstring != "")
      {  
        othercredentials = Json.getObjectMapper().readValue(othercredentialsstring, ArrayList.class);
  
        if (othercredentials != null)
        {  
          for (Object cred_obj: othercredentials)
          {
            LinkedHashMap<String, String> credhashmap = (LinkedHashMap<String, String>) cred_obj;
            String label = credhashmap.get("label");
            String username = credhashmap.get("username");
            String password = credhashmap.get("password");
            if (label == null || username == null || password == null)
            {
              return false;
            }
            credentialsMap.put(label, new CredentialsPair(username, password));
          }
        }  
  
        System.out.println(Json.getObjectMapper().writeValueAsString(credentialsMap));
      }  
    }
    catch (Exception e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    writeCredentialsFile();
    return true;
  }
  
  public static String listCredentials()
  {
    String jsonString = null;
    try
    {
      jsonString = Json.getObjectMapper().writeValueAsString(credentialsMap);
    }
    catch (JsonProcessingException e)
    {
      e.printStackTrace();
    }
    return jsonString;
  }
  
  /**
   * @param credentialsFilename the credentialsFilename to set
   */
  public static void setCredentialsFilename(String credentialsFilename)
  {
    MongoCredentials.credentialsFilename = credentialsFilename;
  }

  public static String getDefaultUsername()
  {
    CredentialsPair credentialsPair = credentialsMap.get("default");
    if (credentialsPair == null)
    {
      return null;
    }
    String userhostport = credentialsPair.getUsername();
    int lastampersand = userhostport.lastIndexOf("@");
    if (lastampersand < 1)
    {
      return null;
    }
    return userhostport.substring(0, lastampersand);
  }
  
  public static String getDefaultHost()
  {
    CredentialsPair credentialsPair = credentialsMap.get("default");
    if (credentialsPair == null)
    {
      return null;
    }
    String userhostport = credentialsPair.getUsername();
    int lastampersand = userhostport.lastIndexOf("@");
    if (lastampersand < 1)
    {
      return null;
    }
    int lastcolon = userhostport.lastIndexOf(":");
    if (lastcolon < 1)
    {
      return null;
    }
    if (lastcolon < lastampersand)
    {
      return null;
    }
    return userhostport.substring(lastampersand+1, lastcolon);
  }
  
  public static String getDefaultPort()
  {
    CredentialsPair credentialsPair = credentialsMap.get("default");
    if (credentialsPair == null)
    {
      return null;
    }
    String userhostport = credentialsPair.getUsername();
    int lastcolon = userhostport.lastIndexOf(":");
    if (lastcolon < 1)
    {
      return null;
    }
    return userhostport.substring(lastcolon+1);
  }

  public static String getDefaultPassword()
  {
    CredentialsPair credentialsPair = credentialsMap.get("default");
    if (credentialsPair == null)
    {
      return null;
    }
    return credentialsPair.getPassword();
  }
  
  public static List<MongoCredential> getCredentialsList(String dbname)
  {
    List<MongoCredential> credentialsList = new ArrayList<MongoCredential>();
    
    for (Map.Entry<String, CredentialsPair> entry : credentialsMap.entrySet()) 
    {
      MongoCredential mongoCredential;
      String key = entry.getKey();
      CredentialsPair credentailsPair = entry.getValue();
      if ("default".equals(key))
      {
        mongoCredential = MongoCredential.createCredential(getDefaultUsername(), dbname, credentailsPair.getPassword().toCharArray());
      }
      else
      {  
        mongoCredential = MongoCredential.createCredential(credentailsPair.getUsername(), dbname, credentailsPair.getPassword().toCharArray());
      }  
      credentialsList.add(mongoCredential);
      
    }
    return credentialsList;
  }
  
  public static MongoCredential getDefaultCredentials(String dbname)
  {
    return MongoCredential.createCredential(getDefaultUsername(), dbname, getDefaultPassword().toCharArray());
  }
  
}
