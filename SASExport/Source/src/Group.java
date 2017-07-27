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
 *  Describes all the columns and items that make up a group on a crf
 *
 **/
public class Group extends ContainerItem
{
    public Group(String id, String name)
    {
	super(id, name);
    }

    public int type() {	return Item.GROUP; }

    public static void main(String[] args)
    {

    }
}