import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Iterator;
// SQL and Drivers for database
import java.sql.*;
import java.sql.DriverManager;
// XML
import org.xml.sax.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.sax.*; 

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.io.PrintStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

class Export 
{
    String url;
    String db;
    String driver;
    String user;
    String pass;
    Connection con;
    Statement statement;
    Integer study_id;
    Format formats;
    UserInterface ui;
    String export_file_name;
    String map_file_name;
    Study main_study;
    Crf crf;
    Table subjectTable;
    Table discrepancyTable;
    Table seTable; // Subject Event table
    Table subjectCRFTable; // Subject CRF table
    String study_unique_id = "";
    BufferedWriter file;
    boolean DEBUG = false;
    GlobalFlags GF;

    public Export(String[] args)
    {
	main_study = new Study("MAIN STUDY AT TOP", "main_id");

	url = "jdbc:postgresql://127.0.0.1:5432/";
	db = "openclinica";
	driver = "org.postgresql.Driver";
	user = "clinica";
	pass = "clinica";

	formats = new Format();
	main_study.setFormatObject(formats);

	ui = new UserInterface();	
	ui.parseCommandLine(args);
	ui.showOutputDetails();

	initializeDatabaseConnection(ui);

	ArrayList<String> params = ui.getStudy(con);

	study_id = Integer.parseInt(params.get(0));
	
	if (export_file_name == null) {
	    export_file_name = params.get(1);
	}

	if (map_file_name == null) {
	    map_file_name = params.get(2);
	}

	GF = GlobalFlags.getInstance();
	FileOutputStream fos = null;
	if (GF.output_to_file) {
	    // Send output to file
	    
	    try {
		fos = new FileOutputStream("OUTPUT.TEST");
		System.setOut(new PrintStream(fos));
	    } catch(IOException e) {}
	}

	runExport();

	try {
	    con.close();
	} catch (SQLException e) {}


	if (fos != null) {
	    try {
		fos.close();
	    } catch (IOException e) {}
	}
    }

    public void runExport()
    {
	//formats.setDebug();
	if (ui.isGenerateSPSS()) {
	    formats.setSPSS();
	}

	System.out.println("\n\nRetrieving study metadata");
	getCrfMetadata();  // Used to construct the fmtlib xml tags

	file = beginXMLFile();

	getStudyMetadata();                     // Construct the study structure in memory
	main_study.collapseCrfVersions();       // Collapse crf_versions into one version for export
	main_study.createUniqueSectionNames(); 	// Step through the Item structure and create unique xml section names for the export
	    

	//formats.showAllMappings();

	// Uncomment to print a list of crf's in this study
	//main_study.printCrfs(); // Debugging
	
	System.out.println("Creating subject table");
	createSubjectTable();	// Create the subject table
	writeXMLFile(file, subjectTable);  // writes subject table data

	System.out.println("Creating discrepancy notes table");
	createDiscrepancyTable(); 
	writeXMLFile(file, discrepancyTable);		

	System.out.println("Creating subject events table");
	createSubjectEventTable(); 
	writeXMLFile(file, seTable);		

	System.out.println("Creating subject crf table");
	createSubjectCRFTable(); 
	writeXMLFile(file, subjectCRFTable);		

	System.out.println("Retrieving study item data");
	getData(file);  // Pull data from the database for all crf sections
	System.out.println("Writing study item data to file");

	// Map file needs to be written after the getData() call, otherwise, lengths will not be set correctly
	writeMapFile();
	writeXMLFile(file, null);          // writes the format data

	endXMLFile(file);

	// Alternate for CRF table names
	//main_study.printCrfsContained();

	System.out.println("Complete");
	System.out.println("Files generated:   " + map_file_name + "\n                   " + export_file_name);
    }

    public void getData(BufferedWriter file)
    {
	// Pull data items for every section in the main_study
	Iterator sections_itr = main_study.sections().iterator();

	while (sections_itr.hasNext()) {
	    Section section = (Section)sections_itr.next();
	    //section.printIDS();

	    // Get all section id's in collapsed section
	    Iterator section_ids_itr = section.getIds();

	    //System.out.print(" Section: " + section.xmlName() + "   ==> ");
	    //	    section.printIDS();
	    //System.out.println();
		
	    // Get all data for this section
	    getDataItems(section_ids_itr, section);
	    
	    // Write this section to disk
	    //writeXMLFile(file, section);
	    
	    // Remove this section from memory
	    //System.out.println("REMOVING SECTION FROM MEMORY");
	    //sections_itr.remove();
	    
	    //	    main_study.printCrfs();
	}
    }

