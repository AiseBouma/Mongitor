package nl.tools4all.mongitor;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import org.bson.BsonTimestamp;
import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

/**
 * 
 */

/**
 * @author supabouma
 *
 */
public class OplogCommands
{

  private OplogCommands()
  {
  }

  public static CommandResult replicationInfo(String server)
  {
    String json = "";
    String error = "";
    MongoDatabase mongoDatabase = null;
    MongoCollection<Document> collection = null;
    MongoCursor<Document> cursor;
    Document document;
    int ts_start = 0;
    int ts_end = 0;
    
    // try to connect to the oplog collection
    try
    {
      MongoClient mongoClient = MongoConnections.getConnection(server);
      if (mongoClient == null)
      {
        error = "No connection to server " + server;
      }
      else
      {
        mongoDatabase = mongoClient.getDatabase("local");
        collection = mongoDatabase.getCollection("oplog.rs");
      }
    }
    catch (Exception e)
    {
      error = "Exception while reading oplog:" + e.getMessage();
    }
    
    if ("".equals(error))
    {
      try
      {
        // get the start time of the oplog
        cursor = collection.find()
                           .sort(new Document("$natural", 1))
                           .limit(1)
                           .iterator();
        if (!cursor.hasNext())
        {
          error = "No document found in oplog";
        }
        else
        {  
          document = cursor.next();
          BsonTimestamp timestamp = (BsonTimestamp) document.get("ts");
          ts_start = timestamp.getTime();
        }
        cursor.close();
      }  
      catch (Exception e)
      {
        error = "Exception while getting start time oplog:" + e.getMessage();
      }
    }
    
    if ("".equals(error))
    {
      try
      {
        // get the end time of the oplog
        cursor = collection.find()
                           .sort(new Document("$natural", -1))
                           .limit(1)
                           .iterator();
        if (!cursor.hasNext())
        {
          error = "No last document found in oplog";
        }
        else
        {  
          document = cursor.next();
          BsonTimestamp timestamp = (BsonTimestamp) document.get("ts");
          ts_end = timestamp.getTime();
        }
        cursor.close();
      }  
      catch (Exception e)
      {
        error = "Exception while getting end time oplog:" + e.getMessage();
      }
    }
    
    double maxsize = 0.0;
    double size = 0.0;
    long usage = 0;
    
    if ("".equals(error))
    {
      try
      {
        Document stats = mongoDatabase.runCommand(new Document("collStats", "oplog.rs"));
        DocumentGetNumber documentGetNumber = new DocumentGetNumber(stats, "size");
        if (!documentGetNumber.isOk())
        {
          error = "Could not get size of oplog";
        }
        else
        {
          size = documentGetNumber.getValue();
        
          documentGetNumber = new DocumentGetNumber(stats, "maxSize");
          if (!documentGetNumber.isOk())
          {
            error = "Could not get  maximum size of oplog";
          }
          else
          {
            maxsize = documentGetNumber.getValue();
            if (maxsize > 0)
            {  
              usage = Math.round(100 * size / maxsize);
            }
            else
            {
              error = "Maximum size of oplog is incorrect";
            }
          }  
        }  
      }
      catch (Exception e)
      {
        error = "Exception while getting oplog usage:" + e.getMessage();
      }
    }  
    
    if ("".equals(error))
    {
      int oploglength = ts_end - ts_start;
      int numberOfDays;
      int numberOfHours;

      numberOfDays = oploglength / 86400;
      numberOfHours = (oploglength % 86400 ) / 3600;
      SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
      json = "{";
      json =json + "\"Oplog start time\": \"" + dateFormat.format(Date.from(Instant.ofEpochSecond( ts_start ))) + "\"";
      json =json + ", \"Oplog end time\": \"" + dateFormat.format(Date.from(Instant.ofEpochSecond( ts_end ))) + "\"";
      json =json + ", \"Length of Oplog\": \"" + numberOfDays + " days, " + numberOfHours + " hours\"";
      json = json + ", \"Oplog maximum size\": \"" + Math.round(maxsize/(1024*1024)) + " MB\"";
      json = json + ", \"Oplog size\": \"" + Math.round(size/(1024*1024)) + " MB\"";
      json = json + ", \"Percentage used\": \"" + usage + "%\"";
      json = json + "}";
      return new CommandResult("OK", json);
    }
    else
    {
      return new CommandResult(error);
    }
  }
}

