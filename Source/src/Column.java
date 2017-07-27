import java.util.HashMap;
import java.util.TreeMap;
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
public class Column extends Item
{
    private String type;
    private String datatype;
    private String fmt_type;
    private String format;
    private String format_label;
    int format_width;
    int length = 1;
    boolean write_to_xml = true;

    private ArrayList<ColumnData> column_data;
    private TreeMap<String,ColumnData> column_data_tree;

    // HACK
    private HashMap<String,Integer> max_ordinals = new HashMap<String,Integer>();
    
    public ColumnData getDataPoint(Section.UniqueData d, int ordinal)
    {
	// Find this data point in the tree first...TODO: improve this
	String key = generateKey(d.sid, d.event_id,/*RR*/ d.event_crf_id, d.crf_version, ordinal);
	ColumnData data1 = column_data_tree.get(key);

	if (data1 == null) {
	    // Sometimes, a data item can have an ordinal of 0 instead of 1
	    if (ordinal == 1) {
		key = generateKey(d.sid, d.event_id,/*RR*/ d.event_crf_id, d.crf_version, 0);
		data1 = column_data_tree.get(key);
	    }
	}
		
	if (data1 != null) {
	    // Remove this item from the tree
	    column_data_tree.remove(key);
	    return data1;
	} else {

	}
	
	// data not found in the tree, so search incrementally
	for (ColumnData data : column_data) {
	    if (data.studySubjectId().equals(d.sid) && data.eventId().equals(d.event_id) /*RR*/&& data.event_crf_id().equals(d.event_crf_id) && data.crfVersion().equals(d.crf_version)) {
		if (data.ordinal() == ordinal) {
		    return data;
		}

		else if (ordinal == 1) {
		    // Can sometimes be a 0...not a 1
		    if (data.ordinal() == 0) {
			return data;
		    }
		}
	    }
	}

	return null;
    }

    /* Used while copying one column's data into another when two crf_versions are being collapsed into one. */
    public Column(Column col)
    {
	super("-1",col.name());

	//	System.out.println("COLLAPSING COLUMN: " + col.name());


	this.description = col.description();
	this.datatype = col.datatype();
	this.fmt_type = col.fmtType();
	this.format = col.format();

	column_data = new ArrayList<ColumnData>();
	column_data.addAll(col.data());

	// TODO: Copy this as above
	column_data_tree = new TreeMap<String,ColumnData>();

	max_ordinals = new HashMap<String,Integer>();
    }

    /* Only being used for columns I am creating, not columns from the database */
    public Column(String name, String description, String fmt_type, String datatype, int length, int format_width, String format)
    {
	super("-1",name);

	this.name = name;
	this.description = description;
	this.fmt_type = fmt_type;
	this.datatype = datatype;
	this.length = length;
	this.format_width = format_width;
	this.format = format;

	this.write_to_xml = false;

	column_data = new ArrayList<ColumnData>();
	column_data_tree = new TreeMap<String,ColumnData>();

	max_ordinals = new HashMap<String,Integer>();	
    }