    public void getStudyMetadata() 
    {
	try {
	    /* Query the database for all crf's and crf_versions that are part of this study */
	    Statement st = con.createStatement();
	    String query = 
		"SELECT DISTINCT s.unique_identifier, c.name, v.crf_id, v.crf_version_id, v.name AS crf_version_name " +
		"FROM crf c, event_definition_crf e, crf_version v, study s, study_event_definition se " +
		"WHERE v.crf_id = c.crf_id AND " +
		"      e.status_id != 5 AND e.status_id != 7 AND " + 
		"      v.status_id != 5 AND v.status_id != 7 AND " + // Must consider status of crf_version as well
		"      c.crf_id = e.crf_id AND " +
		"      se.study_event_definition_id = e.study_event_definition_id AND " +
		"      se.study_id = s.study_id AND " +
		"      (s.study_id = " + study_id + " OR s.parent_study_id = " + study_id + ")" +
		"ORDER BY v.crf_id";
	    ResultSet res = st.executeQuery(query);

	    /* Step through each crf found in above query and get all sections that belong to it */
	    while(res.next()) {
		String protocol = res.getString("unique_identifier");
		String crf_name = res.getString("name");
		String crf_id = res.getString("crf_id");
		String crf_version_id = res.getString("crf_version_id");
		String crf_version_name = res.getString("crf_version_name");
		
		// Create the Crf and CrfVersion objects
		Crf crf = main_study.getCrfWithID(crf_id);		
		if (crf == null) {
		    crf = new Crf(crf_id, crf_name);
		    main_study.addItem(crf);
		    main_study.protocol(protocol);
		}

		CrfVersion crf_version = crf.getCrfVersionWithID(crf_version_id);
		if (crf_version == null) {
		    crf_version = new CrfVersion(crf_version_id, crf_version_name);
		    crf.addItem(crf_version);
		}

		/* Get all sections on this crf */
		ResultSet sections = getSections(crf_version_id);

		if (DEBUG) {
		    ui.println("\nCRF name: " + crf_name + "\n  crf_id: " + crf_id + "\n  ver_id: " + crf_version_id);
		}

		/* Create and populate Section Objects for each section found in the given crf version. */
		while (sections.next()) {
		    String label = sections.getString("label").toLowerCase(); // SAS is not case sensitive
		    String title = sections.getString("title");
		    String section_id = sections.getString("section_id");
		    String version_name = sections.getString("name");

		    // SAS is not case sensitive, so lower case all table names
		    

		    if (DEBUG) {
			System.out.println(" Section (" + section_id + "): " + label);
		    }

		    // Add this section to the crf_version container
		    Section section = new Section(section_id, label, title, Section.CREATE_GENERIC_HEADER);
		    crf_version.addItem(section);

		    // Populate this section object with metadata
		    addColumnMetadata(section_id, section);
		}
	    }

	    //st.close();

	} catch (SQLException e) {}
    }

    private void writeMapFile()
    {
	if (ui.isGenerateMapFile()) {
	    // Create the SAS map file
	    BufferedWriter mapFile;
	    try {
		mapFile = new BufferedWriter(new FileWriter(map_file_name));		
		mapFile.write("<SXLEMAP description=\"Maps OpenClinica Data Export to SAS data structures.\" name=\"OCEXPORT\" version=\"1.2\">");
		mapFile.newLine();		
		main_study.writeMapFile(mapFile);
		subjectTable.writeMapFile(mapFile);
		discrepancyTable.writeMapFile(mapFile);
		seTable.writeMapFile(mapFile);
		subjectCRFTable.writeMapFile(mapFile);
		formats.writeToMapFile(mapFile);
		mapFile.write("</SXLEMAP>");
		mapFile.newLine();
		mapFile.close();
	    } catch (IOException e) {
	    }
	}	
    }

    private BufferedWriter beginXMLFile()
    {
	// Create the xml export file
	try {
	    BufferedWriter xmlFile;
	    xmlFile = new BufferedWriter(new FileWriter(export_file_name));
	    xmlFile.write("<?xml version=\"1.0\"?>"); xmlFile.newLine();
	    xmlFile.write("<OCEXPORT>"); xmlFile.newLine();	    
	    return xmlFile;
	} catch (IOException e) {
	    
	}

	return null;
    }

    private void writeXMLFile(BufferedWriter xmlFile, ContainerItem item)
    {
	if (item == null) {
	    System.out.println("Writing formats to .xml file");
	    formats.writeToXMLFile(xmlFile);
	} else {
	    System.out.println("Writing " + item.name() + " to .xml file");
	    item.writeToXmlFile(xmlFile,"  ");
	}
    }

    private void endXMLFile(BufferedWriter xmlFile)
    {
	try {
	    xmlFile.write("</OCEXPORT>"); xmlFile.newLine();
	    xmlFile.close();
	} catch (IOException e) {
	}
    }

    public ResultSet getSections(String crf_version_id)
    {
	try {
	    /* Get all sections on this crf */
	    Statement st = con.createStatement();
	    String query = "SELECT s.title, s.label, s.section_id, c.name " +
		"FROM section s, crf_version c " +
		"WHERE s.crf_version_id = " + crf_version_id + " AND s.crf_version_id = c.crf_version_id";
	    ResultSet sections = st.executeQuery(query);

	    return sections;

	} catch (SQLException x) {
	    ui.println("SQLException: " +x);
	}

	return null;
    }

