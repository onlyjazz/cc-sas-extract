import java.util.HashMap;
import java.util.HashSet;
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
public class Section extends ContainerItem
{
    public static int NO_HEADER = 0;
    public static int CREATE_GENERIC_HEADER = 1;
    private boolean generic_header;    
    private int has_group; // -1 = not defined, 0 = false, 1 = true

    private boolean xml_started;
    private boolean xml_completed;
    private boolean xml_table_tag_written;

    private ArrayList<String> ids = null;  // The id's of the sections this section represents

    public class UniqueData
    {
	public String sid;
	public String crf_version;
	public String event_id;

	public UniqueData(String s, String c, String e)
	{
	    sid = s;
	    crf_version = c;
	    event_id = e;
	}

	public int hashCode() 
	{
	    return 0;
	}

	public boolean equals(Object u)
	{	    
	    if (!sid.equals( ((UniqueData)u).sid))
		return false;

	    if (!crf_version.equals( ((UniqueData)u).crf_version))
		return false;

	    if (!event_id.equals( ((UniqueData)u).event_id))
		return false;

	    else return true;
	}
		
	public void print()
	{
	    System.out.println("sid: " + sid + "\tcrf_version: " + crf_version + "\tevent_id: " + event_id);
	}
    }

    public Section(String id, String name, String description, int header)
    {
	super(id, name);

	ids = new ArrayList<String>();
	ids.add(id);
	
	this.description = description;

	if (header == 1) {
	    generic_header = true;
	    createGenericColumns();
	} else {
	    generic_header = false;
	}

	has_group = -1;

	xml_started = xml_completed = xml_table_tag_written = false;
    }

    public void createGenericColumns()
    {
	// Currently only working for map file generation.
	Column column;
	column = new Column("study","Study Protocol ID","character","string",50,50,"$CHAR");                addItem(column);
	column = new Column("study_site","Site name","character","string",50,50,"$CHAR");	            addItem(column);
	column = new Column("sid","Study Subject ID","character","string",50,50,"$CHAR");              	    addItem(column);
	column = new Column("study_event","Study Event","character","string",50,50,"$CHAR");	                    addItem(column);
	column = new Column("event_startdt","Event start date time","numeric","datetime",19,19,"IS8601DT"); addItem(column);
	column = new Column("crf_name","Crf Name","character","string",50,50,"$CHAR");	                    addItem(column);
	column = new Column("crf_version","Crf Version","character","string",50,50,"$CHAR");	            addItem(column);
    }

    /** Gets all combinations of subject, crf_version and event that exist in this section 
	TODO: This needs to be refactored.  A hash should be created on the fly as data is
	being inserted.
    **/
    public ArrayList<UniqueData> getUniqueData()
    {
	// Get all ColumnData inside this Section object
	ArrayList<ColumnData> data = data();

	//	System.out.println("Number of data points in section: " + data.size());


	/*
	for (ColumnData cd : data) {
	    System.out.println("Subject: " + cd.studySubjectId());
	    System.out.println("Site   : " + cd.site());
	    System.out.println("Event  : " + cd.eventId());
	    System.out.println("CrfVer : " + cd.crfVersion());
	    System.out.println("Name   : " + cd.column.name());
	    System.out.println("Value  : " + cd.value());
	    System.out.println();
	}
	*/

	// Used to speed up code for large datasets
	HashSet<UniqueData> hash = new HashSet<UniqueData>(1000000);

	for (ColumnData d : data) {
	    UniqueData u = new UniqueData(d.studySubjectId(), d.crfVersion(), d.eventId());
	    hash.add(u);
	}

	ArrayList<UniqueData> ud = new ArrayList<UniqueData>(hash);	

	/*
	// TESTING
	for (UniqueData d : ud) {
	    System.out.print("  Data found in this section: "); d.print();
	}	
	*/

	return ud;
    }
       
    public Iterator getIds()
    {
	return ids.iterator();
    }

    public boolean hasId(String id)
    {
	Iterator itr = ids.iterator();

	while (itr.hasNext()) {
	    String sid = (String)itr.next();
	    if (sid.equals(id)) {
		return true;
	    }
	}

	return false;
    }

    // Returns true if this section contains a group
    // TODO: There should be a flag that already has this information and is modified as data is entered
    public boolean hasGroup()
    {
	switch(has_group) {
	case 0: return false;
	case 1: return true;
	default:
	    Iterator itr = getItemsIterator();
	    while (itr.hasNext()) {
		Item item = (Item)itr.next();
		if (item.type() == Item.GROUP) {
		    has_group = 1;
		    return true;
		}
	    }
	    has_group = 0;
	    return false;
	}
    }

    public void writeToXmlFile(BufferedWriter file, String indent)
    {
	try {

	    // Iterate through each study_subject that has information stored in this section
	    // and write out a sas row in the xml file for that study subject
	    ArrayList<UniqueData> ud = getUniqueData();

	    // Check if this section has been partially written to the file already
	    if (!xml_started) {
		xml_started = true;

		if (ud.size() > 0) {
		    xml_table_tag_written = true;
		    file.write(indent + "<TABLE>"); file.newLine();
		}
	    }

	    for (UniqueData d : ud) {
		//d.print();
		
		// Depth is the maximum number of repeat items on this section
		int depth = getDepth(d);

		for (int i=1; i <= depth; i++) {
		    write(file,indent+"  ",d, i);
		}

		// Erase this column data for this subject/event/crfversion from memory
		for (Item item : items_contained) {
		    item.eraseSubjectData(d);
		}		
	    }

	    // Has this section been finalized?
	    if (xml_completed && xml_table_tag_written) {
		file.write(indent + "</TABLE>"); file.newLine();		
	    }
	
	} catch (IOException e) {
	    System.out.println("IOException in Section::writeToXmlFile(): " + e);
	}
    }

