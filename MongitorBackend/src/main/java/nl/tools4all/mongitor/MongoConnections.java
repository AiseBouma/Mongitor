package nl.tools4all.mongitor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

/**
 * 
 */

/**
 * @author supabouma
 *
 */
public class MongoConnections
{
  private static HashMap<String, MongoClient> connectionsMap = new HashMap<String, MongoClient>();
  private static MongoClientOptions mongoClientOptions = MongoClientOptions.builder().connectTimeout(5000).serverSelectionTimeout(5000).build();
  
  private MongoConnections()
  {
  }
  
  public static MongoClient getConnection(String host, int port)
  {
    MongoClient mongoClient = connectionsMap.get(host);
    if (mongoClient == null)
    {
      if ("OK".equals(setupConnection(host, port)))
      {
        mongoClient = connectionsMap.get(host); 
      }
    }
    return mongoClient;
  }
  
  public static MongoClient getConnection(String host)
  {
    return connectionsMap.get(host);
  }
  
  public static String setupConnection(String host, int port)
  {
    String result = "OK";
    
    if (!connectionsMap.containsKey(host))
    {
      // first try with default credentials
      MongoCredential credential = MongoCredentials.getDefaultCredentials("admin");
      if (!"OK".equals(testConnection(host, port, credential)))
      {
        // try the other credentials
        List<MongoCredential> credentialsList = MongoCredentials.getCredentialsList("admin");
        int i = 0;
        boolean found = false;
        while (i < credentialsList.size() && !found)
        {
          found = "OK".equals(testConnection(host, port, credentialsList.get(i)));
          i++;
        }
        if (!found)
        {
          result = "Could not connect to " + host;
        }
      }  
    }
    
    return result;
  }  
  
  public static String testConnection(String host, int port, MongoCredential credential)
  {
    String result = "OK";
    
    try
    {
      MongoClient mongoClient = new MongoClient(new ServerAddress(host, port), Arrays.asList(credential), mongoClientOptions);
      MongoDatabase mongoDatabase = mongoClient.getDatabase("admin");
      Document statusResults = mongoDatabase.runCommand(new Document("serverStatus", 1));
      if ((statusResults == null) || (statusResults.getDouble("ok") != 1.0))
      {
        result = "Could not get status of " + host;
      }
      else
      {  
        connectionsMap.put(host, mongoClient);
      }  
    }
    catch (Exception e)
    {
      e.printStackTrace(Debug.getPrintStream());
      result = "Connection failed";
    }
    return result;
  }
  
  public static CommandResult runCommand(String server, String command)
  {
    CommandResult commandResult = null;
    
    if ("printReplicationInfo".equals(command))
    {
      commandResult = OplogCommands.replicationInfo(server);
    }
    else
    {  
      MongoClient mongoClient = connectionsMap.get(server);
      if (mongoClient == null)
      {
        commandResult = new CommandResult("No connection to " + server);
      }
      else
      {
        MongoDatabase mongoDatabase = mongoClient.getDatabase("admin");
        Document commandResults = mongoDatabase.runCommand(new Document(command, 1));
        if ((commandResults == null) || (commandResults.getDouble("ok") != 1.0))
        {
          commandResult = new CommandResult("Command on " + server  + " failed");
        } 
        else
        {
          commandResult = new CommandResult("OK", commandResults.toJson());
        }
      }  
    }
    return commandResult;
  }
  
  
  public static void closeAllConnections()
  {
    for(Map.Entry<String, MongoClient> entry : connectionsMap.entrySet())
    {
      MongoClient client = entry.getValue();
      if (client != null)
      {
        try
        {
          client.close();
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }
    connectionsMap.clear();
  }
}