    /**
     *
     *  createSubjectTable()
     *
     *  Finds all subjects that are part of the study and creates an export table
     *  with this information
     *
     **/
    private void createSubjectTable()
    {
	// Create the subject table
	subjectTable = new Table("subjects", "subjects");  
	subjectTable.description("Study Subject Details");

	Section subject_section = new Section("-1","subjects", "Study Subjects", Section.NO_HEADER);
	subjectTable.addItem(subject_section);

	// Add the columns to the subject table
	subject_section.addItem(new Column("sid", "Study subject id", "text", -1, "ST"));
	subject_section.addItem(new Column("secondary_label", "Study subject secondary label", "text", -1, "ST"));
	subject_section.addItem(new Column("person_id", "Person id", "text", -1, "ST"));
	subject_section.addItem(new Column("study", "The study that the subject is enrolled in", "text", -1, "ST"));
	subject_section.addItem(new Column("study_site", "The site that the subject is enrolled in", "text", -1, "ST"));
	subject_section.addItem(new Column("group", "The group this subject belongs to", "text", -1, "ST"));
	subject_section.addItem(new Column("group_class", "The group class this group belongs to", "text", -1, "ST"));
	subject_section.addItem(new Column("gender", "Subjects gender", "text", -1, "ST"));
	subject_section.addItem(new Column("date_of_birth", "Subjects date of birth", "date", -1, "ISODATE"));
	subject_section.addItem(new Column("date_created", "Subject creation date", "date", -1, "ISODATE"));
	subject_section.addItem(new Column("enrollment_date", "Subject enrollment date", "date", -1, "ISODATE"));

	try {
	    // I need to get the unique_identifier for the parent_study only and apply it to all sites
	    Statement st = con.createStatement();
	    String query = 
		"SELECT distinct s1.unique_identifier " + 
		"FROM study s1 LEFT OUTER JOIN study s2 ON (s1.study_id = s2.parent_study_id) " +
		"WHERE (s1.study_id = " + study_id + " OR s2.study_id = " + study_id + ")";
	    ResultSet studies = st.executeQuery(query);

	    if (studies.next()) {
		study_unique_id = studies.getString("unique_identifier");	    
	    }
	    
	    query = 
		"SELECT ss.label, ss.secondary_label, s.unique_identifier, s.date_of_birth, s.gender, s.date_created, ss.enrollment_date, st.name " +
		"FROM study st, study_subject ss, subject s " +
		"WHERE ss.study_id = st.study_id AND " + 
		"      (st.study_id = " + study_id + " OR st.parent_study_id = " + study_id + ") AND " +
		"      s.subject_id = ss.subject_id and " + 
		"      ss.status_id != 5 and ss.status_id != 7 " +
		"GROUP BY s.subject_id, ss.label, s.unique_identifier, ss.secondary_label, s.date_of_birth, s.gender, s.date_created, ss.enrollment_date, st.unique_identifier, st.name";
	    ResultSet res = st.executeQuery(query);

	    /* Step through each subject found in this study */
	    while(res.next()) {
		String sid = res.getString("label");
		String secondary_label = res.getString("secondary_label");
		String person_id = res.getString("unique_identifier");
		String date_of_birth = res.getString("date_of_birth");
		String gender = res.getString("gender");
		String date_created = res.getString("date_created");
		String enrollment_date = res.getString("enrollment_date");
		String site = res.getString("name");
		
		subjectTable.insertData(sid, "sid", sid);
		subjectTable.insertData(sid, "secondary_label", secondary_label);
		subjectTable.insertData(sid, "person_id", person_id);
		subjectTable.insertData(sid, "study", study_unique_id);
		subjectTable.insertData(sid, "study_site", site);
		subjectTable.insertData(sid, "gender", gender);
		subjectTable.insertData(sid, "date_of_birth", date_of_birth);
		subjectTable.insertData(sid, "date_created", date_created);
		subjectTable.insertData(sid, "enrollment_date", enrollment_date);
	    }

	    // Get group information for the subjects
	    query = "select ss.label, sg.name as group, sg.description, sgc.name as group_class " +
		"from study s, study_subject ss, subject_group_map sgm, study_group sg, study_group_class sgc " +
		"where ss.study_subject_id = sgm.study_subject_id and " +
		"      sgm.study_group_id = sg.study_group_id and " +
		"      sg.study_group_class_id = sgc.study_group_class_id and " +
		"      (s.study_id = " + study_id + " OR s.parent_study_id = " + study_id + ") AND " +
		"      ss.study_id = s.study_id and " +
		"      ss.status_id != 5 and ss.status_id != 7 " +
		"group by ss.label, sg.name, sgc.name, sg.description";
	    res = st.executeQuery(query);

	    /* Step through each subject found in this study */
	    while(res.next()) {
		String sid = res.getString("label");
		String group = res.getString("group");
		String group_class = res.getString("group_class");
		String group_description = res.getString("description");
		
		subjectTable.insertData(sid, "group", group);
		subjectTable.insertData(sid, "group_class", group_class);
	    }

	    subject_section.sectionCompleted();  // TODO: Not very easy to use....

	} catch (SQLException x) {
	    ui.println("SQLException: " + x);
	}
    }

