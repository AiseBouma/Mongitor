package nl.tools4all.mongitor;
import java.io.PrintStream;

/**
 * 
 */

/**
 * @author supabouma
 *
 */
public class Debug
{
  private static PrintStream printStream = null;
  
  private Debug()
  {
    
  }
  
  public static String initialize(String filename)
  {
    if ("".equals(filename))
    {
      printStream = System.err;
    }
    else
    {  
      try
      {
        printStream = new PrintStream(filename); 
      }
      catch (Exception e)
      {
        return e.getMessage();
      }
    
    }
    return "OK";
  }

  /**
   * @return the printStream
   */
  public static PrintStream getPrintStream()
  {
    return printStream;
  }  
}
