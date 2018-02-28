package com.clearclinica.extract;

import java.util.ArrayList;

/**
 *
 *
 **/
public class CrfVersion extends ContainerItem {
    private ArrayList<String> versions;

    public CrfVersion(String id, String name) {
        super(id, name);
        versions = new ArrayList<String>();
    }

    public int type() {
        return CRF_VERSION;
    }

    public void addVersion(String version) {
        versions.add(version);
    }

    public ArrayList<String> crfVersions() {
        return versions;
    }


    public void printSections() {
        for (Item item : getItems()) {
            System.out.println("      com.clearclinica.extract.Section: \t<<-- " + item.xmlName() + " -->>");
            System.out.print("          ids: ");
            ((Section) item).printIDS();
            System.out.println();
        }
    }

    public static void main(String[] args) {

    }
}