    public void createDiscrepancyTable()
    {
	// Create the discrepancy table
	discrepancyTable = new Table("discrepancies", "discrepancies");  
	discrepancyTable.description("Discrepancy Note Details");

	Section discrepancy_section = new Section("-1","discrepancies", "discrepancies", Section.NO_HEADER);
	discrepancyTable.addItem(discrepancy_section);

	// Add the columns to the discrepancy table
	discrepancy_section.addItem(new Column("study", "Study", "text", -1, "ST"));
	discrepancy_section.addItem(new Column("sid", "Study subject ID", "text", -1, "ST"));
	discrepancy_section.addItem(new Column("study_site", "The site that the subject is enrolled in", "text", -1, "ST"));
	discrepancy_section.addItem(new Column("crf_name", "CRF name", "text", -1, "ST"));
	discrepancy_section.addItem(new Column("thread_id", "The thread id for this discrepancy note.", "text", -1, "ST"));
	discrepancy_section.addItem(new Column("parent_thread_id", "The parent thread id for this discrepancy note.", "text", -1, "ST"));
	discrepancy_section.addItem(new Column("study_event", "Study event", "text", -1, "ST"));
	//RR
	discrepancy_section.addItem(new Column("event_crf_id", "event_crf_id", "text", -1, "ST"));
	discrepancy_section.addItem(new Column("event_startdt", "Event start date", "date", -1, "ISODATE"));
	discrepancy_section.addItem(new Column("user_name", "User who created this discrepancy", "text", -1, "ST"));
	discrepancy_section.addItem(new Column("description", "Discrepancy note description", "text", -1, "ST"));
	discrepancy_section.addItem(new Column("resolution_status", "Resolution status of the discrepancy", "text", -1, "ST"));
	discrepancy_section.addItem(new Column("detailed_notes", "Detailed notes about the discrepancy", "text", -1, "ST"));
	discrepancy_section.addItem(new Column("date_created", "Discrepancy note creation date", "date", -1, "ISODATE"));
	discrepancy_section.addItem(new Column("item_value", "Item value causing discrepancy", "text", -1, "ST"));
	discrepancy_section.addItem(new Column("column_name", "Column name from crf", "text", -1, "ST"));

	try {
	    Statement st = con.createStatement();
	    String query = 

		"select st.name as site, c.name as crf_name, se.date_start as event_startdt, " + 
		"       d.discrepancy_note_id as dn_id, d.discrepancy_note_id as thread_id, " +
		"       d.parent_dn_id as parent_thread_id, u.user_name, ss.label as sid, " +
		"       sed.name as event, d.description, rs.name as resolution_status, " +
		"       d.detailed_notes, d.date_created, id.value, i.name as column_name " + 
		//RR
		"       e.event_crf_id " + 

		"from   user_account u, study_subject ss, event_crf e, item_data id, item i, " +
		"       study_event se, study_event_definition sed, study st, " +
		"       discrepancy_note d, discrepancy_note_type dnt, resolution_status rs, dn_item_data_map im, " + 
		"       crf c, crf_version cv " +
		"where  (st.study_id = " + study_id + " or st.parent_study_id = " + study_id + ") and " +
		"       cv.crf_version_id = e.crf_version_id and " +
		"       cv.crf_id = c.crf_id and " +
		"       d.study_id = st.study_id and " +
		"       dnt.discrepancy_note_type_id = d.discrepancy_note_type_id and " +
		"       rs.resolution_status_id = d.resolution_status_id and " +
		"       im.discrepancy_note_id = d.discrepancy_note_id and " +
		"       id.item_data_id = im.item_data_id and " +
		"       i.item_id = id.item_id and " + 
		"       e.event_crf_id = id.event_crf_id and " +
		"       e.study_subject_id = ss.study_subject_id and " +
		"       e.study_event_id = se.study_event_id and " +
		"       se.study_event_definition_id = sed.study_event_definition_id and " +
		"       ss.status_id != 5 and ss.status_id != 7 and " +
		"       cv.status_id != 5 and cv.status_id != 7 and " +  // Must check crf_version status as well
		"       u.user_id = d.owner_id";

	    ResultSet res = st.executeQuery(query);
	    int note_num = 0;

	    /* Step through each note found in this study */
	    while(res.next()) {
		String sid = res.getString("sid");
		String site = res.getString("site");
		String crf_name = res.getString("crf_name");
		String dn_id = res.getString("dn_id");
		String thread_id = res.getString("thread_id");
		String parent_thread_id = res.getString("parent_thread_id");
		String event = res.getString("event");
		String event_startdt = res.getString("event_startdt");
		String user_name = res.getString("user_name");
		String description = res.getString("description");
		String resolution_status = res.getString("resolution_status");
		String detailed_notes = res.getString("detailed_notes");
		String date_created = res.getString("date_created");
		String item_value = res.getString("value");
		String column_name = res.getString("column_name");
		//RR
		String event_crf_id = res.getString("event_crf_id");
		
		if (parent_thread_id == null) {
		    parent_thread_id = thread_id;
		}

		discrepancyTable.insertData(""+note_num, "sid", sid);
		discrepancyTable.insertData(""+note_num, "study", study_unique_id);
		discrepancyTable.insertData(""+note_num, "study_site", site);
		discrepancyTable.insertData(""+note_num, "crf_name", crf_name);
		discrepancyTable.insertData(""+note_num, "thread_id", thread_id);
		discrepancyTable.insertData(""+note_num, "parent_thread_id", parent_thread_id);
		discrepancyTable.insertData(""+note_num, "study_event", event);
		//RR
		discrepancyTable.insertData(""+note_num, "event_crf_id", event_crf_id);
		discrepancyTable.insertData(""+note_num, "event_startdt", event_startdt);
		discrepancyTable.insertData(""+note_num, "user_name", user_name);
		discrepancyTable.insertData(""+note_num, "description", description);
		discrepancyTable.insertData(""+note_num, "resolution_status", resolution_status);
		discrepancyTable.insertData(""+note_num, "detailed_notes", detailed_notes);
		discrepancyTable.insertData(""+note_num, "date_created", date_created);
		discrepancyTable.insertData(""+note_num, "item_value", item_value);
		discrepancyTable.insertData(""+note_num, "column_name", column_name);


		note_num++;
	    }

	    discrepancy_section.sectionCompleted();

	} catch (SQLException x) {
	    ui.println("SQLException: " + x);
	}
    }


