package nl.tools4all.mongitor;
/**
 * 
 */

/**
 * @author supabouma
 *
 */
public class CommandResult
{
  private String status = "";
  private String result = "";
  
  /**
   * @param status
   */
  public CommandResult(String status)
  {
    this.status = status;
  }
  
  /**
   * @param status
   * @param result
   */
  public CommandResult(String status, String result)
  {
    this.status = status;
    this.result = result;
  }

  /**
   * @return the status
   */
  public String getStatus()
  {
    return status;
  }
  /**
   * @param status the status to set
   */
  public void setStatus(String status)
  {
    this.status = status;
  }
  /**
   * @return the result
   */
  public String getResult()
  {
    return result;
  }
  /**
   * @param result the result to set
   */
  public void setResult(String result)
  {
    this.result = result;
  }
  
}
