package com.clearclinica.extract;

import java.util.Iterator;
import java.util.ListIterator;

/**
 *
 *
 **/
public class Study extends ContainerItem {
    private String protocol;
    private Format formats;

    public Study(String id, String name) {
        super(id, name);
    }

    // Display a list of crf's contained in this study
    public void printCrfsContained() {
        ListIterator crfs_itr = getItemsIterator();

        while (crfs_itr.hasNext()) {
            Crf crf = (Crf) crfs_itr.next();

            System.out.println("com.clearclinica.extract.Crf: " + crf.name());
        }
    }

    public void setFormatObject(Format format) {
        this.formats = format;
    }

    public Format getFormatObject() {
        return formats;
    }

    public void collapseCrfVersions() {
        ListIterator crfs_itr = getItemsIterator();

        while (crfs_itr.hasNext()) {
            Crf crf = (Crf) crfs_itr.next();

            crf.collapseVersions();
        }
    }

    public void printCrfs() {
        Iterator crfs_itr = getItemsIterator();

        while (crfs_itr.hasNext()) {
            Crf crf = (Crf) crfs_itr.next();
            System.out.println("\n\ncom.clearclinica.extract.Crf: " + crf.name());

            crf.printVersions();
        }
    }

    public int type() {
        return STUDY;
    }

    public void protocol(String protocol) {
        this.protocol = protocol;
    }

    public String protocol() {
        return protocol;
    }

    public static void main(String[] args) {

    }
}