import java.util.HashMap;
import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

class Label
{
    private String label;   // This is the modified label for export
    private String options;
    private String values;
    private String dtype;
    private HashMap<String,String> mapping;

    private String label_type;
    private int format_width;  // The width of the longest value.  Not all labels may need this value.
    private int options_length; // length of the largest option   : used for FMTLIB map file

    boolean is_written;
    boolean DEBUG = false;

    public Label(String dtype, String label, String options, String values)
    {
	this.label = label;
	this.options = options;
	this.values = values;
	this.is_written = false;
	this.dtype = dtype;
	mapping = new HashMap<String,String>();

	String[] split_options = options.split(",");
	String[] split_values = values.split(",");

	/* Obsolete code 
	if (label.startsWith("$")) {
	    label_type = "C";
	} else {
	    label_type = "N";
	}
	*/
	// Replaced with
	if (dtype.equals("ST")) {
	    // This is a string datatype as determined from OpenClinica's database
	    label_type = "C";
	} else {
	    // Not a string, therefore is one of "INT", "REAL", "ISODATE", "PDATE"
	    label_type = "N";
	}

	int o = split_options.length;
	int v = split_values.length;

	if (o != v) {
	    System.out.println("ERROR: There are a different number of options compared to values.\nLabel:\t"+label); 
	    System.out.println("Options:\t" + options + "\nValues:\t" + values);
	    return;
	}

	// Strip the blank options and labels.  This can occur when a drop down selection box is
	// utilized in a crf.  Sometimes the first option is a blank.  If both option and value
	// is blank after stripping whitespace, then remove that option and value
	for (int i = 0; i < split_options.length; i++) {
	    /*	    if ((split_options[i].trim().length() > 0) && (split_values[i].trim().length() > 0)) { */
	    if (split_options[i].trim().length() > 0) {
		if (split_values[i].trim().length() > 0) {
		    mapping.put(split_options[i].trim(), split_values[i].trim());
		}
	    }
	}

	// Determine the length of the longest value
	format_width = 0;
	options_length = 0;
	int o_length, v_length;
	for (int i=0; i < split_values.length; i++) {
	    v_length = split_values[i].length();
	    if (v_length > format_width) {
		format_width = v_length;		
	    }
	    o_length = split_options[i].length();
	    if (o_length > options_length) {
		options_length = o_length;		
	    }
	}

    }

    public String getLabel()      { return label; }
    public int getOptionsLength() { return options_length; }
    public int getFormatWidth()   { return format_width; }
    public String getDtype()      { return dtype; }

    // Checks if this options and values mapping is represented in this label object
    // This is made more complicated because the ordering of these values and options
    // should not matter.  Eg: 0,1 => Yes,No should be the same as 1,0 => No,Yes
    public boolean contains(String options, String values)
    {
	// Split the options and values
	String[] opt = options.split(",");
	String[] val = values.split(",");

	// TODO: There must be a better way of counting the non-null values?
	int size = 0;
	for (int i=0; i < opt.length; i++) {
	    if (( opt[i].length() > 0) && ( val[i].length() > 0)) {
		size++;
	    }
	}

	// First check is that the sizes of these objects are the same
	if (size != mapping.size()) {
	    if (DEBUG) {
		System.out.println("Label.java: Not same 1");
	    }
	    return false;
	}

	// Step through each of these and compare to this objects options and values
	for (int i=0; i < opt.length; i++) {

	    // If the length is 0, then this is a blank option and value.  TODO: Could this happen?
	    if ((opt[i].length() > 0) && (val[i].length() > 0)) {

		String v = mapping.get(opt[i].trim());
		if (v == null) {
		    // This mapping does not exist for this object

		    if (DEBUG) {
			System.out.println("Label.java: Not same 2");
		    }
		    return false;
		}
		
		if (!v.equals(val[i].trim())) {
		    // This mapping has the value, but a different option
		    if (DEBUG) {
			System.out.println("Label.java: Not same 3");
			System.out.println("i: " + i);
			System.out.println("v: " + v);
			System.out.println("incoming option: " + opt[i]);
			System.out.println("incoming value:  " + val[i]);
			System.out.println("stored value:    " + v);
		    }
		    return false;
		}
	    }
	}

	//return (this.options.equals(options) && this.values.equals(values));	    

	return true;
    }



