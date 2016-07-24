import java.sql.*;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.io.*;

class UserInterface
{
    public String host = null;
    public String port = null;
    public String study_oid = null;
    public String study_uid = null;
    public String study_name = null;
    public String study_id = null;
    public String user = null;
    public String password = null;
    public String database = null;
    public String mapFile = null;
    public String xmlFile = null;
    public boolean nomap = false;
    public boolean spss = false;
    public boolean output_to_file = false;

    public UserInterface()    {}

    public boolean isGenerateMapFile() { return !nomap; }
    public boolean isGenerateSPSS() { return spss; }

    public void println(String line)   { System.out.println(line); }

    public void parseCommandLine(String[] args)
    {
	int current_arg = 0;
	int number_args = args.length;

	while (current_arg < number_args) {
	    // All arguments come in pairs
	    String arg = args[current_arg];

	    // Check that the first char in this argument is the "-"
	    if (!arg.startsWith("-",0)) {
		// Illegal command line argument
		System.out.println("Unknown command: " + arg);
		displayHelp();
	    }

	    if (arg.equals("-?") || arg.equals("-help")) {
		displayHelp();
	    } else if (arg.equals("-h") || arg.equals("-host")) {
		host = getValue(current_arg + 1, args);
	    } else if (arg.equals("-p") || arg.equals("-port")) {
		port = getValue(current_arg + 1, args);
	    } else if (arg.equals("-soid") || arg.equals("-study_oid")) {
		study_oid = getValue(current_arg + 1, args);
	    } else if (arg.equals("-sname") || arg.equals("-study_name")) {
		study_name = getValue(current_arg + 1, args);
	    } else if (arg.equals("-sid") || arg.equals("-study_id")) {
		study_id = getValue(current_arg + 1, args);
	    } else if (arg.equals("-suid") || arg.equals("-study_unique_id")) {
		study_uid = getValue(current_arg + 1, args);
	    } else if (arg.equals("-U") || arg.equals("-user")) {
		user = getValue(current_arg + 1, args);
	    } else if (arg.equals("-P") || arg.equals("-password")) {
		password = getValue(current_arg + 1, args);
	    } else if (arg.equals("-D") || arg.equals("-database")) {
		database = getValue(current_arg + 1, args);
	    } else if (arg.equals("-nomap")) {
		nomap = true;
		current_arg--;
	    } else if (arg.equals("-spss")) {
		spss = true;
		current_arg--;
	    } else if (arg.equals("-xmlFile")) {
		xmlFile = getValue(current_arg + 1, args);
	    } else if (arg.equals("-mapFile")) {
		mapFile = getValue(current_arg + 1, args);
	    } else if (arg.equals("-f") || arg.equals("-output_to_file")) {
		output_to_file = true;
		current_arg--;
	    } else {
		System.out.println("Unknown command: " + args[current_arg]);
		displayHelp();
	    }

	    current_arg+=2;
	}

	//showCommadsGiven();
	
	// Set the Global Flags for this application instance
	setGlobalFlags();
    }

    private void setGlobalFlags()
    {
	GlobalFlags gf = GlobalFlags.getInstance();
	gf.host = host;
	gf.port = port;
	gf.study_oid = study_oid;
	gf.study_uid = study_uid;
	gf.study_name = study_name;
	gf.study_id = study_id;
	gf.user = user;
	gf.password = password;
	gf.database = database;
	gf.mapFile = mapFile;
	gf.xmlFile = xmlFile;
	gf.nomap = nomap;
	gf.spss = spss;
	gf.output_to_file = output_to_file;
    }

    public void showCommadsGiven()
    {
	System.out.println("Commands given:");
	System.out.println("host      : " + host);
	System.out.println("port      : " + port);
	System.out.println("study_oid : " + study_oid);
	System.out.println("study_name: " + study_name);
	System.out.println("study_id  : " + study_id);
	System.out.println("user      : " + user);
	System.out.println("password  : " + password);
	System.out.println("database  : " + database);
	System.out.println("mapFile   : " + mapFile);
	System.out.println("xmlFile   : " + xmlFile);
	System.out.println("nomap     : " + nomap);
	System.out.println("spss      : " + spss);
	System.out.println("output_to_file: " + output_to_file);
    }

    public String getValue(int arg, String[] args)
    {
	if (arg < args.length) {
	    return args[arg];
	} else {
	    System.out.println("Missing value for command: " + args[arg-1]);
	    displayHelp();
	}

	return null;
    }