    public void createSubjectEventTable()
    {
	// Create the table
	seTable = new Table("subject_events", "subject_events");  
	seTable.description("Subject Event Details");

	Section se_section = new Section("-1","subject_events", "subject_events", Section.NO_HEADER);
	seTable.addItem(se_section);

	// Add the columns to the discrepancy table
	se_section.addItem(new Column("study", "Study", "text", -1, "ST"));
	se_section.addItem(new Column("study_site", "The site that the subject is enrolled in", "text", -1, "ST"));
	se_section.addItem(new Column("sid", "Study subject ID", "text", -1, "ST"));
	se_section.addItem(new Column("study_event", "Study event", "text", -1, "ST"));
	//RR
	se_section.addItem(new Column("event_crf_id", "event_crf_id", "text", -1, "ST"));
	se_section.addItem(new Column("event_startdt", "Event start date", "date", -1, "DATETIME"));
	se_section.addItem(new Column("event_enddt", "Event end date", "date", -1, "DATETIME"));
	se_section.addItem(new Column("event_status", "Event status", "text", -1, "ST"));

	try {
	    Statement st = con.createStatement();
	    String query = 
		"select s.name as site, " +
		"       ss.label as sid, " +
		"       sed.name as event, " +
		"       se.date_start as event_startdt, " +
		"       se.date_end as event_enddt, " +
		"       sta.name as event_status " +
		//RR
		"       ,e.event_crf_id " +
		"from   study s, " +
		"       study_subject ss, " +
		"       study_event se, " +
		"       study_event_definition sed, " +
		"       subject_event_status sta " +
		//RR
		"       event_crf e " +
		"where  (s.study_id = " + study_id + " or s.parent_study_id = " + study_id + ") and " +
		"       s.study_id = ss.study_id and " +
		"       se.study_subject_id = ss.study_subject_id and " +
		"       sed.study_event_definition_id = se.study_event_definition_id and " +
		"       se.subject_event_status_id = sta.subject_event_status_id and " + 
		"       ss.status_id != 5 and ss.status_id != 7 " +
		//RR
		"       e.study_event_id = se.study_event_id" +
		"group by sid, site, event, event_startdt, event_enddt, event_status" /*RR*/+ ",e.event_crf_id";

	    ResultSet res = st.executeQuery(query);
	    int note_num = 0;

	    /* Step through each note found in this study */
	    while(res.next()) {
		String sid = res.getString("sid");
		String site = res.getString("site");
		String event = res.getString("event");
		//RR
		String event_crf_id = res.getString("event_crf_id");
		String event_startdt = res.getString("event_startdt");
		String event_enddt = res.getString("event_enddt");
		String event_status = res.getString("event_status");
		
		seTable.insertData(""+note_num, "study", study_unique_id);
		seTable.insertData(""+note_num, "sid", sid);
		seTable.insertData(""+note_num, "study_site", site);
		seTable.insertData(""+note_num, "study_event", event);
		//RR
		seTable.insertData(""+note_num, "event_crf_id", event);
		seTable.insertData(""+note_num, "event_startdt", event_startdt);
		seTable.insertData(""+note_num, "event_enddt", event_enddt);
		seTable.insertData(""+note_num, "event_status", event_status);

		note_num++;
	    }

	    se_section.sectionCompleted();

	} catch (SQLException x) {
	    ui.println("SQLException: " + x);
	}
    }

