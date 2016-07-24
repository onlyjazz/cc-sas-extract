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
public class MultiSelect extends Column
{
    ArrayList<String> columns;     // Column names
    ArrayList<String> options;     // Options    
    ArrayList<String> item_names;  // Columns that will be marked as "true" or "1". These match up to column names.

    public MultiSelect(String item_name, String description, String format, String values, String text)
    {
	super(item_name, description, format, -2, "MULTISELECT");

	// Create the column names for this multiselect/checkbox object
	columns = new ArrayList<String>();
	options = new ArrayList<String>();
	item_names = new ArrayList<String>();	

	String[] names = values.split(",");
	String[] texts = text.split(",");	

	for (int i=0; i < names.length; i++) {
	    String column_name = Helper.createXMLTagName(item_name + "_" + names[i]);	    
	    columns.add(column_name);
	    options.add(Helper.cleanse(Helper.cleanse(texts[i])));
	    item_names.add(names[i].trim());	    
	}
    }

    public void write(BufferedWriter file, String indent, Section.UniqueData d, int ordinal)
    {
	try {

	    ArrayList<String> data_arr = new ArrayList<String>();
	    Iterator names = columns.listIterator();
	    ColumnData data = getDataPoint(d,ordinal);

	    if (data != null) {
		String[] split_data = data.value().split(",");
		for (int i=0; i < split_data.length; i++) {
		    data_arr.add(split_data[i]);
		}
	    }

	    

	    int index = -1;
	    while (names.hasNext()) {
		index++;
		String column_name = (String)names.next();
		file.write(indent + "  <" + column_name + ">");		

		// Data for this multiselect is comma seperated
		if (data != null) {
		    if (data_arr.contains(item_names.get(index))) {
			file.write("1");
		    } else {
			file.write("0");
		    }
		}

		file.write("</" + column_name + ">"); file.newLine();
	    }

	} catch (IOException e) {}
    }

    public void writeToMapFile(BufferedWriter file, String section_name)
    {
	try {
	    
	    /* Data must be written to the map file for every column that this multi-select item contains */
	    ListIterator itr = columns.listIterator();
	    Iterator options_itr = options.listIterator();

	    while (itr.hasNext()) {

		String column = (String)itr.next();
		String option = (String)options_itr.next();
	    
		file.write("    <COLUMN name=\"" + Helper.cleanse(column) + "\">"); file.newLine();
		file.write("      <PATH syntax=\"XPath\">/OCEXPORT/TABLE/"+ section_name +"/"+ column +"</PATH>"); file.newLine();
		file.write("      <DESCRIPTION>"+ xmlDescription() + " - " + option +"</DESCRIPTION>"); file.newLine();
		file.write("      <TYPE>numeric</TYPE>"); file.newLine();
		file.write("      <DATATYPE>integer</DATATYPE>"); file.newLine();
		file.write("      <FORMAT width=\"2\">BEST</FORMAT>"); file.newLine();
		file.write("    </COLUMN>"); 	file.newLine();    	    
		
	    }

	} catch (IOException e) {}
    }

    public static void main(String[] args)
    {

    }
}