    public void displayHelp()
    {
	System.out.println("\nUsage: java [-javaoptions] -jar export.jar [-option value] [-option value]...\n");
	System.out.println("Where -javaoptions typically needed:");
	System.out.println("   -Xmx###m\tTotal memory to allow this export to use during runtime");
	System.out.println("   -Xms###m\tTotal memory to initially allocate\n");
	System.out.println("Where -options value include:");
	System.out.println("   -h | -host\thost machine where database is located (default: localhost)");
	System.out.println("             \t-eg: 192.168.123.45 or www.servername.com\n");
	System.out.println("   -p | -port\tport where database is located (default: 5432)\n");
	System.out.println("   -soid\n   -study_oid\tstudy to export based on oid as shown in OpenClinica\n");
	System.out.println("   -sname\n   -study_name\tstudy to export based on study name");
	System.out.println("             \t-if study name contains whitespace or special characters, it must be");
	System.out.println("             \t enclosed in quotes. eg: -study_name \"my study name\"\n");
	System.out.println("   -sid\n   -study_id\tstudy to export based on primary key id in database\n");
	System.out.println("   -suid\n   -study_unique_id\tstudy to export based on studies unique identifier as specified in open clinica\n");
	System.out.println("   -U | -user\tusername used to access database containing study for export\n");
	System.out.println("   -P\n   -password\tusers password to access database\n");
	System.out.println("   -D\n   -database\tdatabase name containing OpenClincia (default: openclinica)\n");
	System.out.println("   -mapFile\tmap file name created during export\n");
	System.out.println("   -xmlFile\texport file name to create\n");
	System.out.println("   -nomap\tinstructs export to not create a map file\n");
	System.out.println("   -f\n-output_to_file\tSend all output to file OUTPUT.TXT. File will be overwritten.\n");
	System.out.println("   -spss\tgenerate date formats compatible with SPSS\n");
	System.out.println("   -?\n   -help\tprint this help message\n");
	System.exit(0);
    }

    public void showOutputDetails()
    {
	System.out.println    ("----------------------------------------");
	System.out.println    ("             Export Output:             ");
	System.out.println    ("----------------------------------------");

	if (!nomap) {
	    System.out.println("          MAP FILE: export.map.xml");
	} else {
	    System.out.println("          MAP FILE: not generated");
	}
	System.out.println    ("       EXPORT FILE: " + xmlFile);
	System.out.println    ("----------------------------------------");
    }

    public ArrayList<String> getStudy(Connection con)
    {
	ArrayList<String> params = new ArrayList<String>();

	ArrayList<String> studies = new ArrayList<String>();
	ArrayList<String> study_ids = new ArrayList<String>();
	String study_id_selected = null;
	String study_name_selected = null;
		

	// Check that there is a connection to the database
	if (con == null) {
	    System.out.println("Null pointer.  Not connected to database!");
	    System.exit(1);
	}

	try {


	    // If the user has specified a study to retrieve on the command line, search for it in the database
	    if (study_oid != null) {
		// User has specified an OID for the study
		PreparedStatement pst = con.prepareStatement("select name, study_id from study where oc_oid = ?");
		pst.setString(1,study_oid);
		ResultSet res = pst.executeQuery();
	    
		if (res.next()) {
		    study_id_selected = res.getString("study_id");
		    study_name_selected = res.getString("name");
		}
		
	    } else if (study_id != null) {
		// User has specifed an ID for the study
		PreparedStatement pst = con.prepareStatement("select name, study_id from study where study_id = ?");
		pst.setString(1,study_id);
		ResultSet res = pst.executeQuery();
	    
		if (res.next()) {
		    study_id_selected = res.getString("study_id");
		    study_name_selected = res.getString("name");
		}

	    } else if (study_uid != null) {
		// User has specifed an ID for the study
		PreparedStatement pst = con.prepareStatement("select name, study_id from study where unique_identifier = ?");
		pst.setString(1,study_uid);
		ResultSet res = pst.executeQuery();
	    
		if (res.next()) {
		    study_id_selected = res.getString("study_id");
		    study_name_selected = res.getString("name");
		}
		
	    } else if (study_name != null) {
		// User has specified a study name
		PreparedStatement pst = con.prepareStatement("select name, study_id from study where name = ?");
		pst.setString(1,study_name);
		ResultSet res = pst.executeQuery();
	    
		if (res.next()) {
		    study_id_selected = res.getString("study_id");
		    study_name_selected = res.getString("name");
		}
	    }

	    if (study_id_selected == null) {
		
		// Find all parent studies
		Statement st = con.createStatement();
		String query = "SELECT name, study_id, status_id FROM study WHERE parent_study_id IS null";
		ResultSet rs = st.executeQuery(query);
		
		while (rs.next()) {
		    study_id_selected = rs.getString("study_id");
		    String study_name = rs.getString("name");
		    String status = rs.getString("status_id");
		    
		    if (status.equals("5")) {
			study_name = study_name.concat("  <== REMOVED");
		    }
		    
		    studies.add(study_name);
		    study_ids.add(study_id_selected);
		    
		    Statement st2 = con.createStatement();
		    String query2 = "SELECT name, study_id, status_id FROM study WHERE parent_study_id = " + study_id;
		    ResultSet rs2 = st2.executeQuery(query2);
	
		    while (rs2.next()) {		
			String site_name = rs2.getString("name");
			String site_id = rs2.getString("study_id");
			String status2 = rs2.getString("status_id");
			
			if (status2.equals("5")) {
			    site_name = site_name.concat("  <== REMOVED");
			}   
			site_name = " |-- " + site_name;
			
			studies.add(site_name);
			study_ids.add(site_id);		
		    }
		}
		
		if (studies.size() == 0) {
		    // No studies found matching search term
		    
		} else if (studies.size() == 1) {
		    // Only one match found
		    study_id_selected = study_ids.get(1);
		} else {
		    
		    boolean selected = false;
		    int choice = 0;
		    
		    while (!selected) {
			
			// Mulitple matches found
			System.out.println("\n\n\nPlease choose a study:");
			System.out.println("----------------------");
			
			for (int i=0; i < studies.size(); i++) {
			    if ( i < 9 ) {
				System.out.print(" ");
			    }
			    System.out.println((i+1) + ") " + studies.get(i));
			}
			
			System.out.print("\n==> ");
			
			String input = readLine();
			
			try {
			    choice = Integer.parseInt(input);
			    choice--;
			    if ( (choice < 0) || (choice >= studies.size()) ) {
				System.out.println("Please chose a valid study number.");
			    } else {
				selected = true;
				study_id_selected = study_ids.get(choice);
				study_name_selected = studies.get(choice);
			    }			
			}
			
			catch (NumberFormatException nfe) {
			    // User didn't enter an integer
			    System.out.println("Please chose a valid study number.");
			}
			
		    } // while
		    
		} // else
		
	    }
	    
	} catch (SQLException e) {
	    System.out.println("SQLException: " + e);
	}
	    
	String name = study_name_selected.replace("|--","").trim().replace("(","_").replace(")","_").replace(" ","_");
	params.add(study_id_selected);	      
	name = Helper.cleanseFileName(name);
	params.add(name + ".xml");
	params.add(name + ".map.xml");
	return params;
    }

