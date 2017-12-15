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
    //System.out.println(connectionsMap.toString());
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
    MongoClient mongoClient = null;
    
    try
    {
      mongoClient = new MongoClient(new ServerAddress(host, port), Arrays.asList(credential), mongoClientOptions);
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
    if ((mongoClient != null) && (!"OK".equals(result)))
    {
      try
      {
        mongoClient.close();
      }
      catch (Exception e)
      {
        
      }
    }
    return result;
  }
  
  public static CommandResult runCommand(String server, String command)
  {
    CommandResult commandResult = null;
    Document document = null;
    
    try
    {
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
          if ("getLogGlobal".equals(command))
          {
            document = new Document("getLog", "global");
          }
          else if ("getLogRs".equals(command))
          {
            document = new Document("getLog", "rs");
          }
          else if ("getLogStartup".equals(command))
          {
            document = new Document("getLog", "startupWarnings");
          }
          else
          {
            document = new Document(command, 1);
          }
          Document commandResults = mongoDatabase.runCommand(document);
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
    }
    catch (Exception e)
    {
      commandResult = new CommandResult("Exception while running command: " + e.getMessage());
    }
    return commandResult;
  }
  
  public static CommandResult orphanCleanup(String index, String shard, String server, String collection)
  {
    CommandResult commandResult = null;
    Document commandResults;
    int indexnr = -1;
    
    try
    {
      indexnr = Integer.parseInt(index);
    }
    catch (Exception e)
    {
      commandResult = new CommandResult("index must be an integer");
    }
    if (indexnr != -1)
    {
      Document mindoc = MongoDBCluster.getChunkStart(shard, collection, indexnr);
    
      try
      {
        MongoClient mongoClient = connectionsMap.get(server);
        if (mongoClient == null)
        {
          commandResult = new CommandResult("No connection to " + server);
        }
        else
        {
          MongoDatabase mongoDatabase = mongoClient.getDatabase("admin");
          Document commandDoc = new Document("cleanupOrphaned", collection);
          
          commandDoc.append("startingFromKey", mindoc);
            
          commandResults = mongoDatabase.runCommand(commandDoc);
          
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
      catch (Exception e)
      {
        commandResult = new CommandResult("Exception while cleaning: " + e.getMessage());
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