    public void createSubjectCRFTable()
    {
	// Create the table
	subjectCRFTable = new Table("subject_crf", "subject_crf");  
	subjectCRFTable.description("Subject CRF Details");

	Section se_section = new Section("-1","subject_crf", "subject_crf", Section.NO_HEADER);
	subjectCRFTable.addItem(se_section);

	// Add the columns to the discrepancy table
	se_section.addItem(new Column("study", "Study", "text", -1, "ST"));
	se_section.addItem(new Column("study_site", "The site that the subject is enrolled in", "text", -1, "ST"));
	se_section.addItem(new Column("sid", "Study subject ID", "text", -1, "ST"));
	se_section.addItem(new Column("study_event", "Study event", "text", -1, "ST"));
	//RR
	se_section.addItem(new Column("event_crf_id", "event_crf_id", "text", -1, "ST"));
	se_section.addItem(new Column("event_startdt", "Event start date", "date", -1, "DATETIME"));
	se_section.addItem(new Column("crf_name", "CRF Name", "text", -1, "ST"));
	se_section.addItem(new Column("crf_version","CRF Version", "text", -1, "ST"));
	se_section.addItem(new Column("date_created", "Creation date", "date", -1, "ISODATE"));
	se_section.addItem(new Column("date_completed", "Completion date", "date", -1, "DATETIME"));
	se_section.addItem(new Column("event_crf_status", "Event CRF status", "text", -1, "ST"));


	try {
	    Statement st = con.createStatement();
	    String query = 
		"select s.name as site, " +
		"       ss.label as sid, " +
		"       sed.name as event, " +
		"       se.date_start as event_startdt, " +
		"       c.name as crf_name, " +
		"       cv.name as crf_version, " +
		"       e.date_created as date_created, " +
		"       e.date_completed as date_completed, " +
		"       status.name as event_crf_status, " +
		// RR added according to event_crf_id
		"       v.event_crf_id as event_crf_id " +
		"from   study s, " +
		"       study_subject ss, " +
		"       study_event se, " +
		"       event_crf e, " +
		"       study_event_definition sed, " +
		"       crf_version cv, " +
		"       status, " +
		"       crf c " +
		"where  (s.study_id = " + study_id + " or s.parent_study_id = " + study_id + ") and " +
		"       s.study_id = ss.study_id and " +
		"       se.study_subject_id = ss.study_subject_id and " +
		"       se.study_event_id = e.study_event_id and " +
		"       sed.study_event_definition_id = se.study_event_definition_id and " +
		"       e.crf_version_id = cv.crf_version_id and " + 
		"       cv.crf_id = c.crf_id and " +
		"       e.status_id = status.status_id and " +
		"       e.status_id != 7 and e.status_id != 5 AND " +
		"       cv.status_id != 7 and cv.status_id != 5 " + // Must check version as well
		"group by sid, site, event, crf_name, crf_version, event_startdt, e.date_created, date_completed, event_crf_status"/*RR*/+",v.event_crf_id";

	    ResultSet res = st.executeQuery(query);
	    int note_num = 0;

	    /* Step through each note found in this study */
	    while(res.next()) {
		String sid = res.getString("sid");
		String site = res.getString("site");
		String event = res.getString("event");
		String event_startdt = res.getString("event_startdt");
		String crf_name = res.getString("crf_name");
		String crf_version = res.getString("crf_version");
		String date_created = res.getString("date_created");
		String date_completed = res.getString("date_completed");
		String crf_status = res.getString("event_crf_status");
		// RR added according to event_crf_id 
		String event_crf_id = res.getString("event_crf_id");

		//if (crf_status.equals("unavailable")) {
		//    crf_status = "completed";
		//}
		
		subjectCRFTable.insertData(""+note_num, "study", study_unique_id);
		subjectCRFTable.insertData(""+note_num, "sid", sid);
		subjectCRFTable.insertData(""+note_num, "study_site", site);
		subjectCRFTable.insertData(""+note_num, "study_event", event);
		// RR added according to event_crf_id 
		subjectCRFTable.insertData(""+note_num, "event_crf_id", event_crf_id);
		subjectCRFTable.insertData(""+note_num, "event_startdt", event_startdt);
		subjectCRFTable.insertData(""+note_num, "crf_name", crf_name);
		subjectCRFTable.insertData(""+note_num, "crf_version", crf_version);
		subjectCRFTable.insertData(""+note_num, "date_created", date_created);
		subjectCRFTable.insertData(""+note_num, "date_completed", date_completed);
		subjectCRFTable.insertData(""+note_num, "event_crf_status", crf_status);
		

		note_num++;
	    }

	    se_section.sectionCompleted();

	} catch (SQLException x) {
	    ui.println("SQLException: " + x);
	}
    }