    public static void writeStandardNullFormats(BufferedWriter file, String FMTNAME, String TYPE, String dtype)
    {
	try {

	    if (TYPE.equals("I")) {
		file.write("    <fmtlib>"); file.newLine();
		file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		file.write("      <START>NA</START>"); file.newLine();
		file.write("      <LABEL>.A</LABEL>"); file.newLine();
		file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		file.write("    </fmtlib>"); file.newLine();		
		
		file.write("    <fmtlib>"); file.newLine();
		file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		file.write("      <START>NASK</START>"); file.newLine();
		file.write("      <LABEL>.D</LABEL>"); file.newLine();
		file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		file.write("    </fmtlib>"); file.newLine();		
		
		file.write("    <fmtlib>"); file.newLine();
		file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		file.write("      <START>UNK</START>"); file.newLine();
		file.write("      <LABEL>.U</LABEL>"); file.newLine();
		file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		file.write("    </fmtlib>"); file.newLine();		
		
		file.write("    <fmtlib>"); file.newLine();
		file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		file.write("      <START>NI</START>"); file.newLine();
		file.write("      <LABEL>.I</LABEL>"); file.newLine();
		file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		file.write("    </fmtlib>"); file.newLine();		
		
		file.write("    <fmtlib>"); file.newLine();
		file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		file.write("      <START>ASKU</START>"); file.newLine();
		file.write("      <LABEL>.K</LABEL>"); file.newLine();
		file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		file.write("    </fmtlib>"); file.newLine();		
		
		file.write("    <fmtlib>"); file.newLine();
		file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		file.write("      <START>OTH</START>"); file.newLine();
		file.write("      <LABEL>.O</LABEL>"); file.newLine();
		file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		file.write("    </fmtlib>"); file.newLine();		
		
		file.write("    <fmtlib>"); file.newLine();
		file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		file.write("      <START>NP</START>"); file.newLine();
		file.write("      <LABEL>.P</LABEL>"); file.newLine();
		file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		file.write("    </fmtlib>"); file.newLine();		
		
		if (dtype.equals("INT") || dtype.equals("REAL")) {
		    file.write("    <fmtlib>"); file.newLine();
		    file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		    file.write("      <START>**OTHER**</START>"); file.newLine();
		    file.write("      <LABEL>BEST10.</LABEL>"); file.newLine();
		    file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		    file.write("      <HLO>OF</HLO>"); file.newLine();
		    file.write("    </fmtlib>"); file.newLine();		
		} else if (dtype.equals("ISODATE")) {
		    file.write("    <fmtlib>"); file.newLine();
		    file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		    file.write("      <START>**OTHER**</START>"); file.newLine();
		    file.write("      <LABEL>IS8601DA</LABEL>"); file.newLine();
		    file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		    file.write("      <HLO>OF</HLO>"); file.newLine();
		    file.write("    </fmtlib>"); file.newLine();		
		} else {
		    file.write("    <fmtlib>"); file.newLine();
		    file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		    file.write("      <START>**OTHER**</START>"); file.newLine();
		    file.write("      <LABEL>mmddyy10.</LABEL>"); file.newLine();
		    file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		    file.write("      <HLO>OF</HLO>"); file.newLine();
		    file.write("    </fmtlib>"); file.newLine();		
		}
	    }

	    // Swap the order of START and LABEL
	    else {
		file.write("    <fmtlib>"); file.newLine();
		file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		file.write("      <START>.A</START>"); file.newLine();
		file.write("      <LABEL>NA</LABEL>"); file.newLine();
		file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		file.write("    </fmtlib>"); file.newLine();		
		
		file.write("    <fmtlib>"); file.newLine();
		file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		file.write("      <START>.D</START>"); file.newLine();
		file.write("      <LABEL>NASK</LABEL>"); file.newLine();
		file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		file.write("    </fmtlib>"); file.newLine();		
		
		file.write("    <fmtlib>"); file.newLine();
		file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		file.write("      <START>.U</START>"); file.newLine();
		file.write("      <LABEL>UNK</LABEL>"); file.newLine();
		file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		file.write("    </fmtlib>"); file.newLine();		
		
		file.write("    <fmtlib>"); file.newLine();
		file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		file.write("      <START>.I</START>"); file.newLine();
		file.write("      <LABEL>NI</LABEL>"); file.newLine();
		file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		file.write("    </fmtlib>"); file.newLine();		
		
		file.write("    <fmtlib>"); file.newLine();
		file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		file.write("      <START>.K</START>"); file.newLine();
		file.write("      <LABEL>ASKU</LABEL>"); file.newLine();
		file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		file.write("    </fmtlib>"); file.newLine();		
		
		file.write("    <fmtlib>"); file.newLine();
		file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		file.write("      <START>.O</START>"); file.newLine();
		file.write("      <LABEL>OTH</LABEL>"); file.newLine();
		file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		file.write("    </fmtlib>"); file.newLine();		
		
		file.write("    <fmtlib>"); file.newLine();
		file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		file.write("      <START>.P</START>"); file.newLine();
		file.write("      <LABEL>NP</LABEL>"); file.newLine();
		file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		file.write("    </fmtlib>"); file.newLine();		

		if (dtype.equals("INT") || dtype.equals("REAL")) {

		} else {
		    file.write("    <fmtlib>"); file.newLine();
		    file.write("      <FMTNAME>" + FMTNAME + "</FMTNAME>"); file.newLine();
		    file.write("      <START>**OTHER**</START>"); file.newLine();
		    file.write("      <LABEL>IS8601DA</LABEL>"); file.newLine();
		    file.write("      <TYPE>" + TYPE + "</TYPE>"); file.newLine();
		    file.write("      <HLO>OF</HLO>"); file.newLine();
		    file.write("    </fmtlib>"); file.newLine();		
		}

	    }

	} catch (IOException e) {}
    }

