package nl.tools4all.mongitor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

public class MongoConfigServer extends MongoServer
{
  private boolean master = false;
  private String state = "";

  public MongoConfigServer(String id, String hostname, int port)
  {
    super(id, hostname, port);
  }

  /**
   * @return the master
   */
  public boolean isMaster()
  {
    return master;
  }

  /**
   * @param master
   *          the master to set
   */
  public void setMaster(boolean master)
  {
    this.master = master;
  }

  /**
   * @return the state
   */
  public String getState()
  {
    return state;
  }

  /**
   * @param state
   *          the state to set
   */
  public void setState(String state)
  {
    this.state = state;
  }

  public void reset()
  {
    mongitorstate = 0;
    connected = false;
    state = "?";
    master = false;
  }

  public String check()
  {
    String result = "OK";

    MongoClient mongoClient = null;
    MongoDatabase mongoDatabase = null;

    try
    {

      mongoClient = MongoConnections.getConnection(hostname, port);
      if (mongoClient == null)
      {
        mongitorstate = 3;
        return "Could not connect to " + hostname;
      }
      mongoDatabase = mongoClient.getDatabase("admin");
      Document statusResults = mongoDatabase.runCommand(new Document("serverStatus", 1));
      if ((statusResults == null) || (statusResults.getDouble("ok") != 1.0))
      {
        result = "Could not get status of " + id;
        mongitorstate = 3;
      }
      else
      {
        connected = true;

        mongitorstate = Math.max(1, mongitorstate);  // preserve existing warnings/errors
        Document repl_doc = (Document) statusResults.get("repl");
        if (repl_doc != null)
        {
          master = repl_doc.getBoolean("ismaster");
          if (master)
          {
            Document replResults = mongoDatabase.runCommand(new Document("replSetGetStatus", 1));
            if ((replResults == null) || (replResults.getDouble("ok") != 1.0))
            {
              result = "Could not get replication status via " + id;
              mongitorstate = 3;
            }
            else
            {
              @SuppressWarnings("unchecked")
              ArrayList<Document> replmembers = (ArrayList<Document>) replResults.get("members");
              if (replmembers == null)
              {
                result = "No replication members found via " + id;
                mongitorstate = 3;
              }
              else
              {
                long ts_primary = 0;
                for (Document memberdoc : replmembers)
                {
                  if (memberdoc.containsKey("self") && memberdoc.getBoolean("self"))
                  {
                    
                    ts_primary = memberdoc.getDate("optimeDate").toInstant().toEpochMilli();
                  }
                }  
                for (Document memberdoc2 : replmembers)
                {    
                  String name = memberdoc2.getString("name");
                  if (name == null)
                  {
                    result = "Replication members name not found via " + id;
                    mongitorstate = 3;
                  }
                  else
                  {
                    MongoConfigServer member = ReverseServerIndex.getConfigServer(name);
                    if (member == null)
                    {
                      result = name + " not found in reverse index";
                      mongitorstate = 3;
                    }
                    else
                    {
                      String state = memberdoc2.getString("stateStr");
                      if (state == null)
                      {
                        result = "Replication members state not found for " + name;
                        mongitorstate = 3;
                      }
                      else
                      {
                        member.setState(state);
                      }
                    
                      if (!memberdoc2.containsKey("self") || !memberdoc2.getBoolean("self"))
                      {
                        long ts_seondary = memberdoc2.getDate("optimeDate").toInstant().toEpochMilli();
                        long lag = Math.round((ts_primary - ts_seondary)/1000);
                        WebsocketMessage websocketMessage = new WebsocketMessage("replicationlag", "response", "{ \"server\": \"" + name + "\",\"lag\": " + lag + "}");
                        websocketMessage.send();
                        if (lag > 10)
                        {
                          member.setMongitorstate(Math.max(2, member.getMongitorstate()));
                          mongitorstate = 2;
                          result = "High replication lag on " + name;
                        }
                      }
                    }
                  } 
                }
                
              }
            }
          }
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace(Debug.getPrintStream());
      result = "Exception while checking " + id;
      mongitorstate = 3;
      
    }

    return result;
  }
  
  public CommandResult overview()
  {
    MongoClient mongoClient = null;
    MongoDatabase mongoDatabase = null;
    String json = "{\"_id\":\""+ hostname +"\",\"version\":";
    try
    {
      mongoClient = MongoConnections.getConnection(hostname, port);
      if (mongoClient == null)
      {
        return new CommandResult("Could not connect to " + hostname);
      }
      mongoDatabase = mongoClient.getDatabase("admin");
      Document statusResults = mongoDatabase.runCommand(new Document("serverStatus", 1));
      if ((statusResults == null) || (statusResults.getDouble("ok") != 1.0))
      {
        return new CommandResult("Could not get status of " + id);
      }
      else
      {
        String version = (String) statusResults.get("version");
        if (version == null)
        {
          version = "?";
        }
        json = json + "\"" + version + "\",\"uptime\":";
        
        double uptime = statusResults.getDouble("uptime");
        int numberOfDays = (int) Math.floor(uptime / 86400);
        int numberOfHours = (int) Math.floor((uptime % 86400 ) / 3600);
        json = json + "\"" + numberOfDays + " days, " + numberOfHours + " hours\",\"local time\":";
        
        Date localTime = statusResults.getDate("localTime");
        if (localTime == null)
        {
          json = json + "\"?\"";
        }
        else
        {
          SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
          json = json + "\"" + dateFormat.format(localTime) + "\"";
        }
        
      }
      Document infoResults = mongoDatabase.runCommand(new Document("hostInfo", 1));
      if ((infoResults == null) || (infoResults.getDouble("ok") != 1.0))
      {
        return new CommandResult("Could not get host info of " + id);
      }
      else
      {
        Document osdoc = (Document) infoResults.get("os");
        if (osdoc != null)
        {
          json = json + ",\"os\":" + osdoc.toJson();
        }
      }      
      json = json + "}";
    }
    catch (Exception e)
    {
      e.printStackTrace(Debug.getPrintStream());
      return new CommandResult("Exception while checking " + id);
    }
    return new CommandResult("OK", json);
  }
}
