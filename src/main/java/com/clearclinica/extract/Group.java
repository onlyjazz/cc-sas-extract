package com.clearclinica.extract;

/**
 * Describes all the columns and items that make up a group on a crf
 **/
public class Group extends ContainerItem {
    public Group(String id, String name) {
        super(id, name);
    }

    public int type() {
        return GROUP;
    }

    public static void main(String[] args) {

    }
}