    public void writeXMLFile(BufferedWriter file)
    {
	// Only write this object once.  Many objects may be mapped to this object.
	if (!is_written) {

	    is_written = true;

	    try {
		
		Set s = mapping.keySet();
		Iterator i = s.iterator();

		// Write the NULL value mappings
		if (!dtype.equals("ST")) {
		    writeStandardNullFormats(file,label,"I",dtype);
		    writeStandardNullFormats(file,label,"N",dtype);
		}

		if (label_type.equals("C")) {
		    while (i.hasNext()) {
			String key = (String)i.next();		
			
			file.write("    <fmtlib>"); file.newLine();
			file.write("      <FMTNAME>" + label + "</FMTNAME>"); file.newLine();
			file.write("      <START>" + key + "</START>"); file.newLine();
			file.write("      <LABEL>" + mapping.get(key) + "</LABEL>"); file.newLine();
			file.write("      <TYPE>C</TYPE>"); file.newLine();
			file.write("    </fmtlib>"); file.newLine();

			file.write("    <fmtlib>"); file.newLine();
			file.write("      <FMTNAME>" + label + "</FMTNAME>"); file.newLine();
			file.write("      <START>" + mapping.get(key) + "</START>"); file.newLine();
			file.write("      <LABEL>" + key + "</LABEL>"); file.newLine();
			file.write("      <TYPE>J</TYPE>"); file.newLine();
			file.write("    </fmtlib>"); file.newLine();
		    }
		} else {
		    while (i.hasNext()) {
			String key = (String)i.next();		
			
			file.write("    <fmtlib>"); file.newLine();
			file.write("      <FMTNAME>" + label + "</FMTNAME>"); file.newLine();
			file.write("      <START>" + mapping.get(key) + "</START>"); file.newLine();
			file.write("      <LABEL>" + key + "</LABEL>"); file.newLine();
			file.write("      <TYPE>I</TYPE>"); file.newLine();
			file.write("    </fmtlib>"); file.newLine();
			
			file.write("    <fmtlib>"); file.newLine();
			file.write("      <FMTNAME>" + label + "</FMTNAME>"); file.newLine();
			file.write("      <START>" + key + "</START>"); file.newLine();
			file.write("      <LABEL>" + mapping.get(key) + "</LABEL>"); file.newLine();
			file.write("      <TYPE>N</TYPE>"); file.newLine();
			file.write("    </fmtlib>"); file.newLine();
		    }
		}
		
	    } catch (IOException e) {}
	}
    }

    public void print()
    {
	System.out.println("Label: " + "==>"+label+"<==");
	System.out.println("Options: " + "==>"+options+"<==");
	System.out.println("Values: " + "==>"+values+"<==");

	Set s = mapping.keySet();
	Iterator i = s.iterator();

	while (i.hasNext()) {
	    String key = (String)i.next();
	    System.out.println(key + " => " + mapping.get(key));
	}
    }

    public static void main(String[] args)
    {
	Label lab = new Label("empty", "Stephen", "1,2,3", "medication,none,tons");
	lab.print();

	if (lab.contains("str,int,dex","19,21,18")) {
	    System.out.println("It has these values already");
	} else {
	    System.out.println("FAILED");
	}

	int width = lab.getFormatWidth();
	System.out.println("Format Width is: "+width);
    }
}