package nl.tools4all.mongitor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.Properties;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Mongitor
{
  @SuppressWarnings("serial")
  public static class PortServlet extends HttpServlet
  {
     public static int port;
     @Override
     protected void doGet( HttpServletRequest request,
                           HttpServletResponse response ) throws ServletException,
                                                         IOException
     {
         response.setContentType("text/html");
         response.setStatus(HttpServletResponse.SC_OK);
         response.getWriter().println("var websocketport="+port+";");
     }
  }
  
  // no instances needed
  private Mongitor()
  {
  }

  private static Server server;
  private static Server webserver;
  private static WebSocket websocket;
  private static Properties prop;
  
  public static int initialize(String[] arguments)
  { 
    String propertiesfile = "Mongitor.properties";
    if (arguments.length > 0)
    {
      propertiesfile = arguments[0];
    }  
    prop = new Properties();
    InputStream input = null;

    try
    {
      input = new FileInputStream(propertiesfile);

      // load a properties file
      prop.load(input);
    }
    catch (Exception e)
    {
      System.err.println("Could not read properties file " + propertiesfile);
      e.printStackTrace();
    } finally
    {
      if (input != null)
      {
        try
        {
          input.close();
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }
    

    Json.initialize();
    String debugfilename = prop.getProperty("debugfile", "");
    String result = Debug.initialize(debugfilename);
    if (!"OK".equals(result))
    {
      System.err.println("Failed to open debugfile " + debugfilename);
      return 4;
    }
    MongoCredentials.setCredentialsFilename(prop.getProperty("credentialsfile", "Mongitor.credentials"));
    
    String wsportstr = prop.getProperty("websocketport", "8081");
    int wsport;
    try
    {
      wsport = Integer.parseInt(wsportstr);
    } 
    catch (Exception e)
    {
      wsport = 8081;
    }
    server = new Server(wsport);
    
    PortServlet.port = wsport;
    WebSocketHandler wsHandler = new WebSocketHandler()
    {
      @Override
      public void configure(WebSocketServletFactory factory)
      {
        factory.register(MyWebSocketHandler.class);
      }
    };

    server.setHandler(wsHandler);
    try
    {
      server.start();
    }
    catch (Exception e)
    {
      System.err.println("Error starting websocket server");
      e.printStackTrace();
      return 1;
    }

    System.out.println(Paths.get(".").toAbsolutePath().normalize().toString());
    System.out.println("Message: websocket server started.");
    // server.join();
    
    
    String webportstr = prop.getProperty("webserverport", "8080");
    int webport;
    try
    {
      webport = Integer.parseInt(webportstr);
    } 
    catch (Exception e)
    {
      webport = 8081;
    }
    // Create a basic jetty server object that will listen on port 8080.
    // Note that if you set this to port 0 then a randomly available port
    // will be assigned that you can either look in the logs for the port,
    // or programmatically obtain it for use in test cases.
    webserver = new Server(webport);

    // Setup JMX
    MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    webserver.addBean(mbContainer);

    // The WebAppContext is the entity that controls the environment in
    // which a web application lives and breathes. In this example the
    // context path is being set to "/" so it is suitable for serving root
    // context requests and then we see it setting the location of the war.
    // A whole host of other configurations are available, ranging from
    // configuring to support annotation scanning in the webapp (through
    // PlusConfiguration) to choosing where the webapp will unpack itself.
    WebAppContext webapp = new WebAppContext();
    webapp.setContextPath("/Mongitor");
    File warFile = new File(prop.getProperty("warfile", "Mongitor.war"));
    // File warFile = new File("Mongitor.war");
    if (!warFile.exists())
    {
      System.err.println("Unable to find WAR File: " + warFile.getAbsolutePath());
      return 2;
    }
    webapp.setWar(warFile.getAbsolutePath());

    // This webapp will use jsps and jstl. We need to enable the
    // AnnotationConfiguration in order to correctly
    // set up the jsp container
    Configuration.ClassList classlist = Configuration.ClassList.setServerDefault(webserver);
    classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
        "org.eclipse.jetty.annotations.AnnotationConfiguration");

    // Set the ContainerIncludeJarPattern so that jetty examines these
    // container-path jars for tlds, web-fragments etc.
    // If you omit the jar that contains the jstl .tlds, the jsp engine will
    // scan for them instead.
    webapp.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
        ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$");

    webapp.addAliasCheck(new AllowSymLinkAliasChecker());

    // A WebAppContext is a ContextHandler as well so it needs to be set to
    // the server so it is aware of where to send the appropriate requests.
    //webserver.setHandler(webapp);
    // The ServletHandler is a dead simple way to create a context handler
    // that is backed by an instance of a Servlet.
    // This handler then needs to be registered with the Server object.
    ServletHandler handler = new ServletHandler();
    //webserver.setHandler(handler);

    // Passing in the class for the Servlet allows jetty to instantiate an
    // instance of that Servlet and mount it on a given context path.

    // IMPORTANT:
    // This is a raw Servlet, not a Servlet that has been configured
    // through a web.xml @WebServlet annotation, or anything similar.
    handler.addServletWithMapping(PortServlet.class, "/port");
    HandlerCollection handlerCollection = new HandlerCollection();
    handlerCollection.setHandlers(new Handler[] {webapp, handler});
    webserver.setHandler(handlerCollection);
    // Start webserver
    try
    {
      webserver.start();
    }
    catch (Exception e)
    {
      System.err.println("Error starting webserver");
      e.printStackTrace();
      return 3;
    }

    // webserver.dumpStdErr();

    // The use of server.join() the will make the current thread join and
    // wait until the server is done executing.
    // See http://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html#join()
    // webserver.join();
    System.out.println("Message: webserver started.");
    return 0;
  }

  /**
   * @return the websocket
   */
  public static WebSocket getWebsocket()
  {
    return websocket;
  }

  /**
   * @param websocket
   *          the websocket to set
   */
  public static void setWebsocket(WebSocket websocket)
  {
    Mongitor.websocket = websocket;
  }

}
