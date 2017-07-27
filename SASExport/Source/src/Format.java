import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collection;
import java.util.Iterator;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


/**
 *
 *  Format
 *
 *  Contains a collection of CRFFormat's that represents all of the
 *  information required to export FMTLIB details to SAS
 *
 *
 **/
public class Format
{
    private boolean SPSS = false;
    private boolean DEBUG = false;

    // Both HashMap's below refer to same Label object
    HashMap<String,Label> formats;        // key is made from crf_version_id + openclinica's label
    HashMap<String,LabelManager> labels;  // key is made from openclinica label.

    private int fmtname_length = 1;  // These values are used in the MAP file.  Length's of longest items in this object
    private int start_length = 1;
    private int label_length = 1;

    public Format()
    {
	formats = new HashMap<String,Label>();
	labels = new HashMap<String,LabelManager>();	
    }

    public void setSPSS()
    {
	SPSS = true;
    }

    private String createKey(String crf_version_id, String label)
    {
	return crf_version_id.concat("<MULTIKEY>"+label);
    }

    public void addLabelMapping(String crf_version_id, String dtype, String label, String options, String values)
    {
	 // Debugging information
	if (DEBUG) {
	    System.out.println("In Format.addLabelMapping:");
	    System.out.println("crf_version_id: " + crf_version_id);
	    System.out.println("dtype:" + dtype);
	    System.out.println("label:" + label);
	    System.out.println("options:" + options);
	    System.out.println("values:" + values + "\n");
	}

	// Trim the whitespace from this label
	label = label.trim();

	// Combine the crf_version_id and label into a unique key
	String key = createKey(crf_version_id, label);
	
	// Try to retrieve this label from the labels hashmap
	LabelManager lm = labels.get(label);

	if (lm == null) {
	    if (DEBUG) {
		System.out.println("Could not find label in the collection of LabelManager's with name: " + label + "\nCreating new label for this item.");
	    }
		
	    // No label with this name (eg: yes_no, month, ...) was found in the hashmap
	    lm = new LabelManager(label);
	    labels.put(label,lm);
	} else {
	    if (DEBUG) {
		System.out.println("Found label: " + label);
	    }
	}

	// Pass these options and values to the label manager and store the Label object
	// that it returns to us with the key created from the crf and label name.
	Label new_label = lm.addLabel(dtype,options,values);
	formats.put(key, new_label);


	// Get the lengths for the map file
	// Determine size of this label
	int length = new_label.getLabel().length();
	int s_length = new_label.getOptionsLength();
	int l_length = new_label.getFormatWidth();

	if ((length) > fmtname_length)
	    fmtname_length = length;

	if ((s_length) > start_length)
	    start_length = s_length;

	if ((l_length) > label_length)
	    label_length = l_length;

	// TODO: DON'T KNOW IF THIS WORKS OR NOT
	if (label_length > start_length)
	    start_length = label_length;
	if (start_length > label_length)
	    label_length = start_length;

    }

    public Label label(String crf_version_id, String label)
    {
	return formats.get(createKey(crf_version_id, label));
    }

    public Label label(ArrayList<String> versions, String label)
    {
	Iterator itr = versions.listIterator();

	while(itr.hasNext()) {
	    String version = (String)itr.next();
	    String key = createKey(version,label);
	    Label a_label = formats.get(key);

	    if (a_label != null) {
		return a_label;
	    }
	}

	return null;
    }

