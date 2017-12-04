package nl.tools4all.mongitor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 */

/**
 * @author Aise
 *
 */
public class Json
{
  private static ObjectMapper objectMapper;
  
  private Json()
  {
  }
  
  public static void initialize()
  {
    objectMapper = new ObjectMapper();
  }

  /**
   * @return the objectMapper
   */
  public static ObjectMapper getObjectMapper()
  {
    return objectMapper;
  }
}
