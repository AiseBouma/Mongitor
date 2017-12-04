package nl.tools4all.mongitor;

public class MongoServer
{
  protected String id;
  protected String hostname;
  protected int port;
  protected int mongitorstate = 0;
  protected boolean connected = false;

  /**
   * @param id
   * @param hostname
   * @param port
   */
  public MongoServer(String id, String hostname, int port)
  {
    this.id = id;
    this.hostname = hostname;
    this.port = port;
  }

  /**
   * @return the _id
   */
  public String get_id()
  {
    return id;
  }

  /**
   * @param _id
   *          the _id to set
   */
  public void set_id(String id)
  {
    this.id = id;
  }

  /**
   * @return the hostname
   */
  public String getHostname()
  {
    return hostname;
  }

  /**
   * @param hostname
   *          the hostname to set
   */
  public void setHostname(String hostname)
  {
    this.hostname = hostname;
  }

  /**
   * @return the port
   */
  public int getPort()
  {
    return port;
  }

  /**
   * @param port
   *          the port to set
   */
  public void setPort(int port)
  {
    this.port = port;
  }

  /**
   * @return the connected
   */
  public boolean isConnected()
  {
    return connected;
  }

  /**
   * @param connected
   *          the connected to set
   */
  public void setConnected(boolean connected)
  {
    this.connected = connected;
  }

  /**
   * @return the mongitorstate
   */
  public int getMongitorstate()
  {
    return mongitorstate;
  }

  /**
   * @param mongitorstate
   *          the mongitorstate to set
   */
  public void setMongitorstate(int mongitorstate)
  {
    this.mongitorstate = mongitorstate;
  }


}