    public void writeToMapFile(BufferedWriter file)
    {
	try {

	    file.write("  <TABLE name=\"fmtlib\">"); file.newLine();
	    
	    file.write("    <TABLE-PATH syntax=\"XPath\">/OCEXPORT/TABLE/fmtlib</TABLE-PATH>"); file.newLine();
	    file.write("    <COLUMN name=\"FMTNAME\">"); file.newLine();
	    file.write("      <PATH syntax=\"XPath\">/OCEXPORT/TABLE/fmtlib/FMTNAME</PATH>"); file.newLine();
	    file.write("      <TYPE>character</TYPE>"); file.newLine();
	    file.write("      <DATATYPE>string</DATATYPE>"); file.newLine();
	    file.write("      <LENGTH>" + fmtname_length + "</LENGTH>"); file.newLine();
	    file.write("    </COLUMN>"); file.newLine();

	    file.write("    <COLUMN name=\"START\">"); file.newLine();
	    file.write("      <PATH syntax=\"XPath\">/OCEXPORT/TABLE/fmtlib/START</PATH>"); file.newLine();
	    file.write("      <TYPE>character</TYPE>"); file.newLine();
	    file.write("      <DATATYPE>string</DATATYPE>"); file.newLine();
	    file.write("      <LENGTH>" + start_length + "</LENGTH>"); file.newLine();
	    file.write("    </COLUMN>"); file.newLine();

	    file.write("    <COLUMN name=\"LABEL\">"); file.newLine();
	    file.write("      <PATH syntax=\"XPath\">/OCEXPORT/TABLE/fmtlib/LABEL</PATH>"); file.newLine();
	    file.write("      <TYPE>character</TYPE>"); file.newLine();
	    file.write("      <DATATYPE>string</DATATYPE>"); file.newLine();
	    file.write("      <LENGTH>" + label_length + "</LENGTH>"); file.newLine();
	    file.write("    </COLUMN>"); file.newLine();

	    file.write("    <COLUMN name=\"TYPE\">"); file.newLine();
	    file.write("      <PATH syntax=\"XPath\">/OCEXPORT/TABLE/fmtlib/TYPE</PATH>"); file.newLine();
	    file.write("      <TYPE>character</TYPE>"); file.newLine();
	    file.write("      <DATATYPE>string</DATATYPE>"); file.newLine();
	    file.write("      <LENGTH>1</LENGTH>"); file.newLine();
	    file.write("    </COLUMN>"); file.newLine();

	    file.write("    <COLUMN name=\"HLO\">"); file.newLine();
	    file.write("      <PATH syntax=\"XPath\">/OCEXPORT/TABLE/fmtlib/HLO</PATH>"); file.newLine();
	    file.write("      <TYPE>character</TYPE>"); file.newLine();
	    file.write("      <DATATYPE>string</DATATYPE>"); file.newLine();
	    file.write("      <LENGTH>2</LENGTH>"); file.newLine();
	    file.write("    </COLUMN>"); file.newLine();
	    file.write("  </TABLE>"); file.newLine();

	} catch (IOException e) {}
    }

    public void writeToXMLFile(BufferedWriter file)
    {
	try {
	    file.write("  <TABLE>"); file.newLine();

	    Collection c = formats.values();
	    Iterator i = c.iterator();

	    Label.writeStandardNullFormats(file, "BESTNULL", "I", "INT");
	    Label.writeStandardNullFormats(file, "BESTNULL", "N", "INT");

	    // SPSS comes into play here
	    if (SPSS) {
		Label.writeStandardNullFormats(file, "CRFDATE", "I", "DATE");	    
		//Label.writeStandardNullFormats(file, "DATE9", "N", "DATE");	    
	    } else {
		Label.writeStandardNullFormats(file, "CRFDATE", "I", "DATE");	    
		Label.writeStandardNullFormats(file, "CRFDATE", "N", "DATE");	    
	    }

	    while (i.hasNext()) {
		Label label = (Label)i.next();
		label.writeXMLFile(file);
	    }	
	    
	    file.write("  </TABLE>"); file.newLine();

	} catch (IOException e) {}
    }

    public void showAllMappings()
    {
	Set s = formats.keySet();
	Iterator i = s.iterator();

        while (i.hasNext()) {
	    String key = (String)i.next();
	    Label label = formats.get(key);
	    System.out.println("Key: " + key);
	    label.print();
	    System.out.println();
	}
    }

    public void setDebug()
    {
	this.DEBUG = true;
    }

    public static void main(String[] args)
    {
	// Test
	Format sasFormat = new Format();

	sasFormat.setDebug();

	sasFormat.addLabelMapping("A","STR","sex","0,1","male,female");
	sasFormat.addLabelMapping("B","STR","sex","0,2","male,female");
	sasFormat.addLabelMapping("C","STR","sex","0,3","male,female");
	sasFormat.addLabelMapping("D","STR","sex","0,1","male,female");
	sasFormat.addLabelMapping("E","STR","sex","1,0","female,male");

	sasFormat.showAllMappings();

	// Write to file
	try {
	    BufferedWriter file = new BufferedWriter(new FileWriter("Formats.xml"));
	    sasFormat.writeToXMLFile(file);
	    file.close();
	} catch (IOException ioe) {
	    System.out.println("Some exception");
	}
    }
}