package nl.tools4all.mongitor;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jetty.websocket.api.Session;

/**
 * 
 */

/**
 * @author Aise
 *
 */
public class WebSocketSession
{
  private static Session session = null;
  private static ArrayList<String> backlog = new ArrayList<String>();
  
  private WebSocketSession()
  {
  }

  private static void sendBacklog()
  {
    boolean ok = true;
    while (!backlog.isEmpty() && session != null && session.isOpen() && ok)
    {
      try
      {
        session.getRemote().sendString(backlog.get(0));
        backlog.remove(0);
      }
      catch (IOException e)
      {
        ok = false;
      }
    }
  }
  
  /**
   * @param session the session to set
   */
  public static void setSession(Session session)
  {
    WebSocketSession.session = session;
    sendBacklog();
  }
  
  public static void sendMessage(String message)
  {
    backlog.add(message);
    sendBacklog();
  }
}
