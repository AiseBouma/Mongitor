package nl.tools4all.mongitor;

public class MongoShard
{
  private String id;
  private MongoShardServer[] shardServers =  new MongoShardServer[] {};
  
  /**
   * @return the id
   */
  public String getId()
  {
    return id;
  }
  /**
   * @param id the id to set
   */
  public void setId(String id)
  {
    this.id = id;
  }
  /**
   * @return the shardServers
   */
  public MongoShardServer[] getShardServers()
  {
    return shardServers;
  }
  /**
   * @param shardServers the shardServers to set
   */
  public void setShardServers(MongoShardServer[] shardServers)
  {
    this.shardServers = shardServers;
  }
  


}
