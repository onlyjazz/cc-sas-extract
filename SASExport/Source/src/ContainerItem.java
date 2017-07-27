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

public class ContainerItem extends Item
{
    protected LinkedList<Item> items_contained;  // List of all child items contained in this item

    // Speed improvement -- from 10% of running time to 0.07%
    protected HashMap<String,Item> columns_contained;

    public ContainerItem(String id, String name)
    {
	super(id,name);
	items_contained = new LinkedList<Item>();
	columns_contained = new HashMap<String,Item>();
    }
    
    public void copyColumns(Item item)
    {
	// Copy all data this item contains to this new section
	if (item.type() == Item.COLUMN) {
	    Column column = findColumn(item.name());
	    if (column == null) {
		// Copy the whole column and all it's data
		column = new Column((Column)item);
		addItem(column);
	    } else {
		// Already has this column, so just append data to it
		column.addData( ((Column)item).data() );
	    }
	}

	else {
	    ListIterator itr = ((ContainerItem)item).getItemsIterator();

	    while (itr.hasNext()) {
		Item item_child = (Item)itr.next();
		ContainerItem this_item = (ContainerItem)findItemByName(item.name());
		if (this_item == null) {
		    System.out.println("ERROR:(CONTAINER) Cannot find container item " + item.name());
		    return;
		}
		this_item.copyColumns(item_child);
	    }
	}
    }

    /**
     *  insertData(String column_name,
     *             String value, 
     *             String study_subject, 
     *             String event,
     *             int ordinal)
     *  ----------------------------------
     *  Insert a single chunk of data into a column object that is the child of 
     *  the calling object.
     *
     *  column_name:   The column name or field that this data belongs to
     *  value:         The actual data to enter into the column_name
     *  study_subject: The study subject identifier that this data belongs to
     *  event:         Event this data belongs to
     *  ordinal:       The order of this column if it belongs to a group on the
     *                 crf. If this data does not belong to a group, it will have
     *                 an ordinal of 1.
     *
     **/
    public void insertData(String study_subject_id, String site, String event, String crf_version,
			   String crf_name, String event_startdt, String item_name, String value,
			   int ordinal)
    {
	// Get the Column Object where this data point will live
	Column column = findColumn(item_name);

	if (column == null) {
	    System.out.println("ERROR: Can not find column with name " + item_name);
	    return;
	}

	// Store this data point in the Column Object
	column.insertData(study_subject_id, event, crf_version, crf_name, site, event_startdt, value, ordinal);
    }
    
    // Add an Item object to this Container Object. Items added here represent metadata.
    public void addItem(Item item)
    {
	// Generic column names can cause conflict with existing names, so add an underscore to the name.
	// There shouldn't be a case to change the name more than once...but this could happen.  Make this
	// more robust if the need arises.
	if (findItemByName(item.name()) != null){
	    //System.out.println("Attempting to add column name: " + item.name() + " but it already exists!");
	    item.name(item.name()+"_");
	    //System.out.println("New name: " + item.name());
	}
		
	items_contained.add(item);
	item.parentContainer(this);

	// Speed improvement
	/* If this is a COLUMN object, add it to the hashmap */
	if (item.type() == Item.COLUMN) {
	    String column_name = ((Column)item).name();
	    columns_contained.put(column_name, item);
	}
    }

    public Item findItemByName(String name)
    {
	for (Item item : items_contained) {
	    if (item.name().equals( name ))
		return item;
	}
	return null;
    }

    public ListIterator getItemsIterator()
    {
	ListIterator itr = items_contained.listIterator();
	return itr;
    }

    public LinkedList<Item> getItems()
    {
	return items_contained;
    }

    /**
     *  findColumn
     *
     *  Find the column with this column name. This column should exist. This function is called
     *  when actual data items are being input after the Metadata has already been aquired.
     *
     **/
    public Column findColumn(String column_name)
    {
	Column column = (Column)columns_contained.get(column_name);

	if (column == null) {

	    // Step through all contained items searching them for this column
	    for (Item item : items_contained) {
		
		switch (item.type()) {
		    
		case Item.COLUMN: 
		    break;
		    
		case Item.MULTISELECT:
		    break;
		    
		default:
		    column = ((ContainerItem)item).findColumn(column_name);
		}
		
		if (column != null)
		    return column;
	    }

	    return null;

	} else {	    
	    return column;
	}
    }


    // Flattens out all columns in container. 
    public ArrayList<Column> getAllColumns()
    {
	ArrayList<Column> columns = new ArrayList<Column>();

	for (Item item : items_contained) {
	    switch(item.type()) {
	    case Item.COLUMN:
		columns.add((Column)item);
		break;
	    default:
		columns.addAll(((ContainerItem)item).getAllColumns());
		break;
	    }
	}

	return columns;
    }

    /* Returns the number of times a Group item has repeat data in it. Important for 
       displaying group data properly in the export file */
    public int getDepth(Section.UniqueData d)
    {
	int max_depth = 0;
	for (Item item : items_contained) {
	    int depth = item.getDepth(d);
	    if (depth > max_depth)
		max_depth = depth;
	}

	return max_depth;
    }

    public ColumnData getDataPoint(Section.UniqueData d, int ordinal)
    {
	// Need to get a single columns worth of data under this item that satisfies the UniqueData item requirements
	ColumnData column_data = null;
	
	for (Item item : items_contained) {
	    column_data = item.getDataPoint(d, ordinal);
	    
	    if (column_data != null)
		return column_data;
	}

	// TODO: Code reaches this point sometimes.  Check why and if this effects anything...

	return null;
    }

