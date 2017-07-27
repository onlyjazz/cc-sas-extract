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
 *  Describes all the columns and items that make up a group on a crf
 *
 **/
public class Table extends ContainerItem
{
    Format format = null;

    public Table(String id, String name)
    {
	super(id, name);
    }

    // Overridden
    public void insertData(String study_subject_id, String item_name, String value)
    {
	// Get the Column Object where this data point will live
	Column column = findColumn(item_name);

	if (column == null) {
	    System.out.println("ERROR: Can not find column with name " + item_name);
	    return;
	}

	// Store this data point in the Column Object
	column.insertData(study_subject_id, value);
    }

    /* Must iterate over the sections in this table and have them print to map file */
    public void writeMapFile(BufferedWriter file)
    {
	// Step through each section in this table and write out the map file
	Iterator sections = sections().iterator();

	while (sections.hasNext()) {
	    Section section = (Section)sections.next();
	    section.writeMapFile(file);
	}
    }

    public Format getFormatObject()
    {
	return format;
    }

    public int type() {	return Item.TABLE; }

    public static void main(String[] args)
    {

    }
}