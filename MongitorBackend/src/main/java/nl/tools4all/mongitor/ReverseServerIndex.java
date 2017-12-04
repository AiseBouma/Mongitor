package nl.tools4all.mongitor;
import java.util.HashMap;

/**
 * 
 */

/**
 * @author supabouma
 *
 */
public class ReverseServerIndex
{
  private static HashMap<String, MongoConfigServer> configservers = new HashMap<String, MongoConfigServer>();
  private static HashMap<String, MongoShardServer> shardservers = new HashMap<String, MongoShardServer>();
  
  private ReverseServerIndex()
  {
  }
  
  public static void clear()
  {
    configservers.clear();
    shardservers.clear();
  }  
  
  public static void addConfigServer(String id, MongoConfigServer mongoConfigServer)
  {
    configservers.put(id, mongoConfigServer);
  }
  
  public static void addShardServer(String id, MongoShardServer mongoShardServer)
  {
    shardservers.put(id, mongoShardServer);
  }
  
  public static MongoConfigServer getConfigServer(String id)
  {
    return configservers.get(id);
  }
  
  public static MongoShardServer getShardServer(String id)
  {
    return shardservers.get(id);
  }

}