    /**
     *
     *  createUniqueSectionNames()
     *
     *  Find all sections that are descendants of this container item and ensure
     *  that they have unique names.
     *
     **/    
    public void createUniqueSectionNames()
    {
	ArrayList<ContainerItem> sections = sections();

	ListIterator itr = sections.listIterator();

	// Step through each section in the ArrayList and make sure it is unique against the remaining
	// sections in the list
	while (itr.hasNext()) {
	    Section current_section = (Section)itr.next();

	    int attempts = 0;
	    ListIterator remaining_sections_itr = sections.listIterator();

	    while (remaining_sections_itr.hasNext()) {
		Section compare_section = (Section)remaining_sections_itr.next();

		// Don't compare section to itself
		if (compare_section == current_section)
		    continue;

		if (current_section.xmlName().equals( compare_section.xmlName() )) {		   
		    // Section name is duplicate
		    attempts++;

		    String new_name = Helper.changeDuplicateName(current_section.xmlName(), attempts);
		    current_section.xmlName(new_name);

		    // Reset the iterator to the beginning
		    remaining_sections_itr = sections.listIterator();
		}
	    }	    
	}
    }

    /* Iterate over the sections in this table and have them print to map file */
    public void writeMapFile(BufferedWriter file)
    {
	// Step through each section in this table and write out the map file
	Iterator sections = sections().iterator();

	while (sections.hasNext()) {
	    Section section = (Section)sections.next();
	    section.writeMapFile(file);
	}
    }

    public void writeToXmlFile(BufferedWriter file, String indent)
    {
	// Get all sections that are ancestors of this object
	ArrayList<ContainerItem> sections_list = sections();

	// Step through each section in the above array and write
	// all columns it contains to the xml file
	for (ContainerItem section : sections_list) {
	    section.writeToXmlFile(file,indent);
	}
    }

    public void write(BufferedWriter file, String indent, Section.UniqueData d, int ordinal)
    {
	/* Iterate through all this ContainerItem object's contained items and
	   have these items write themselves to the xml file */
	for (Item item : items_contained) {
	    item.write(file,indent,d,ordinal);
	}
    }

    // GETTERS
    public int type() {	return Item.CONTAINER; }

    public Crf getCrfWithID(String id)
    {
	// Traverse the linked list, searching for the item with this id
	for (Item item : items_contained) {

	    switch (item.type()) {

	    case Item.CRF:
		if (item.hasId(id)) {
		    return (Crf)item;
		}
		break;

	    default:
		Crf foundItem = ((ContainerItem)item).getCrfWithID(id);

		if (foundItem != null) {
		    return foundItem;
		}
		break;
	    }
	}
	
	return null;	
    }

    public CrfVersion getCrfVersionWithID(String id)
    {
	// Traverse the linked list, searching for the item with this id
	for (Item item : items_contained) {

	    switch (item.type()) {

	    case Item.CRF_VERSION:
		if (item.hasId(id)) {
		    return (CrfVersion)item;
		}
		break;

	    default:
		CrfVersion foundItem = ((ContainerItem)item).getCrfVersionWithID(id);

		if (foundItem != null) {
		    return foundItem;
		}
		break;
	    }
	}
	
	return null;	
    }

    public Item getItemWithID(String id)
    {
	// Traverse the linked list, searching for the item with this id
	ListIterator itr = items_contained.listIterator();

	while (itr.hasNext()) {
	    Item item = (Item)itr.next();

	    switch (item.type()) {

	    case Item.COLUMN:
		if (item.hasId(id)) {
		    return item;
		}
		break;

	    default:
		if (item.hasId(id)) {
		    return item;
		} else {
		    Item foundItem = ((ContainerItem)item).getItemWithID(id);

		    if (foundItem != null) {
			return foundItem;
		    }
		}
		break;
	    }
	}
	
	return null;
    }

    public boolean contains(Item check_item)
    {
	// Check by name if they are the same
	String check_name = check_item.name();

	for (Item item : items_contained) {
	    String item_name = item.name();

	    if (item_name.equals(check_name)) {
		// Same item contained
		return true;
	    }
	}

	// Does not contain this item
	return false;
	
    }

    // Gets item based on item.name()
    public Item getItem(Item check_item)
    {
	// Check by name if they are the same
	String check_name = check_item.name();

	for (Item item : items_contained) {
	    String item_name = item.name();

	    if (item_name.equals(check_name)) {
		// Same item contained
		return item;
	    }
	}

	// Does not contain this item
	return null;
	
    }

    public void eraseSubjectData(Section.UniqueData d)
    {
	for (Item item : items_contained) {
	    item.eraseSubjectData(d);
	}
    }

    // Get all ColumnData that is a child of this ContainerItem object
    public ArrayList<ColumnData> data()
    {
	ArrayList<ColumnData> data = new ArrayList<ColumnData>();

	for (Item item : items_contained) {
	    data.addAll( item.data() );	
	}

	return data;		
    }

    // Get all sections contained in this container object
    public ArrayList<ContainerItem> sections()
    {
	ArrayList<ContainerItem> sections = new ArrayList<ContainerItem>();

	for (Item child_item : items_contained) {

	    switch(child_item.type()) {

	    case Item.COLUMN: 
		break;
	    case Item.SECTION: 
		sections.add((ContainerItem)child_item);
	    default:
		sections.addAll(((ContainerItem)child_item).sections());
	    }
	}

	return sections;		    
    }

    // DEBUGGING AND TESTING
    public static void main(String[] args)
    {

    }
}