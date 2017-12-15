package nl.tools4all.mongitor;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

@WebSocket
public class MyWebSocketHandler
{

  @OnWebSocketClose
  public void onClose(int statusCode, String reason)
  {
    System.out.println("Close: statusCode=" + statusCode + ", reason=" + reason);
  }

  @OnWebSocketError
  public void onError(Throwable t)
  {
    System.out.println("Error2: " + t.getMessage());
  }

  @OnWebSocketConnect
  public void onConnect(Session session)
  {
    System.out.println("Connect: " + session.getRemoteAddress().getAddress());
    WebSocketSession.setSession(session);
  }

  @SuppressWarnings("unchecked")
  @OnWebSocketMessage
  public void onText(String message)
  {
    System.out.println("Message: " + message);
    Map<String, String> messageMap = new HashMap<String, String>();

    try
    {
      messageMap = Json.getObjectMapper().readValue(message, HashMap.class);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    System.out.println("Map is: " + messageMap);
    WebsocketMessage websocketMessage = null;
    if ("command".equals(messageMap.get("type")))
    {
      if ("init".equals(messageMap.get("id")))
      {
        if (MongoCredentials.fileExists())
        {
          websocketMessage = new WebsocketMessage("init", "response", "getpassword");
        }
        else
        {
          websocketMessage = new WebsocketMessage("init", "response", "nocredentialsfile");
        }
      }
      else if ("setpassword".equals(messageMap.get("id")))
      {
        String result = MongoCredentials.setMongitorPassword(messageMap.get("password"));

        if ("OK".equals(result))
        {
          websocketMessage = new WebsocketMessage("setpassword", "response", "");
        }
        else
        {
          websocketMessage = new WebsocketMessage("setpassword", "response", "", false, result);
        }
      }
      else if ("checkpassword".equals(messageMap.get("id")))
      {
        String result = MongoCredentials.checkMongitorPassword(messageMap.get("password"));

        if ("OK".equals(result))
        {
          websocketMessage = new WebsocketMessage("checkpassword", "response", "");
        }
        else
        {
          websocketMessage = new WebsocketMessage("checkpassword", "response", "", false, result);
        }
      }
      else if ("defaultcredentialsexist".equals(messageMap.get("id")))
      {
        String result = MongoCredentials.credentialExists("default");

        if ("yes".equals(result) || "no".equals(result))
        {
          websocketMessage = new WebsocketMessage("defaultcredentialsexist", "response", result);
        }
        else
        {
          websocketMessage = new WebsocketMessage("defaultcredentialsexist", "response", "", false, result);
        }
      }
      else if ("connecttorouter".equals(messageMap.get("id")))
      {
        String result = MongoDBCluster.checkConnection(messageMap.get("host"), messageMap.get("port"),
            messageMap.get("username"), messageMap.get("password"));
        // result="OK";
        if ("OK".equals(result))
        {
          websocketMessage = new WebsocketMessage("connecttorouter", "response", result);
        }
        else
        {
          websocketMessage = new WebsocketMessage("connecttorouter", "response", "", false, result);
        }
      }
      else if ("detectcluster".equals(messageMap.get("id")))
      {
        String result = MongoDBCluster.detectCluster();
        // result="OK";
        if ("OK".equals(result))
        {
          websocketMessage = new WebsocketMessage("detectcluster", "response", MongoDBCluster.getJson());
          // websocketMessage = new WebsocketMessage("detectcluster", "response", "");
        }
        else
        {
          websocketMessage = new WebsocketMessage("detectcluster", "response", "", false, result);
        }
      }
      else if ("checkcluster".equals(messageMap.get("id")))
      {
        String result = MongoDBCluster.checkCluster();

        if ("OK".equals(result))
        {
          websocketMessage = new WebsocketMessage("checkcluster", "response", MongoDBCluster.getJson());
        }
        else
        {
          websocketMessage = new WebsocketMessage("checkcluster", "response", MongoDBCluster.getJson(), false, result);
        }
      }
      else if ("updateothercredentials".equals(messageMap.get("id")))
      {
        if (MongoCredentials.updateOtherCredentials(messageMap.get("credentials")))
        {
          websocketMessage = new WebsocketMessage("updateothercredentials", "response", "");
        }
        else
        {
          websocketMessage = new WebsocketMessage("updateothercredentials", "response", "", false,
              "Not all required fields are filled.");
        }
      }
      else if ("listcredentials".equals(messageMap.get("id")))
      {
        String credentialsList = MongoCredentials.listCredentials();
        if (credentialsList != null)
        {
          websocketMessage = new WebsocketMessage("listcredentials", "response", credentialsList);
        }
        else
        {
          websocketMessage = new WebsocketMessage("listcredentials", "response", "", false,
              "Could not retrieve the credentials.");
        }
      }
      else if ("updatedefaultcredentials".equals(messageMap.get("id")))
      {
        String result = MongoDBCluster.checkConnection(messageMap.get("host"), messageMap.get("port"),
            messageMap.get("username"), messageMap.get("password"));
        if ("OK".equals(result))
        {
          websocketMessage = new WebsocketMessage("updatedefaultcredentials", "response", result);
        }
        else
        {
          websocketMessage = new WebsocketMessage("updatedefaultcredentials", "response", "", false, result);
        }
      }
      else if ("mongocommand".equals(messageMap.get("id")))
      {
        CommandResult commandResult = MongoConnections.runCommand(messageMap.get("server"), messageMap.get("mongocommand"));
        if ("OK".equals(commandResult.getStatus()))
        {
          websocketMessage = new WebsocketMessage("mongocommand", "response", commandResult.getResult());
        }
        else
        {
          websocketMessage = new WebsocketMessage("mongocommand", "response", "", false, commandResult.getStatus());
        }
      }
      else if ("databasedetails".equals(messageMap.get("id")))
      {
        CommandResult commandResult = MongoDBCluster.databaseDetails(messageMap.get("database"));
        if ("OK".equals(commandResult.getStatus()))
        {
          websocketMessage = new WebsocketMessage("databasedetails", "response", commandResult.getResult());
        }
        else
        {
          websocketMessage = new WebsocketMessage("databasedetails", "response", "", false, commandResult.getStatus());
        }
      }
      else if ("collectiondetails".equals(messageMap.get("id")))
      {
        CommandResult commandResult = MongoDBCluster.collectionDetails(messageMap.get("database"), messageMap.get("collection"));
        if ("OK".equals(commandResult.getStatus()))
        {
          websocketMessage = new WebsocketMessage("collectiondetails", "response", commandResult.getResult());
        }
        else
        {
          websocketMessage = new WebsocketMessage("collectiondetails", "response", "", false, commandResult.getStatus());
        }
      }
      else if ("sharddetails".equals(messageMap.get("id")))
      {
        CommandResult commandResult = MongoDBCluster.shardDetails(messageMap.get("shard"));
        if ("OK".equals(commandResult.getStatus()))
        {
          websocketMessage = new WebsocketMessage("sharddetails", "response", commandResult.getResult());
        }
        else
        {
          websocketMessage = new WebsocketMessage("sharddetails", "response", "", false, commandResult.getStatus());
        }
      }
      else if ("cleanuporphanscount".equals(messageMap.get("id")))
      {
        CommandResult commandResult = MongoDBCluster.orphanChunkCount(messageMap.get("shard"), messageMap.get("hostname"), messageMap.get("collection"));
        if ("OK".equals(commandResult.getStatus()))
        {
          websocketMessage = new WebsocketMessage("cleanuporphanscount", "response", commandResult.getResult());
        }
        else
        {
          websocketMessage = new WebsocketMessage("cleanuporphanscount", "response", "", false, commandResult.getStatus());
        }
      }
      else if ("cleanuporphans".equals(messageMap.get("id")))
      {
        CommandResult commandResult = MongoConnections.orphanCleanup(messageMap.get("index"), messageMap.get("shard"), messageMap.get("server"), messageMap.get("collection"));
        if ("OK".equals(commandResult.getStatus()))
        {
          websocketMessage = new WebsocketMessage("cleanuporphans", "response", commandResult.getResult());
        }
        else
        {
          websocketMessage = new WebsocketMessage("cleanuporphans", "response", "", false, commandResult.getStatus());
        }
      }
      else if ("getserverinfo".equals(messageMap.get("id")))
      {
        CommandResult commandResult = MongoDBCluster.getClusterOverview();
        if ("OK".equals(commandResult.getStatus()))
        {
          websocketMessage = new WebsocketMessage("getserverinfo", "response", commandResult.getResult());
        }
        else
        {
          websocketMessage = new WebsocketMessage("getserverinfo", "response", "", false, commandResult.getStatus());
        }
      }
      else if ("listdatabases".equals(messageMap.get("id")))
      {
        CommandResult commandResult = MongoDBCluster.listDatabases();
        if ("OK".equals(commandResult.getStatus()))
        {
          websocketMessage = new WebsocketMessage("listdatabases", "response", commandResult.getResult());
        }
        else
        {
          websocketMessage = new WebsocketMessage("listdatabases", "response", "", false, commandResult.getStatus());
        }
      }
      else if ("closeconnections".equals(messageMap.get("id")))
      {
        MongoDBCluster.closeConnections();
      } 
      else if ("getclusterinfo".equals(messageMap.get("id")))
      {
        CommandResult commandResult = MongoDBCluster.getClusterInfo();
        if ("OK".equals(commandResult.getStatus()))
        {
          websocketMessage = new WebsocketMessage("getclusterinfo", "response", commandResult.getResult());
        }
        else
        {
          websocketMessage = new WebsocketMessage("getclusterinfo", "response", "", false, commandResult.getStatus());
        }
      }
      else if ("getshardedcollections".equals(messageMap.get("id")))
      {
        CommandResult commandResult = MongoDBCluster.getShardedCollections();
        if ("OK".equals(commandResult.getStatus()))
        {
          websocketMessage = new WebsocketMessage("getshardedcollections", "response", commandResult.getResult());
        }
        else
        {
          websocketMessage = new WebsocketMessage("getshardedcollections", "response", "", false, commandResult.getStatus());
        }
      }
      else if ("setbalancer".equals(messageMap.get("id")))
      {
        CommandResult commandResult = MongoDBCluster.setBalancerState(!"off".equals(messageMap.get("state")));
        if ("OK".equals(commandResult.getStatus()))
        {
          websocketMessage = new WebsocketMessage("setbalancer", "response", commandResult.getResult());
        }
        else
        {
          websocketMessage = new WebsocketMessage("setbalancer", "response", "", false, commandResult.getStatus());
        }
      }
    }
    if (websocketMessage != null)
    {
      websocketMessage.send();
    }
  }
}