    /* Creates columns from DB data */
    public Column(String name, String description, String format, int width, String dtype)
    {
	super("-1",name);
	
	this.description = description;
	this.format = format;
	this.format_label = format;
	this.length = -1;

	// Set the datatype.		
	fmt_type = "numeric";	
	if (dtype.equals("ST")) {
	    datatype = "string";
	    fmt_type = "character";
	    this.format = "$CHAR";
	    length = 8;// Min length for string is 8 due to fmtlib's for strings
	} else if (dtype.equals("INT")) {
	    datatype = "integer";	    
	    this.format = "BESTNULL";
	} else if (dtype.equals("REAL")) {
	    datatype = "double";
	    this.format = "BESTNULL";
	} else if (dtype.equals("DATE")) {
	    datatype = "date";
	    format_width = 10;
	    this.format = "CRFDATE";
	} else if (dtype.equals("ISODATE")) {
	    datatype = "date";
	    format_width = 10;
	    this.format = "IS8601DA";
	} else if (dtype.equals("DATETIME")) {
	    datatype = "datetime";
	    format_width = 19;	
	    this.format = "IS8601DT";    
	} else if (dtype.equals("MULTISELECT")) {  // TODO: likely not needed
	    // This is a multiselect item
	    this.format = format;
	    format_width = -9999;
	    datatype = "numeric";
	} else {	    
	    
	    if (dtype.equals("PDATE")) { // New for OC 3.x.x  -- Partial Date format
		//System.out.println("** Found PDATE object **");
	    } else if (dtype.equals("FILE")) { // New for OC 3.x.x -- File type as URL
		//System.out.println("** Found FILE object **");
	    } else {
		System.out.println("** Found an UNKNOWN datatype **: " + dtype);
	    }
	    datatype = "string";
	    fmt_type = "character";
	    this.format = "$CHAR";
	    length = -2;
	}

	format_width = width;

	column_data = new ArrayList<ColumnData>();
	column_data_tree = new TreeMap<String,ColumnData>();
	max_ordinals = new HashMap<String,Integer>();


    }

    public String key(String study_subject, String event,/*RR*/String event_crf_id, String crf_version)
    {
	return study_subject + "<>" + event /*RR*/+ "<>" + event_crf_id /*RR*/+ "<>" + crf_version ;
    }

    public String generateKey(String study_subject, String event,/*RR*/String event_crf_id, String crf_version, int ordinal)
    {
	return study_subject + "<>" + event /*RR*/+ "<>" + event_crf_id /*RR*/+ "<>" + crf_version + "<>" + ordinal;
    }

    public void insertData(String study_subject, String event,/*RR*/String event_crf_id, String crf_version, String crf_name, String site, String event_startdt, String value, int ordinal)
    {
	ColumnData data = new ColumnData(this, study_subject, event,/*RR*/event_crf_id, crf_version, crf_name, site, event_startdt, value, ordinal);

	// Store the max ordinal, which represents how many repeat events have been "repeated" for this subject on this section
	Integer max_ordinal = max_ordinals.get(key(study_subject,event,/*RR*/event_crf_id,crf_version));

	if (max_ordinal == null) {
	    max_ordinals.put(key(study_subject,event,/*RR*/event_crf_id,crf_version), new Integer(ordinal));
	} else {       
	    if (ordinal > max_ordinal.intValue()) {
		max_ordinals.put(key(study_subject,event,/*RR*/event_crf_id,crf_version),new Integer(ordinal));
	    }
	}

	column_data.add(data);
	String key = generateKey(study_subject, event,/*RR*/event_crf_id, crf_version, ordinal);
	//column_data_tree.put(key, data);  // TODO: Speed improvement

	if (value != null) {
	    if (datatype.equals("string")) {
		// Check if the length of this string is greater than the largest length stored in this column
		if ((value.length()) > length) {
		    length = value.length();
		}
	    }
	}

	/*
	if (ordinal != 1) {
	    System.out.println("insertData() : INSERTING DATA TO COLUMN: " + name());
	    System.out.println("study_subject: " + study_subject);
	    System.out.println("event        : " + event );
	    System.out.println("crf_version  : " + crf_version);
	    System.out.println("crf_name     : " + crf_name);
	    System.out.println("site         : " + site);
	    System.out.println("event_startdt: " + event_startdt);
	    System.out.println("value        : " + value);
	    System.out.println("ordinal      : " + ordinal);
	    System.out.println();
	}
	*/
	
    }

    // Used for a table with no crf's.  Generic table.
    public void insertData(String study_subject, String value)
    {
	ColumnData data = new ColumnData(this, study_subject, value);
	column_data.add(data);

	String key = generateKey(study_subject, "-", "-",/*RR*/"-", 0);
	//column_data_tree.put(key, data);  // TODO: Speed improvement

	if (value != null) {
	    if (datatype.equals("string")) {
		// Check if the length of this string is greater than the current format_width
		if ((value.length()) > length) {
		    length = value.length();
		}
	    }
	}
    }

