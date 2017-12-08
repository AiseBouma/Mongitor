/**
 * 
 */
package nl.tools4all.mongitor;

import org.bson.Document;

/**
 * @author supabouma
 *
 */
public class DocumentGetNumber
{
  private boolean ok = true;
  private double value;
  
  /**
   * @param document
   * @param key
   */
  public DocumentGetNumber(Document document, String key)
  {
    try
    {
      Object object = document.get(key);
      if (object == null)
      {
        ok = false;
      }
      else if (Integer.class.isInstance(object))
      {
        int i = ((Integer) object).intValue();    
        value = (double) i;
      }
      else if (Long.class.isInstance(object))
      {
        long l = ((Long) object).longValue();    
        value = (double) l;
      }
      else if (Double.class.isInstance(object))
      {
        value = ((Double) object).doubleValue();
      }
      else
      {
        ok = false;
      }
    }
    catch (Exception e)
    {
      ok = false;
    }
  }

  /**
   * @return the ok
   */
  public boolean isOk()
  {
    return ok;
  }

  /**
   * @return the value
   */
  public double getValue()
  {
    return value;
  }
  
  
}