    public void getDataItems(Iterator section_ids_itr, ContainerItem section)
    {
	// DEBUG
	//boolean PRINT_THIS_ONE = false;
	// END DEBUG
	
	String build = "";
	while (section_ids_itr.hasNext()) {
	    String id = (String)section_ids_itr.next();
	    build = build + id;

	    // DEBUG
	    //if (id.equals("1445") || id.equals("1446") || id.equals("1447")) {
	    //PRINT_THIS_ONE = true;
	    //}
	    // END DEBUG

	    if (section_ids_itr.hasNext()) {
		build = build + ",";
	    }
	}

	//System.out.println("\nSections: " + build);


	// This query was changed as shown below due to errors in the TIPS study export. The TIPS study was showing data
	// in sections that did not belong. I determined that the "item" table is used for many different unrelated
	// section items that share the same name. To fix this, I made sure that the "item_data" pulled belongs to the proper section
	try {
	    String query = 
		"SELECT s.name AS site, " +
		"       ss.label AS sid, " +
		"       sed.name AS event, " +
		"       se.date_start AS event_startdt, " +
		"       crf.name AS crf_name, " +
		"       c.name AS crf_version, " +
		"       i.name AS item_name, " +
		"       d.value, " +
		"       d.ordinal, " +  // Used to determine which order this item is in if it belongs to a group
		// RR added according to event_crf_id
		"       e.event_crf_id " + 
		"FROM   item_data d, " +
		"       item i, " +
		"       event_crf e, " + 
		"       study_event se, " +
		"       study_event_definition sed, " +
		"       study s, " +
		"       crf_version c, " +
		"       crf, " +
		"       study_subject ss " +
		"       , section sec " + // ADDED DUE TO TIPS ERROR MAY 06 2010
		"WHERE  d.item_id = i.item_id AND " +
		"       sec.section_id IN (" + build + ") AND " + // TIPS MAY 06 2010
		"       sec.crf_version_id = c.crf_version_id AND " + // TIPS MAY 06 2010
		"       d.event_crf_id = e.event_crf_id AND " +
		"       e.study_event_id = se.study_event_id AND " +
		"       se.study_event_definition_id = sed.study_event_definition_id AND " +
		"       (s.study_id = sed.study_id OR s.parent_study_id = sed.study_id) and " +
		"       ss.study_id = s.study_id AND " +
		"       e.crf_version_id = c.crf_version_id AND " +
		"       c.crf_id = crf.crf_id AND " +
		"       e.study_subject_id = ss.study_subject_id AND " +
		"       d.value not like '' AND " +
		"       d.status_id != 5 and d.status_id != 7 and " +
		"       c.status_id != 5 and c.status_id != 7 and " +  // Checking crf_version as well
		"       i.item_id = ANY ( SELECT DISTINCT item_id FROM item_form_metadata WHERE section_id IN (" + build + ")) " + 
		"ORDER BY sid, item_name, event";

	    
	    /*
	    // DEBUGGING	    
	    if (PRINT_THIS_ONE)
		System.out.println("QUERY: " + query + "\n");	   
	    // DEBUGGING
	    */

	    Statement ps = con.createStatement();

	    ps.setFetchSize(10000);
	    ResultSet res = ps.executeQuery(query);

	    int resultSetSize = 0;
	    int subjects_count_before_write = 5000;
	    int count = 0;
	    String previous_sid = null;
	    while(res.next()) {
		resultSetSize++;
		String item_name = res.getString("item_name");
		String sid = res.getString("sid");
		String value = res.getString("value");
		String event_startdt = res.getString("event_startdt");
		String site = res.getString("site");
		String event = res.getString("event");		
		int ordinal = res.getInt("ordinal");
		String crf_version = res.getString("crf_version");
		String crf_name = res.getString("crf_name");
		// RR added according to event_crf_id
		String event_crf_id = res.getString("event_crf_id");

		// Check if this sid is a new one. This would signify that a complete subject has been pulled from the DB and this 
		// section could be written to disk if need be
		if (resultSetSize == 1) {

		    //System.out.println("New SID (" + sid + ") for this section (" + section.name() + ")");
		    //System.out.println("CRF name: " + section.parentContainer().parentContainer().name());

		    // Store this sid in the crf that this section belongs to
		    Crf crf = (Crf)section.parentContainer().parentContainer();
		    boolean r = crf.addSID(sid);

		    // Debugging
		    /*if (!r) {
			System.out.println("Already contained this sid");
		    } else  {
			System.out.println("SID added!");
		    }
		    System.out.println("\n");
		    */


		    previous_sid = sid;
		} else if (!sid.equals(previous_sid)) {
		    previous_sid = sid;
		    if (count > subjects_count_before_write) {
			section.writeToXmlFile(file,"  ");
			count = 0;
		    }
		}
		
		section.insertData(sid, site, event,/*RR*/event_crf_id, crf_version, crf_name, event_startdt, item_name, value, ordinal  );
	    }

	    ((Section)section).sectionCompleted();
	    // Write to HD and remove subject from this section...
	    section.writeToXmlFile(file,"  ");

	    //ps.close();
	}
	
	catch(SQLException x) {
	    ui.println("SQLException in getDataItems(): " + x);
	}
    }

    public void addColumnMetadata(String section_id, Section section)
    {
	try {
	    /* Get all the item_name's metadata which will make up the columns in the crf_version for this section */
	    String query = 
		"SELECT g.item_group_id, g.repeat_max, " + 
		"       m.crf_version_id, m.ordinal, " + 
		"       i.name, i.description, t.code, " +
		"       r.label, r.response_type_id, r.options_values, r.options_text, " +
		"       ig.name AS group_name " +
		"FROM item i, item_group_metadata g, item_form_metadata m, " +
		"      item_data_type t, response_set r, item_group ig " +
		"WHERE i.item_id = m.item_id AND " +
		"      i.item_data_type_id = t.item_data_type_id AND " +
		"      m.response_set_id = r.response_set_id AND " +
		"      i.item_id = g.item_id AND " +
		"      g.crf_version_id = m.crf_version_id AND " +
		"      g.item_group_id = ig.item_group_id AND " +
		"      m.section_id = " + section_id +
		"ORDER BY ordinal";
	    
	    Statement statement = con.createStatement();
	    ResultSet res = statement.executeQuery(query);
	   
	    // Step through each item retrieved in this section
	    while (res.next()) {
		String item_name = res.getString("name");
		String description = res.getString("description");
		String dtype = res.getString("code");
		String format_label = res.getString("label");
		String crf_version_id = res.getString("crf_version_id");
		String response_type = res.getString("response_type_id");
		int group_id = res.getInt("item_group_id");
		String group_name = res.getString("group_name");
		int repeat_max = res.getInt("repeat_max");

		// Hack until Formats and Labels are refactored
		// Get the width of the label object for this column
		Label label = formats.label(crf_version_id,format_label);
		int width = -1;

		if (label != null) {
		    width = label.getOptionsLength();
		}
		    

		Column new_column = null;
		MultiSelect multi_select = null;

		// Check if this column is a multiselect or checkbox item.
		if (response_type.equals("3") || response_type.equals("7")) {

		    String values = res.getString("options_values");
		    String text = res.getString("options_text");
		    multi_select = new MultiSelect(item_name, description, format_label, values, text);
		}

		// When repeat_max is > 1, this data belongs to a group item
		if (repeat_max > 1) {

		    // Group item metadata to add
		    String id = "<group>" + group_id;
		    Group group = (Group)section.getItemWithID(id);
		    if (group == null) {
			// This Item does not exist yet
			group = new Group(id, group_name);
			section.addItem(group);
		    }
		    
		    // Is this a multiselect/checkbox item?
		    if (multi_select != null) {
			group.addItem(multi_select);
		    } else {
			new_column = new Column(item_name, description, format_label, width, dtype);
			group.addItem(new_column);
		    }
		    
		} else {
		    // Normal column metadata to add that is not in a group
		    if (multi_select != null) {
			section.addItem(multi_select);
		    } else {
			new_column = new Column(item_name, description, format_label, width, dtype);
			section.addItem(new_column);
		    }
		}		
	    }

	    //statement.close();
	}

	catch(SQLException x) {
	    ui.println("SQLException in addColumnMetadata(): " + x);
	}
    }
    
