package nl.tools4all.mongitor;
import java.util.ArrayList;
import java.util.Arrays;

import org.bson.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.UpdateResult;

public class MongoDBCluster
{
  private static MongoRouter mongoRouter = null;
  private static MongoConfigServer[] mongoConfigServers = new MongoConfigServer[] {};
  private static MongoShard[] mongoShards;
  private static boolean firstcheck = true;
  private static MongoClient mongoClient = null;
  private static boolean detected = false;

  private MongoDBCluster()
  {
  }

  public static String checkConnection(String routerhost, String routerport, String username, String password)
  {
    String result = "OK";
    MongoClient mongoClient = null;

    try
    {
      MongoCredential credential = MongoCredential.createCredential(username, "admin", password.toCharArray());
      MongoClientOptions mongoClientOptions = MongoClientOptions.builder().connectTimeout(5000)
          .serverSelectionTimeout(5000).build();
      mongoClient = new MongoClient(new ServerAddress(routerhost, 27017), Arrays.asList(credential),
          mongoClientOptions);
      MongoDatabase mongoDatabase = mongoClient.getDatabase("admin");
      mongoDatabase.runCommand(new Document("isMaster", 1));

      // add credentials to credentials file
      MongoCredentials.addCredentials("default", username + "@" + routerhost + ":" + routerport, password);
    }
    catch (Exception e)
    {
      e.printStackTrace(Debug.getPrintStream());
      result = "Connection failed";
    } finally
    {
      if (mongoClient != null)
      {
        try
        {
          mongoClient.close();
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }

    return result;
  }

  public static CommandResult listDatabases()
  {
    CommandResult commandResult = null;
    try
    {
      MongoDatabase mongoDatabase = mongoClient.getDatabase("admin");
      Document listResults = mongoDatabase.runCommand(new Document("listDatabases", 1));
      if ((listResults == null) || (listResults.getDouble("ok") != 1.0))
      {
        commandResult = new CommandResult("Could not get databases");
      } 
      else
      {
        commandResult = new CommandResult("OK", listResults.toJson());
      }
    }
    catch (Exception e)
    {
      commandResult = new CommandResult("Exception while getting database list: " + e.getMessage()); 
    }
    return commandResult;
  }
  
  public static CommandResult databaseDetails(String database)
  {
    CommandResult commandResult = null;
    String json = "{ \"database\": \"" + database + "\",\"collections\":[";
    try
    {
      MongoDatabase mongoDatabase = mongoClient.getDatabase(database);
      MongoIterable <String> collections = mongoDatabase.listCollectionNames();
      boolean first = true;
      for (String collectionName: collections)
      {
        if (first)
        {
          first = false;
        }
        else
        {
          json = json + ",";
        }
        json = json + "\"" + collectionName + "\"";
      }
      json = json + "]";
      
      json = json + "}";
      commandResult = new CommandResult("OK", json);
    }
    catch (Exception e)
    {
      commandResult = new CommandResult("Exception while getting database details: " + e.getMessage()); 
    }
    return commandResult;
  }
  
  public static CommandResult shardDetails(String shard)
  {
    CommandResult commandResult = null;
    String json = "{ \"shard\": \"" + shard + "\",\"databases\":[";
    try
    {
      MongoDatabase mongoDatabase = mongoClient.getDatabase("config");
      MongoCollection<Document> collection = mongoDatabase.getCollection("databases");
      MongoCursor<Document> cursor = collection.find(new Document("primary", shard)).iterator();
      boolean first = true;
      while (cursor.hasNext())
      {
        if (first)
        {
          first = false;
        }
        else
        {
          json = json + ",";
        }
        Document document = cursor.next();
        json = json + "\"" + document.getString("_id") + "\"";
      }
      cursor.close();
     
      json = json + "],\"chunks\":";
      collection = mongoDatabase.getCollection("chunks");
      long chunks = collection.count(new Document("shard", shard));
      json = json + chunks;
      json = json + "}";
      commandResult = new CommandResult("OK", json);
    }
    catch (Exception e)
    {
      commandResult = new CommandResult("Exception while getting shard details: " + e.getMessage()); 
    }
    return commandResult;
  }
  
  public static CommandResult orphanChunkCount(String shard, String hostname, String collection_ns)
  {
    CommandResult commandResult = null;
    try
    {
      MongoDatabase mongoDatabase = mongoClient.getDatabase("config");
      MongoCollection<Document> collection = mongoDatabase.getCollection("chunks");
      long chunks = collection.count(new Document("$and", Arrays.asList(new Document("ns", collection_ns), new Document("shard", new Document("$ne", shard)))));
      String json = "{ \"shard\": \"" + shard + "\", \"hostname\": \"" + hostname + "\", \"collection\": \"" + collection_ns + "\", \"chunkscount\": " + chunks + "}";
      commandResult = new CommandResult("OK", json);
    }
    catch (Exception e)
    {
      commandResult = new CommandResult("Exception while counting chunks: " + e.getMessage()); 
    }
    return commandResult;
  }
  
  // on large collections with a lot of consecutive chunks running cleanupOrphaned takes to long, therefore 
  // cleanupOrphaned is run per chunk starting at the last chunk
  
  public static Document getChunkStart(String shard, String collection_ns, int index)
  {
    Document mindoc = null;
    MongoCursor<Document> cursor = null;
    try
    {
      MongoDatabase mongoDatabase = mongoClient.getDatabase("config");
      MongoCollection<Document> collection = mongoDatabase.getCollection("chunks");
      cursor = collection.find(new Document("$and", Arrays.asList(new Document("ns", collection_ns), new Document("shard", new Document("$ne", shard))))).sort(new Document("min", 1)).skip(index).iterator();
      Document document = cursor.next();
      mindoc = (Document) document.get("min");
      System.out.println(mindoc.toJson());
    }
    catch (Exception e)
    {  
      
    }
    if (cursor != null)
    {
      cursor.close();
    }
    return mindoc;
  }
  
  public static CommandResult getClusterInfo()
  {
    CommandResult commandResult = null;
    boolean enabled = false;
    boolean running = false;
    

    try
    {
      MongoDatabase mongoDatabase = mongoClient.getDatabase("config");
      MongoCollection<Document> collection = mongoDatabase.getCollection("settings");
      MongoCursor<Document> cursor = collection.find(new Document("_id", "balancer"))
                                               .limit(1)
                                               .iterator();
      if (!cursor.hasNext())
      {
        commandResult = new CommandResult("Balancer setting not found in config collection");
      }
      else
      {  
        Document document = cursor.next();
        enabled = !document.getBoolean("stopped");
      }
      cursor.close();
      collection = mongoDatabase.getCollection("locks");
      cursor = collection.find(new Document("_id", "balancer"))
                         .limit(1)
                         .iterator();
      if (cursor.hasNext())
      {
        Document document = cursor.next();
        running = (document.getInteger("state") > 0);
      }
      cursor.close();
      String json = "{ \"enabled\": " + enabled + ", \"running\": " + running + "}";
      commandResult = new CommandResult("OK", json);
    }
    catch (Exception e)
    {
      commandResult = new CommandResult("Exception while getting balancer details: " + e.getMessage()); 
    }
    return commandResult;
  }

  public static CommandResult getShardedCollections()
  {
    CommandResult commandResult = null;

    try
    {
      MongoDatabase mongoDatabase = mongoClient.getDatabase("config");
      MongoCollection<Document> collection = mongoDatabase.getCollection("chunks");
      MongoCursor<String> cursor = collection.distinct("ns", String.class).iterator();
      String json = "{ \"collections\": [";
      boolean first = true;
      while (cursor.hasNext())
      {
        if (first)
        {
          first = false;
        }
        else
        {
          json = json + ",";
        }
        json = json + "\"" + cursor.next() + "\"";
      }
      json = json + "]}";
      cursor.close();
      commandResult = new CommandResult("OK", json);
    }
    catch (Exception e)
    {
      commandResult = new CommandResult("Exception while getting sharded collections: " + e.getMessage()); 
    }
    return commandResult;
  }

  public static CommandResult setBalancerState(boolean running)
  {
    CommandResult commandResult = null;

    try
    {
      MongoDatabase mongoDatabase = mongoClient.getDatabase("config");
      MongoCollection<Document> collection = mongoDatabase.getCollection("settings");
      UpdateResult updateResult = collection.updateOne(new Document("_id", "balancer"), new Document("$set", new Document("stopped", !running)));
      if (!updateResult.wasAcknowledged())
      {  
        commandResult = new CommandResult("Setting balancer state was not acknowledged");
      }
      else
      {
        commandResult = new CommandResult("OK", "");
      }
    }
    catch (Exception e)
    {
      commandResult = new CommandResult("Exception while setting balancer state: " + e.getMessage()); 
    }
    return commandResult;
  }

  
  public static CommandResult collectionDetails(String database, String collection)
  {
    CommandResult commandResult = null;
    try
    {
      MongoDatabase mongoDatabase = mongoClient.getDatabase(database);
      Document statsResults = mongoDatabase.runCommand(new Document("collStats", collection));
      commandResult = new CommandResult("OK", statsResults.toJson());
    }
    catch (Exception e)
    {
      commandResult = new CommandResult("Exception while getting collection details: " + e.getMessage()); 
    }
    return commandResult;
  }
  
  public static String detectCluster()
  {
    mongoRouter = null;
    mongoConfigServers = new MongoConfigServer[] {};
    mongoShards = new MongoShard[] {};
    ReverseServerIndex.clear();

    String routerhost = MongoCredentials.getDefaultHost();
    String routerportstr = MongoCredentials.getDefaultPort();
    String username = MongoCredentials.getDefaultUsername();
    String password = MongoCredentials.getDefaultPassword();
    if ((routerhost == null) || (routerportstr == null) || (username == null) || (password == null))
    {
      return ("Data missing on Mongo Router");
    }

    int routerport;
    try
    {
      routerport = Integer.parseInt(routerportstr);
    }
    catch (Exception e)
    {
      return ("Port must be numerical");
    }

    String result = "OK";
    MongoDatabase mongoDatabase = null;
    Document isMasterResults = null;
    MongoCredential credential = null;
    MongoClientOptions mongoClientOptions;
    try
    {
      credential = MongoCredential.createCredential(username, "admin", password.toCharArray());
      mongoClientOptions = MongoClientOptions.builder().connectTimeout(5000).serverSelectionTimeout(5000).build();
      mongoClient = new MongoClient(new ServerAddress(routerhost, routerport), Arrays.asList(credential),
          mongoClientOptions);
      mongoDatabase = mongoClient.getDatabase("admin");
      isMasterResults = mongoDatabase.runCommand(new Document("isMaster", 1));
    }
    catch (Exception e)
    {
      e.printStackTrace(Debug.getPrintStream());
      result = "Connection to Mongo router failed";
    }

    if (result.equals("OK"))
    {
      String msg = isMasterResults.getString("msg");
      if (msg == null || !msg.contains("isdbgrid"))
      {
        result = "Not a Mongo Router";
      }
    }

    String configsvrConnectionString = null;

    if (result.equals("OK"))
    {
      mongoRouter = new MongoRouter(routerhost + ":" + routerport, routerhost, routerport);

      try
      {
        Document statusResults = mongoDatabase.runCommand(new Document("serverStatus", 1));
        if ((statusResults == null) || (statusResults.getDouble("ok") != 1.0))
        {
          result = "Could not get router server status";
        } else
        {
          mongoRouter.setConnected(true);
          Document shardingDoc = (Document) statusResults.get("sharding");
          if (shardingDoc == null)
          {
            result = "No shards found";
          } else
          {
            configsvrConnectionString = shardingDoc.getString("configsvrConnectionString");
            if (configsvrConnectionString == null)
            {
              result = "Config server connection string not found";
            }
          }
        }
      }
      catch (Exception e)
      {
        e.printStackTrace(Debug.getPrintStream());
        result = "Exception while detecting config servers";
      }
    }

    if (result.equals("OK"))
    {
      if (!configsvrConnectionString.startsWith("configReplSet/"))
      {
        result = "Invalid config server connection string";
      } else
      {
        String hosts = configsvrConnectionString.substring(14);
        String[] hosts_arr = hosts.split(",");
        for (String hostport : hosts_arr)
        {
          String[] hostPortArray = hostport.split(":");
          int port;
          try
          {
            port = Integer.parseInt(hostPortArray[1]);
          }
          catch (Exception e)
          {
            port = 27017;
          }
          String id = hostPortArray[0] + ":" + port;
          MongoConfigServer mongoConfigServer = new MongoConfigServer(id, hostPortArray[0],
              port);
          MongoConfigServer[] newArray = Arrays.copyOf(mongoConfigServers, mongoConfigServers.length + 1);
          newArray[mongoConfigServers.length] = mongoConfigServer;
          mongoConfigServers = newArray;
          newArray = null;
          ReverseServerIndex.addConfigServer(id, mongoConfigServer);
        }
      }
    }

    if (result.equals("OK"))
    {
      try
      {
        Document listResults = mongoDatabase.runCommand(new Document("listShards", 1));
        if ((listResults == null) || (listResults.getDouble("ok") != 1.0))
        {
          result = "Could not get shards";
        } else
        {
          Object shards_obj = listResults.get("shards");
          if (shards_obj == null || !(shards_obj instanceof ArrayList))
          {
            result = "Invalid shard list";
          } else
          {
            @SuppressWarnings("unchecked")
            ArrayList<Document> shards = (ArrayList<Document>) shards_obj;
            for (int i = 0; i < shards.size(); i++)
            {
              MongoShard mongoshard = new MongoShard();

              Document shard = shards.get(i);
              String id = shard.getString("_id");
              mongoshard.setId(id);
              String hostString = shard.getString("host");

              if (!hostString.startsWith(id))
              {
                result = "Invalid id in shard string";
              } else
              {
                String shardhosts = hostString.substring(id.length() + 1);
                String[] shardHostArray = shardhosts.split(",");
                for (String oneserver : shardHostArray)
                {
                  String[] hostPortArray = oneserver.split(":");
                  if (hostPortArray.length > 1)
                  {
                    int port;
                    try
                    {
                      port = Integer.parseInt(hostPortArray[1]);
                    }
                    catch (Exception e)
                    {
                      port = 27017;
                    }
                    MongoShardServer mongoShardServer = new MongoShardServer(oneserver, hostPortArray[0], port);
                    MongoShardServer[] newArray = Arrays.copyOf(mongoshard.getShardServers(),
                        mongoshard.getShardServers().length + 1);
                    newArray[mongoshard.getShardServers().length] = mongoShardServer;
                    mongoshard.setShardServers(newArray);
                    newArray = null;
                    ReverseServerIndex.addShardServer(oneserver, mongoShardServer);
                  }
                }
              }
              MongoShard[] newArray = Arrays.copyOf(mongoShards, mongoShards.length + 1);
              newArray[mongoShards.length] = mongoshard;
              mongoShards = newArray;
              newArray = null;
              mongoshard = null;
            }
          }
        }
      }
      catch (Exception e)
      {
        result = "Exception while detecting shards";
      }
    }
    detected = result.equals("OK");
    
    return result;
  }
  
  public static void closeConnections()
  {
    if (mongoClient != null)
    {
      try
      {
        mongoClient.close();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    MongoConnections.closeAllConnections();
    firstcheck = true;
    detected = false;
  }
  
  public static void resetCluster()
  {
    if (mongoRouter != null)
    {  
      mongoRouter.setConnected(false);
      mongoRouter.setMongitorstate(0);
    }
    
    for (MongoConfigServer configServer: mongoConfigServers)
    {
      configServer.reset();
    }
    for (MongoShard shard: mongoShards)
    {
      for (MongoShardServer shardServer: shard.getShardServers())
      {
        shardServer.reset();
      }
    }
  }
  
  public static String checkCluster()
  {
    if (!detected)
    {
      return "Cluster not yet discovered";
    }
    String result = "OK";
    int max_error_severity = 1; // 1 = green, 2 = orange, 3 = red
    WebsocketMessage websocketMessage;
    
    resetCluster();
    
    MongoDatabase mongoDatabase = null;
    Document isMasterResults = null;
    try
    {
      mongoDatabase = mongoClient.getDatabase("admin");
      isMasterResults = mongoDatabase.runCommand(new Document("isMaster", 1));
    }
    catch (Exception e)
    {
      e.printStackTrace(Debug.getPrintStream());
      result = "Connection to Mongo router failed";
      max_error_severity = 3;
    }

    if (result.equals("OK"))
    {
      String msg = isMasterResults.getString("msg");
      if (msg == null || !msg.contains("isdbgrid"))
      {
        result = "Not a Mongo Router";
        max_error_severity = 3;
      }  
      else
      {
        mongoRouter.setConnected(true);
      }
    }
    
    mongoRouter.setMongitorstate(max_error_severity);
    
    for (MongoConfigServer configServer: mongoConfigServers)
    {
      String thisresult = configServer.check();
      if (firstcheck)
      {  
        websocketMessage = new WebsocketMessage("updatecluster", "response", MongoDBCluster.getJson());
        websocketMessage.send();
      }  
      if (configServer.getMongitorstate() > max_error_severity)
      {
        max_error_severity = configServer.getMongitorstate();
        result = thisresult;
      }
    }
    for (MongoShard shard: mongoShards)
    {
      for (MongoShardServer shardServer: shard.getShardServers())
      {
        String thisresult = shardServer.check();
        if (firstcheck)
        {  
          websocketMessage = new WebsocketMessage("updatecluster", "response", MongoDBCluster.getJson());
          websocketMessage.send();
        }
        if (shardServer.getMongitorstate() > max_error_severity)
        {
          max_error_severity = shardServer.getMongitorstate();
          result = thisresult;
        }
      }  
    }
    firstcheck = false;
    return result;
  }
  
  public static CommandResult getClusterOverview()
  {
    CommandResult commandResult = null;
    String json = "{ \"configservers\": [";
    
    boolean first = true;
    for (MongoConfigServer configServer: mongoConfigServers)
    {
      commandResult = configServer.overview();
      if (!"OK".equals(commandResult.getStatus()))
      {
        return commandResult;
      }
      else
      {
        if (first)
        {
          first = false;
        }
        else
        {
          json = json + ",";
        }
        json = json + commandResult.getResult();
      }
    }
    json = json + "], \"shards\": [";
    boolean first_shard = true;
    for (MongoShard shard: mongoShards)
    {
      if (first_shard)
      {
        first_shard = false;
      }
      else
      {
        json = json + ",";
      }
      json = json + "{ \""+ shard.getId() + "\":[";
      boolean first_server = true;
      for (MongoShardServer shardServer: shard.getShardServers())
      {
        if (first_server)
        {
          first_server = false;
        }
        else
        {
          json = json + ",";
        }
        
        commandResult = shardServer.overview();
        if (!"OK".equals(commandResult.getStatus()))
        {
          return commandResult;
        }
        else
        {
          json = json + commandResult.getResult();
        }
      } 
      json = json + "]}";
    }
    json = json + "]}";
    return new CommandResult("OK", json);
  }
  
  public static String getJson()
  {
    String jsonString = "{ \"router\": ";
    try
    {
      jsonString = jsonString + Json.getObjectMapper().writeValueAsString(mongoRouter);
    }
    catch (JsonProcessingException e)
    {
      jsonString = jsonString + "{}";
      e.printStackTrace();
    }
    jsonString = jsonString + ", \"configservers\": ";
    try
    {
      jsonString = jsonString + Json.getObjectMapper().writeValueAsString(mongoConfigServers);
    }
    catch (JsonProcessingException e)
    {
      jsonString = jsonString + "[]";
      e.printStackTrace();
    }
    jsonString = jsonString + " , \"shards\": ";
    try
    {
      jsonString = jsonString + Json.getObjectMapper().writeValueAsString(mongoShards);
    }
    catch (JsonProcessingException e)
    {
      jsonString = jsonString + "[]";
      e.printStackTrace();
    }
    jsonString = jsonString + " }";
    return jsonString;
  }

  /**
   * @return the mongoRouter
   */
  public static MongoRouter getMongoRouter()
  {
    return mongoRouter;
  }

  /**
   * @param mongoRouter
   *          the mongoRouter to set
   */
  public static void setMongoRouter(MongoRouter mongoRouter)
  {
    MongoDBCluster.mongoRouter = mongoRouter;
  }

  /**
   * @return the mongoConfigServers
   */
  public static MongoConfigServer[] getMongoConfigServers()
  {
    return mongoConfigServers;
  }

  /**
   * @param mongoConfigServers
   *          the mongoConfigServers to set
   */
  public static void setMongoConfigServers(MongoConfigServer[] mongoConfigServers)
  {
    MongoDBCluster.mongoConfigServers = mongoConfigServers;
  }

  /**
   * @return the mongoShards
   */
  public static MongoShard[] getMongoShards()
  {
    return mongoShards;
  }

  /**
   * @param mongoShards
   *          the mongoShards to set
   */
  public static void setMongoShards(MongoShard[] mongoShards)
  {
    MongoDBCluster.mongoShards = mongoShards;
  }

}