    public void eraseSubjectData(Section.UniqueData d)
    {       
	ArrayList<ColumnData> removeThese = new ArrayList<ColumnData>();

	for (ColumnData data : column_data) {
	    if (data.studySubjectId().equals(d.sid) && data.eventId().equals(d.event_id) && data.crfVersion().equals(d.crf_version)/*RR*/&& data.event_crf_id().equals(d.event_crf_id)) {
		removeThese.add(data);
	    }
	}

	// This is a little odd...
	for (ColumnData again : removeThese) {
	    column_data.remove(again);
	}
    }

    public void write(BufferedWriter file, String indent, Section.UniqueData d, int ordinal)
    {
	if (!write_to_xml)
	    return;

	try {
	    file.write(indent + "  <" + xml_safe_name + ">");

	    ColumnData data = getDataPoint(d,ordinal);

	    if (data != null) {
		if (data.value() != null) {
		    file.write(Helper.cleanse(data.value()));
		}
	    } else {
		//		System.out.println("data was null : " + ordinal);
	    }

	    file.write("</" + xml_safe_name + ">"); file.newLine();

	} catch (IOException e) {}
    }

    public ArrayList<ColumnData> data()
    {
	return column_data;
    }

    public int getDepth(Section.UniqueData d)
    {
	Integer max = max_ordinals.get(key(d.sid,d.event_id,/*RR*/ d.event_crf_id,d.crf_version));

	if (max == null) {
	    return 1;
	} else {
	    return max.intValue();
	}
    }

    public void addData(ArrayList<ColumnData> new_data)
    {
	column_data.addAll(new_data);
    }

    // Get an arraylist of subject id's that have data in this column
    public ArrayList<String> subjects()
    {
	ListIterator data_itr = column_data.listIterator();
	ArrayList<String> subjects = new ArrayList<String>();

	while(data_itr.hasNext()) {
	    ColumnData data = (ColumnData)data_itr.next();

	    String ssid = data.studySubjectId();
	    if ( !(subjects.contains(ssid)) ) {
		subjects.add(ssid);
	    }
	}

	System.out.print("Column: " + name() + "  contains: ");
	ListIterator i = subjects.listIterator();
	while (i.hasNext()) {
	    String s = (String)i.next();
	    System.out.print(" "+s);
	}
	System.out.println();

	return subjects;
    }

