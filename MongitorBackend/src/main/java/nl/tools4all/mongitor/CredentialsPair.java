package nl.tools4all.mongitor;
/**
 * 
 */

/**
 * @author Aise
 *
 */
public class CredentialsPair
{
  /**
   * @param username the username to set
   */
  public void setUsername(String username)
  {
    this.username = username;
  }

  /**
   * @param password the password to set
   */
  public void setPassword(String password)
  {
    this.password = password;
  }

  private String username;
  private String password;
  
  /**
   * @param username
   * @param password
   */
  public CredentialsPair(String username, String password)
  {
    this.username = username;
    this.password = password;
  }
  
  public CredentialsPair()
  {
  }
  
  /**
   * @return the username
   */
  public String getUsername()
  {
    return username;
  }

  /**
   * @return the password
   */
  public String getPassword()
  {
    return password;
  }
  
}
