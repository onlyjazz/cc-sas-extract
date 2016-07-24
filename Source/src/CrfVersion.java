import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Arrays;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 *
 **/
public class CrfVersion extends ContainerItem
{
    private ArrayList<String> versions;

    public CrfVersion(String id, String name)
    {
	super(id, name);
	versions = new ArrayList<String>();
    }

    public int type() {	return Item.CRF_VERSION; }

    public void addVersion(String version)
    {
	versions.add(version);
    }

    public ArrayList<String> crfVersions()
    {
	return versions;
    }


    public void printSections()
    {
	for (Item item : getItems()) {
	    System.out.println("      Section: \t<<-- " + item.xmlName() + " -->>");
	    System.out.print("          ids: ");
	    ((Section)item).printIDS();
	    System.out.println();
	}
    }

    public static void main(String[] args)
    {

    }
}