    public String getExportFileName()
    {
	if (xmlFile == null) {
	    System.out.print("\nEnter Export file name (default: derived from study): ");
	    xmlFile = readLine();
	    
	    if (xmlFile.length() < 2) {
		xmlFile = null;
	    }
	}

	return xmlFile;
    }

    public String getMapFileName()
    {
	if (!nomap && (mapFile == null)) {
	    System.out.print("Enter Map file name (default: derived from study): ");
	    mapFile = readLine();

	    if (mapFile.length() < 2) {
		mapFile = null;
	    }
	}

	return mapFile;
    }

    public String getDatabaseUrl()
    {
	if (host == null) {
	    System.out.print("\nEnter Database url (default: localhost): ");
	    host = readLine();

	    if (host.length() < 5) {
		host = "localhost";
		//host = "129.128.148.72";
	    }
	}

	return host;
    }

    public String getDatabasePort()
    {
	if (port == null) {
	    System.out.print("     Database port (default: 5432): ");
	    port = readLine();

	    if (port.length() < 2) {
		port = "5432";
	    }
	}

	return port;
    }

    public String getDatabaseName()
    {
	if (database == null) {
	    System.out.print("     Database name (default: openclinica): ");
	    database = readLine();

	    if (database.length() < 2) {
		database = "openclinica";
	    }
	}

	return database;
    }

    public String getUserName()
    {
	if (user == null) {
	    System.out.print("          username (default: clinica): ");
	    user = readLine();
	    
	    if (user.length() < 2) {
		user = "clinica";
	    }
	}

	return user;
    }

    public String getPassword()
    {
	if (password == null) {
	    System.out.print("          password: ");
	    password = readLine();

	    if (password.length() < 2) {
		password = "clinica";
	    }
	}
	return password;
    }

    private String readLine()
    {
	String input = "";
	
	try {
	    InputStreamReader converter = new InputStreamReader(System.in);
	    BufferedReader in = new BufferedReader(converter);
	    
	    input = in.readLine();
	    
	} catch (IOException e) {}

	return input;		
    }

    private String readLineHidden()
    {
	String input = "";
	
	Console in = System.console();
	    
	input = new String(in.readPassword());
	    
	return input;		
    }






    // TESTING

    String url;
    String db;
    String driver;
    String pass;
    Connection con;
    Integer s_id;

    private void Test(String[] args)
    {	
	initializeDatabaseConnection();
	ArrayList<String> params = getStudy(con);
	System.out.println("Selected study id: " + params.get(0));
	System.out.println("Export File Name: " + params.get(1));
	System.out.println("Map File Name: " + params.get(2));
    }

    private void initializeDatabaseConnection()
    {
	url = "jdbc:postgresql://127.0.0.1:5432/";
	db = "openclinica";
	driver = "org.postgresql.Driver";
	user = "clinica";
	pass = "clinica";
	con = null;

	/* Load the driver for the database connection */
	try {
	    Class.forName(driver);
	}
	catch(Exception x) {
	    System.out.println("Could not find driver " + driver +". Is it on the CLASSPATH?");
	}
	System.out.println("Postgresql driver loaded");

	// Get connection to the postgresql database
	try {
	    con = DriverManager.getConnection(url+db, user, pass);
	}
	catch(SQLException x) {
	    System.out.println("Couldn't get connection to postgresql OpenClinica");
	}
	System.out.println("Successful connection to OpenClinica on localhost");
    }

    public static void main(String[] args)
    {
	UserInterface ui = new UserInterface();

	ui.Test(args);
    }
}