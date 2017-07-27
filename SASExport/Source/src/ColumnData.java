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
 *  The actual data retrieved from OpenClinica. A ColumnData object represents a single
 *  piece of data from a section on a crf in OpenClinica. Each piece of data is associated
 *  with an event and a study subject, among other things.
 *
 **/
public class ColumnData
{
    private String study_subject_id;
    private String event_id;
    private String event_startdt;
    private String site;
    private String crf_name;
    private String crf_version;
    private int ordinal;  // Used for group information
    private String data;

    public Column column;

    public ColumnData(Column column, String subject,  String value)
    {
	this.column = column;

	// Column data for generic table
	this.study_subject_id = subject;
	this.data = value;

	// Set defaults for the rest
	event_id = "0";
	crf_version = "0";
	site = "0";
	event_startdt = "0";
	crf_name = "0";
	ordinal = 1;
    }

    public ColumnData(Column column, String subject, String event, String crf_version, String crf_name, String site, String event_startdt, String value, int ordinal)
    {
	this.column = column;
	this.data = value;
	event_id = event;
	this.crf_version = crf_version;
	this.crf_name = crf_name;
	study_subject_id = subject;
	this.site = site;
	this.event_startdt = event_startdt;
	this.ordinal = ordinal;
    }

    // SETTERS
    public void value(String data)             { this.data = data; }
    public void eventId(String event)          { event_id = event; }
    public void studySubjectId(String subject) { study_subject_id = subject; }

    // GETTERS
    public String value()          { return data; }
    public String eventId()        { return event_id; }
    public String studySubjectId() { return study_subject_id; }

    public String crfVersion()     { return crf_version; }
    public String crfName()        { return crf_name; }
    public String eventStartDate() { return event_startdt; }
    public String site()           { return site; }
    public int ordinal()           { return ordinal; }


    // DEBUGGING AND TESTING
    public static void main(String[] args)
    {

    }
}