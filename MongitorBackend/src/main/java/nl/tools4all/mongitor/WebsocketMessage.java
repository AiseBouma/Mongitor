package nl.tools4all.mongitor;

public class WebsocketMessage
{
  private String id;
  private String type;
  private Object message;
  private boolean ok = true;
  private String error = "";

  /**
   * @param id
   * @param type
   * @param message
   */
  public WebsocketMessage(String id, String type, Object message)
  {
    this.id = id;
    this.type = type;
    this.message = message;
  }

  /**
   * @param id
   * @param type
   * @param message
   * @param ok
   * @param error
   */
  public WebsocketMessage(String id, String type, Object message, boolean ok, String error)
  {
    this.id = id;
    this.type = type;
    this.message = message;
    this.ok = ok;
    this.error = error;
  }
  
  public void send()
  {
    try
    {
      String jsonString = Json.getObjectMapper().writeValueAsString(this);
      WebSocketSession.sendMessage(jsonString);
      System.out.println(jsonString);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
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
   * @return the error
   */
  public String getError()
  {
    return error;
  }

  /**
   * @param error
   *          the error to set
   */
  public void setError(String error)
  {
    this.error = error;
  }

  /**
   * @return the ok
   */
  public boolean isOk()
  {
    return ok;
  }

  /**
   * @param ok
   *          the ok to set
   */
  public void setOk(boolean ok)
  {
    this.ok = ok;
  }

  /**
   * @return the type
   */
  public String getType()
  {
    return type;
  }

  /**
   * @param type
   *          the type to set
   */
  public void setType(String type)
  {
    this.type = type;
  }

  /**
   * @return the message
   */
  public Object getMessage()
  {
    return message;
  }

  /**
   * @param message
   *          the message to set
   */
  public void setMessage(Object message)
  {
    this.message = message;
  }
}