    public void writeToMapFile(BufferedWriter file, String section_name)
    {

	// TODO: Debugging
	/*
	System.out.println("Section name: " + section_name);
	System.out.println("COLUMN name=" + xmlName());
	System.out.println("DESCRIPTION: " + xmlDescription());
	System.out.println("TYPE " + fmtType());
	System.out.println("DATATYPE " + datatype());
	System.out.println("Length of? " + length());
	//System.out.println("FORMAT: " + label.getLabel());
	System.out.println("format(): " + format());
	System.out.println("DONE\n\n");
	*/

	try {
	    file.write("    <COLUMN name=\"" + xmlName() + "\">"); file.newLine();
	    file.write("      <PATH syntax=\"XPath\">/OCEXPORT/TABLE/"+section_name+"/"+ xmlName() +"</PATH>"); file.newLine();
	    file.write("      <DESCRIPTION>"+ xmlDescription() +"</DESCRIPTION>"); file.newLine();
	    file.write("      <TYPE>"+ fmtType() +"</TYPE>"); file.newLine();
	    file.write("      <DATATYPE>"+ datatype() +"</DATATYPE>"); file.newLine();

	    if (datatype.equals("string")) {
		file.write("      <LENGTH>"+ length +"</LENGTH>"); file.newLine();
	    }
	   
 	    // Get the format label for this object if it exists
	    Format formats = getFormatObject();
	    GlobalFlags GF = GlobalFlags.getInstance();

	    // FORMATS
	    if (formats != null) {

		Label label = formats.label(crfVersions(),format_label);

		if (label != null) {
		    file.write("      <FORMAT>"+ label.getLabel() +"</FORMAT>"); file.newLine();
		} else {

		    if (format().equals("CRFDATE") || format().equals("BESTNULL") ) {
			// This is not pretty...better way needed
			if (GF.spss) {

			    if (format().equals("CRFDATE")) {
				file.write("      <FORMAT width=\"9\">DATE</FORMAT>"); file.newLine();
			    } else {
				file.write("      <FORMAT ndec=\"2\" width=\"10\">BEST</FORMAT>"); file.newLine();
			    }
			} else {
			    file.write("      <FORMAT width=\"10\">"+ format() +"</FORMAT>"); file.newLine();
			}

		    } else {
			if (GF.spss && format().equals("IS8601DT")) {			    
			    file.write("      <FORMAT width=\"14\">DATETIME</FORMAT>"); file.newLine();
			} else {
			    file.write("      <FORMAT>"+ format() +"</FORMAT>"); file.newLine();
			}
		    }
		}
	    } else {
		// If the format is $CHAR, must append the length. There is a bug in enterprise SAS that
		// requires this length to be specified.
		// REVERSED changes above by commenting out the // format_string
		String format_string = format();

		if (format_string.equals("$CHAR")) {
		    // format_string += length; 
		}


		file.write("      <FORMAT>"+ format_string +"</FORMAT>"); file.newLine();

	    }

	    // INFORMATS
	    if (datatype().equals("date")) {
		file.write("      <INFORMAT width=\"10\">"+ format() +"</INFORMAT>"); file.newLine();
	    } else if (datatype().equals("date")) {
		file.write("      <INFORMAT width=\"10\">IS8601DA</INFORMAT>"); file.newLine();
	    } else if (datatype().equals("datetime")) {
		file.write("      <INFORMAT width=\"19\">"+ format() +"</INFORMAT>"); file.newLine();
	    } else if (datatype().equals("integer") || datatype().equals("double")) {
		file.write("      <INFORMAT>"+ format() +"</INFORMAT>"); file.newLine();
	    }
	    
	    file.write("    </COLUMN>"); file.newLine();

	    // System.out.println("** Complete **");
	}

	catch(IOException e) {}
    }

    // SETTERS

    // GETTERS
    public int type()          { return Item.COLUMN; }
    public String fmtType()    { return fmt_type; }
    public String datatype()   { return datatype; }
    public int formatWidth()   { return format_width; }
    public int length()        { return length; }

    public String format()
    {
	// Get the label from the format object if it exists
	return format;
    }





    // DEBUGGING AND TESTING
    public int getNumberDataPoints()
    {
	return column_data.size();
    }

    public void printItems(String space)
    {
	int number_data = getNumberDataPoints();
       
	if (number_data > 0) {
	    System.out.println(space + name + " ==> " + xmlName() + " #data: " + getNumberDataPoints());
	}
    }

    public void printItemsWithData(String space)
    {
	printItems(space);
	
	ArrayList<ColumnData> cdata = this.data();

	for (ColumnData data : cdata) {
	    System.out.println("\nDATA POINT");
	    System.out.println("----------");
	    System.out.println(space+space+"Value: " + data.value());
	    System.out.println(space+space+"EventID: " + data.eventId());
		//RR
		System.out.println(space+space+"event_crf_id: " + data.event_crf_id());
	    System.out.println(space+space+"studySubjectId: " + data.studySubjectId());
	    System.out.println(space+space+"crfVersion: " + data.crfVersion());
	    System.out.println(space+space+"crfName: " + data.crfName());
	    System.out.println(space+space+"eventStartDate: " + data.eventStartDate());
	    System.out.println(space+space+"site: " + data.site());
	    System.out.println(space+space+"ordinal: " + data.ordinal());
	}
    }


    public static void main(String[] args)
    {

    }
}