    /** 
     *  getCrfMetadata()
     *------------------------
     *  Get the options_text and options_labels for every crf that is part of this study
     *
     **/
    public void getCrfMetadata()
    {
	try {
	    // Get number of crf's in this study	    
	    Statement st = con.createStatement();
	    String query = 
		"SELECT COUNT(distinct e.crf_id) " + 
		"FROM event_definition_crf e, study_event_definition se, study s " + 
		"WHERE e.study_event_definition_id = se.study_event_definition_id and " + 
		"      (se.study_id = s.study_id or se.study_id = s.parent_study_id) and " + 
		"      e.status_id != 5 and e.status_id != 7 and " + // ADDED MAR 3 2010
		"      se.study_id = " + study_id;
	    ResultSet res = st.executeQuery(query);
	    
	    res.next();
	    ui.println("\n\nNumber of CRF's in this study: " + res.getString("count") + " StudyID = " + study_id);
	    
	    /* Get the metadata for all crf's in this study
	     * This metadata contains the options_text and options_values that are
	     * needed to fill in the <FMT...> data in the xml file
	     */
	    query = 
		"SELECT dt.code, r.version_id, r.label, r.options_text, r.options_values " +
		"FROM response_set r, event_definition_crf e, study_event_definition sed, item_form_metadata im, item i, item_data_type dt, study s, section se, crf_version cv, crf c " +
		"WHERE (s.study_id = " + study_id + " OR s.parent_study_id = " + study_id + ") AND " +
		"      sed.study_id = s.study_id AND " +		// checking study_id vs sed instead of e due to db issue
		"      sed.study_event_definition_id = e.study_event_definition_id AND " + // work around db problem
		"      e.crf_id = c.crf_id AND " +
		"      e.status_id != 5 AND e.status_id != 7 AND " + 
		"      cv.status_id != 5 AND cv.status_id != 7 AND " + // Must consider status of crf_version as well
		"      c.crf_id = cv.crf_id AND " +
		"      se.crf_version_id = cv.crf_version_id AND " +
		"      se.section_id = im.section_id AND " +
		"      r.response_set_id = im.response_set_id AND " + 
		"      im.item_id = i.item_id AND " +
		"      dt.item_data_type_id = i.item_data_type_id AND " +
		"      r.options_text LIKE '%,%' " +               // Only want multi value columns
		"GROUP BY dt.code, r.version_id, r.label, r.options_text, r.options_values " + 
		"ORDER BY label";
	    res = st.executeQuery(query);	
	    
	    //System.out.println("Query to get label info: " + query);

	    ArrayList <String> dtypes = new ArrayList<String>();
	    ArrayList <String> version = new ArrayList<String>();
	    ArrayList <String> labels = new ArrayList<String>();
	    ArrayList <String> options_text = new ArrayList<String>();
	    ArrayList <String> options_values = new ArrayList<String>();
	    
	    while (res.next()) {
		// Get crf_version_id
		version.add( res.getString("version_id"));

		// Get the datatype
		dtypes.add( res.getString("code"));

		// Retrieve the current label from the result set
		labels.add( res.getString("label") );
		
		// Get the options_text and options_values from this row
		options_text.add( res.getString("options_text") );
		options_values.add( res.getString("options_values") );
	    }
	    
	    /* All label and option data has been pulled from the database.
	       Put these labels and options into the Format object */
	    for (int i=0; i < labels.size(); i++) {
		String crf_version = version.get(i);
		String dtype = dtypes.get(i);
		String label = labels.get(i);
		String options = options_text.get(i);
		String values = options_values.get(i);

		formats.addLabelMapping(crf_version,dtype,label,values,options);
	    }	

	    //st.close();

	} catch(SQLException x) {
	    ui.println("SQLException: " + x);
	}
    }

    private void initializeDatabaseConnection(UserInterface ui)
    {
	/* Load the driver for the database connection */
	try {
	    Class.forName(driver);
	}
	catch(Exception x) {
	    ui.println("Could not find driver " + driver +". Is it on the CLASSPATH?");
	}
	ui.println("Postgresql driver loaded");

	// Get connection to the postgresql database
	try {	   


	    url = "jdbc:postgresql://" + ui.getDatabaseUrl();
	    url = url + ":" + ui.getDatabasePort() + "/";
	    db = ui.getDatabaseName();
	    user = ui.getUserName();
	    pass = ui.getPassword();
	    con = DriverManager.getConnection(url+db, user, pass);
	    con.setAutoCommit(false);  // Needed for cursor use in retrieving data from database
	    statement = con.createStatement();
	    export_file_name = ui.getExportFileName();
	    map_file_name = ui.getMapFileName();
	    
	}
	catch(SQLException x) {
	    ui.println("Couldn't get connection to database " + db + " on " + url);
	    ui.println("Exception: " + x);
	    return;
	}

	ui.println("Successful connection to database " + db + " on " + url);
    }

    public static void main(String[] args)
    {
	Export main = new Export(args);
    }
}
