package com.clearclinica.extract;

import java.util.ArrayList;
import java.io.BufferedWriter;

/**
 *
 **/
public abstract class Item {
    // Constants used to identify individual objects derived from this one
    static final int COLUMN = 1;
    static final int GROUP = 2;
    static final int MULTISELECT = 3;
    static final int SECTION = 4;
    static final int CONTAINER = 5;
    static final int CRF_VERSION = 6;
    static final int CRF = 7;
    static final int STUDY = 8;
    static final int TABLE = 9;

    protected int ordinal;            // Order of this item on the section
    protected String name;            // The name of the item.
    protected String id;
    protected String description;

    protected String xml_safe_name;   // This object will be the only object with this name
    protected String xml_safe_description;

    protected ContainerItem parent_container;  // The container that this object is in


    public void print_debug() {
        System.out.println("com.clearclinica.extract.Item information: \n  ordinal: " + ordinal + "\n  name: " + name + "\n  id: " + id + "\n  description: " + description);
        System.out.println("\n  xml_safe_name: " + xml_safe_name + "\n  xml_safe_description: " + xml_safe_description);
    }


    public Item(String id, String name) {
        this.name = name;
        this.id = id;

        // Create the xml_safe_name
        xml_safe_name = Helper.createXMLTagName(name);
    }

    public abstract void eraseSubjectData(Section.UniqueData d);

    public abstract void write(BufferedWriter file, String indent, Section.UniqueData d, int ordinal);

    public abstract ColumnData getDataPoint(Section.UniqueData d, int ordinal);

    public abstract int getDepth(Section.UniqueData d);

    public abstract ArrayList<ColumnData> data();

    // SETTERS
    protected void parentContainer(ContainerItem parent) {
        parent_container = parent;
    }

    public void name(String name) {
        this.name = name;
        xml_safe_name = Helper.createXMLTagName(name);
    }

    public void xmlName(String name) {
        this.xml_safe_name = name;
    }

    public void description(String description) {
        this.description = description;
    }

    // GETTERS
    public abstract int type();       // Overload this function to identify item type

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String xmlName() {
        return xml_safe_name;
    }

    protected ContainerItem parentContainer() {
        return parent_container;
    }

    public String description() {
        return description;
    }

    public String xmlDescription() {
        return Helper.cleanse(description);
    }


    public boolean hasId(String id) {
        return this.id.equals(id);
    }


    // Returns all crf versions that this item belongs to. There could be more than
    // one version due to the fact that crf versions are collapsable
    public ArrayList<String> crfVersions() {
        ArrayList<String> versions = new ArrayList<String>();

        versions.addAll(parentContainer().crfVersions());

        return versions;
    }

    protected Format getFormatObject() {
        return parent_container.getFormatObject();
    }

    protected String studyProtocol() {
        if (type() == Item.STUDY) {
            return ((Study) this).protocol();
        } else {
            return parent_container.studyProtocol();
        }
    }

    // DEBUGGING AND TESTING
    public static void main(String[] args) {

    }
}