    public void sectionCompleted()
    {
	xml_completed = true;
    }

    public void write(BufferedWriter file, String indent, Section.UniqueData d, int ordinal)
    {
	try {

	    file.write(indent + "<" + xml_safe_name + ">"); file.newLine();

	    //d.print();

	    if (((Section)this).genericHeader()) {
		if (!writeHeader(file,indent+"  ", d)) {
		    //		    System.out.println(" ====> DOES NOT EXIST: "); // TODO: Should this occur???
		    //		    d.print();
		    //		    System.out.println();
		    return;
		}
	    }

	    /* Iterate through all this Sections contained items and
	       have these items write themselves to the xml file */
	    	   
	    for (Item item : items_contained) {

		if (item.type() == Item.COLUMN) {
		    // If COLUMN, then this column is a child of this section and does not need ordinal
		    item.write(file,indent,d,1);
		} else {
		    // GROUP.  Need ordinal.
		    item.write(file,indent,d,ordinal);
		}	
	    }

	    // Write the ordinal number for this data if there is a group in this section
	    if (hasGroup()) {
		file.write(indent + "  <row_number>" + ordinal + "</row_number>"); file.newLine();	    
	    }

	    file.write(indent + "</" + xml_safe_name + ">"); file.newLine();

	} catch (IOException e) {

	}
    }

    public boolean writeHeader(BufferedWriter file, String indent, Section.UniqueData d)
    {
	try {

	    // This is a fix for ordinals that do not start at 0 or 1.  If an item is deleted from the database,
	    // the ordinals do not get modified.  If item with ordinal 1 was deleted, the ordinals may then start
	    // at 2 or more.  A better method will be needed to get header information instead of rellying on ordinals
	    int ordinal = -1;
	    ColumnData column_data = null;

	    do {
		ordinal++;
		column_data = getDataPoint(d,ordinal);
	    } while ( (column_data == null) && (ordinal < 10) );

	    
	    if (column_data == null) {
		System.out.println("ERROR: no data for subject found when it should be there. WriteHeader()");
		d.print();
		    
		// Debug Section information:
		this.print_debug();

		ArrayList<Column> columns = this.getAllColumns();
		
		for (Column column : columns) {
		    System.out.println("Column Name: " + column.name());
		    column.printItemsWithData("  ");
		}

		//file.write(indent + "XX<study>"+Helper.cleanse(studyProtocol())+"</study>"); file.newLine();
		//file.write(indent + "XX<sid>"+ Helper.cleanse(d.sid)+"</sid>"); file.newLine();
		//file.write(indent + "XX<event>"+ Helper.cleanse(d.event_id)+"</event>"); file.newLine();
		    
		return false;		
	    }

	    file.write(indent + "<study>"+Helper.cleanse(studyProtocol())+"</study>"); file.newLine();
	    file.write(indent + "<study_site>"+Helper.cleanse(column_data.site())+"</study_site>"); file.newLine();
	    file.write(indent + "<sid>"+ Helper.cleanse(d.sid)+"</sid>"); file.newLine();
	    file.write(indent + "<study_event>"+ Helper.cleanse(d.event_id)+"</study_event>"); file.newLine();
	    file.write(indent + "<event_startdt>"+Helper.cleanse(column_data.eventStartDate())+"</event_startdt>"); file.newLine();
	    file.write(indent + "<crf_name>"+Helper.cleanse(column_data.crfName())+"</crf_name>"); file.newLine();
	    file.write(indent + "<crf_version>"+ Helper.cleanse(column_data.crfVersion())+"</crf_version>"); file.newLine();

	} catch (IOException e) {}

	return true;
    }

    public void writeMapFile(BufferedWriter file)
    {
	try {

	    String section_name = xmlName();
	    String section_description = xmlDescription();
	    
	    file.write("  <TABLE name=\""+section_name+"\">"); file.newLine();
	    file.write("    <TABLE-DESCRIPTION>"+ section_description + "</TABLE-DESCRIPTION>"); file.newLine();
	    file.write("    <TABLE-PATH syntax=\"XPath\">/OCEXPORT/TABLE/"+ section_name +"</TABLE-PATH>"); file.newLine();	    
	    
	    // Iterate over the items contained in this section
	    ListIterator itr = getAllColumns().listIterator();
	    
	    while (itr.hasNext()) {
		Column column = (Column)itr.next();
		column.writeToMapFile(file, section_name);
	    }

	    // TODO: Must be able to add generic columns to a section
	    if (hasGroup()) {
		file.write("    <COLUMN name=\"row_number\">"); file.newLine();
		file.write("      <PATH syntax=\"XPath\">/OCEXPORT/TABLE/"+section_name+"/row_number</PATH>"); file.newLine();
		file.write("      <DESCRIPTION>Row number of data in group</DESCRIPTION>"); file.newLine();
		file.write("      <TYPE>numeric</TYPE>"); file.newLine();
		file.write("      <DATATYPE>integer</DATATYPE>"); file.newLine();		
		file.write("      <FORMAT>BEST</FORMAT>"); file.newLine();
		file.write("    </COLUMN>"); file.newLine();		
	    }
	    
	    file.write("  </TABLE>"); file.newLine();
	}

	catch(IOException e) {}
    }

    public void printIDS()
    {
	for (String sid : ids) {
	    System.out.print(" " + sid);
	}
    }

    public void printItems()
    {
	printIDS();
    }

    // Getters
    public int type() { return Item.SECTION; }
    public boolean genericHeader() { return generic_header; }
    // Setters

    public void addId(String id)
    {
	ids.add(id);
    }


    // Debugging and Testing

    public static void main(String[] args)
    {

    }
}