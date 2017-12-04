package nl.tools4all.mongitor;

public class MongitorMain
{
	public static void main(String[] args)
	{
    int status = Mongitor.initialize(args);
    if (status != 0)
    {
      System.exit(status);
    }
	}

}
