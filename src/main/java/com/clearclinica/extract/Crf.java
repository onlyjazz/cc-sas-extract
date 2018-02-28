package com.clearclinica.extract;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 *
 *
 **/
public class Crf extends ContainerItem {
    private ArrayList<String> sids;


    public Crf(String id, String name) {
        super(id, name);
        sids = new ArrayList<String>();
    }

    // Collapses all the crf_versions that are children of this crf into one "version".
    public void collapseVersions() {
        CrfVersion new_version = new CrfVersion("crf_version_id goes here", "Collapsed");
        ListIterator crf_versions_itr = getItemsIterator();

        // Iterate over com.clearclinica.extract.CrfVersion children of this com.clearclinica.extract.Crf object
        while (crf_versions_itr.hasNext()) {

            CrfVersion version = (CrfVersion) crf_versions_itr.next();
            new_version.addVersion(version.id());

            ListIterator sections_itr = version.getItemsIterator();

            // Iterating over Sections in this com.clearclinica.extract.CrfVersion
            while (sections_itr.hasNext()) {

                Section section = (Section) sections_itr.next();
                Section new_section;

                if (new_version.contains(section)) {
                    new_section = (Section) new_version.getItem(section);
                    // Add this section id to the new_section being created
                    new_section.addId(section.id());
                } else {
                    new_section = new Section(section.id(), section.name(), section.description(), Section.CREATE_GENERIC_HEADER);
                    new_version.addItem(new_section);
                }

                // Step through this section item by item
                ListIterator items = section.getItemsIterator();

                while (items.hasNext()) {
                    Item item = (Item) items.next();

                    // Does this new_version we are creating contain this item?
                    if (new_section.contains(item)) {
                        new_section.copyColumns(item);
                    } else {
                        new_section.addItem(item);
                    }
                }

                // Remove this section that has been merged now that it is no longer needed
                sections_itr.remove();
            }

            // Remove this crf version now that it is merged
            crf_versions_itr.remove();
        }

        // Add this new version to the com.clearclinica.extract.Crf
        addItem(new_version);
    }

    // Add subject id to the crf.  This is used when crf's are the table names in the export instead
    // of the sections.  Returns true if sid was added, false if this crf already contained this id
    public boolean addSID(String sid) {
        // Check if this sid is already contained in the list of sid's for this crf
        if (!sids.contains(sid)) {
            sids.add(sid);
            return true;
        }

        return false;
    }

    public void printVersions() {
        // Step through each crfversion that this crf contains and print it out
        for (Item version : getItems()) {

            System.out.println("  com.clearclinica.extract.CrfVersion: " + version.name());
            System.out.print("      versions contained: ");

            for (String s : version.crfVersions()) {
                System.out.print(s + " ");
            }

            System.out.println();

            ((CrfVersion) version).printSections();
        }
    }

    public int type() {
        return CRF;
    }

    public static void main(String[] args) {

    }
}