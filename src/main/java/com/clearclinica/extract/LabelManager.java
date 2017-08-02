package com.clearclinica.extract;

import java.util.ArrayList;

class LabelManager {
    private ArrayList<Label> labels; // Array of com.clearclinica.extract.Label's managed
    private String oc_label;         // OpenClinica's label for this object
    private int num_labels;          // Number of com.clearclinica.extract.Label's managed

    private boolean DEBUG = false;

    public LabelManager(String label) {
        labels = new ArrayList<Label>();
        num_labels = 0;
        oc_label = label;
    }

    public void setDebug() {
        DEBUG = true;
    }

    /**
     * com.clearclinica.extract.Label addLabel(String options, String values):
     * ---------------------------------------------------------------------------
     * Add the given options and values to the com.clearclinica.extract.LabelManager. A new label will be
     * created and added to the ArrayList if this mapping of options to values
     * is not already represented by this com.clearclinica.extract.LabelManager.
     * <p>
     * Takes care of making sure that the sas_label that represents this mapping
     * is unique and is contained within 32 characters.
     * <p>
     * Returns the com.clearclinica.extract.Label that represents this mapping of options to values.
     **/
    public Label addLabel(String dtype, String options, String values) {
        // Step through the managed Labels to determine if this mapping is represented
        for (int i = 0; i < labels.size(); i++) {
            Label label = labels.get(i);

            // If the label does not have the same dtype, then they are different.
            // For example, OpenClinica allows two identical labels that have different
            // data types, "INT" vs "ST"
            if (!dtype.equals(label.getDtype())) {
                break;
            }

            // TODO: Should maybe cleanse these values before writing to file, not sooner
            // TODO: Why haven't I cleansed the options as well?
            values = Helper.cleanse(values);

            if (label.contains(options, values)) {
                if (DEBUG) {
                    System.out.println("com.clearclinica.extract.LabelManager: Found label in label manager.");
                }
                return label;
            } else {
                if (DEBUG) {
                    System.out.println("com.clearclinica.extract.LabelManager: com.clearclinica.extract.Label not found in label manager.");
                }
            }
        }

        // If we made it this far, no match was found for this mapping in the list of labels

        // Determine SAS label name for this mapping
        String sas_label = oc_label;

        // Make label xml tag safe
        sas_label = Helper.createXMLTagName(sas_label, Helper.FORMAT);

        num_labels++;
        sas_label = Helper.changeDuplicateName(sas_label, num_labels, Helper.FORMAT);

        // Add a "$" to the label if it is string
        if (dtype.equals("ST")) {
            sas_label = "$" + sas_label;
        }

        // Add this mapping to the com.clearclinica.extract.Label list
        // Escape the values so that they are xml ready
        values = Helper.cleanse(values);

        Label new_label = new Label(dtype, sas_label, options, values);
        labels.add(new_label);
        return new_label;
    }

    public int getNumLabels() {
        return num_labels;
    }

    public static void main(String[] args) {
        LabelManager lm = new LabelManager("123456789012345-----67890123456789012345");

        Label label;
        label = lm.addLabel("ST", "0---0,1,2", "HERE----HERE,Hepinefrin,Dremol");
        label.print();
    /*	label = lm.addLabel("ST","0,1,2","Prozac,Hepinefrin,Dremol");	label.print();
	label = lm.addLabel("ST","0,1","Prozac,Dremol");	label.print();
	label = lm.addLabel("ST","1,5","Prozac,Dremol");	label.print();
	label = lm.addLabel("ST","0,1","Prozac,Dremol");	label.print();
	label = lm.addLabel("ST","0,1","1,2"); 	                label.print();
	label = lm.addLabel("ST","0,1","1,3");	                label.print();
	*/
    }
}