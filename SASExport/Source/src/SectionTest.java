import junit.framework.TestCase;

public class SectionTest extends TestCase
{
    public SectionTest(String name)
    {
	super(name);
    }

    public void testSomething()
    {
	assertTrue(4 == (2+2));
    }

    public void testAnother()
    {
	assertTrue(5 == (2+3));
    }

    public void testAgain()
    {
	String one, two;
	
	one = "Stephen";
	two = "Stephen";

	assertSame(one,two);
    }
	
}
