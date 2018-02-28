package com.clearclinica.extract;

class Helper {
    // Disallowed xml characters for tag name and SAS name
    private static String[] escChar = {"~", "_",
            "!", "_",
            "@", "_",
            "#", "_",
            "$", "_",
            "%", "_",
            "^", "_",
            "&", "_",
            "*", "_",
            "(", "_",
            ")", "_",
            "+", "_",
            "=", "_",
            "{", "_",
            "}", "_",
            "[", "_",
            "]", "_",
            "|", "_",
            "\\", "_",
            "/", "_",
            ";", "_",
            ":", "_",
            "\"", "_",
            ",", "_",
            ".", "_",
            "'", "_",
            ">", "_",
            "<", "_",
            "?", "_",
            "-", "_",
            "`", "_"};    // Oct 19, 2009

    private static String[] formatChar = {"~", "_",
            "!", "_",
            "@", "_",
            "#", "_",
            "%", "_",
            "^", "_",
            "&", "_",
            "*", "_",
            "(", "_",
            ")", "_",
            "+", "_",
            "=", "_",
            "{", "_",
            "}", "_",
            "[", "_",
            "]", "_",
            "|", "_",
            "\\", "_",
            "/", "_",
            ";", "_",
            ":", "_",
            "\"", "_",
            ",", "_",
            ".", "_",
            "'", "_",
            ">", "_",
            "<", "_",
            "?", "_",
            "-", "_",
            "`", "_"};

    // Added Oct 19, 2009
    private static String[] valueChar = {"&", "&amp;",
            "'", "&apos;",
            "<", "&lt;",
            ">", "&gt;"};

    // Dis-allowed file name characters
    private static String[] fileChar = {"/", "_",
            "?", "_",
            "<", "_",
            ">", "_",
            "\\", "_",
            ":", "_",
            "*", "_",
            "|", "_",
            "\"", "_",
            "-", "_",
            "^", "_"};

    public static int FORMAT = 0;
    public static int LABEL = 1;


    public static String cleanse(String string) {
        return _cleanse(string, valueChar);
    }

    public static String cleanseTagName(String string) {
        return _cleanse(string, escChar);
    }

    public static String cleanseFormatTagName(String string) {
        return _cleanse(string, formatChar);
    }

    public static String cleanseFileName(String name) {
        return _cleanse(name, fileChar);
    }

    private static String _cleanse(String string, String[] escChar) {
        if (string == null) {
            return null;
        }

        StringBuffer sb = new StringBuffer(string);

        int index = 0;
        boolean done = false;

        for (int i = 0; i < escChar.length / 2; i++) {

            done = false;
            index = 0;

            while (!done) {

                index = sb.indexOf(escChar[i * 2], index);

                if (index != -1) {
                    sb = sb.replace(index, index + 1, escChar[i * 2 + 1]);
                    index++;
                } else {
                    done = true;
                }
            }
        }

        return sb.toString();

    }


    public static String createXMLTagName(String name, int type) {
        // Replace illegal characters in tag name
        StringBuilder bld = new StringBuilder(name.trim());
        String first = bld.substring(0, 1);
        first = first.replaceFirst("[^a-zA-Z$_]", "_" + first.charAt(0));
        bld.replace(0, 1, first);

        if (type == FORMAT) {
            // Append an underscore to a name that ends in a numeric character
            char c = bld.charAt(bld.length() - 1);
            if (c >= '0' && c <= '9') {
                // Append an underscore
                bld.append("_");
            }
        }

        String xmlsafename = bld.toString();
        // Replace all spaces with underscores
        xmlsafename = xmlsafename.replace(" ", "_");

        if (type == FORMAT) {
            xmlsafename = xmlsafename.replace("-", "_");
        }

        // Make sure tag isn't larger than 32 characters, and if it is, truncate and append an underscore
        if (xmlsafename.length() > 32) {
            xmlsafename = xmlsafename.substring(0, 31).concat("_");
        }

        if (type == FORMAT) {
            xmlsafename = cleanseFormatTagName(xmlsafename);
        } else {
            xmlsafename = cleanseTagName(xmlsafename);
        }
        return xmlsafename;
    }


    // USING
    public static String createXMLTagName(String name) {
        // Replace illegal characters in tag name
        StringBuilder bld = new StringBuilder(name.trim());
        String first = bld.substring(0, 1);
        first = first.replaceFirst("[^a-zA-Z$_]", "_" + first.charAt(0));
        bld.replace(0, 1, first);

        String xmlsafename = bld.toString();
        // Replace all spaces with underscores
        xmlsafename = xmlsafename.replace(" ", "_");

        // Make sure tag isn't larger than 32 characters, and if it is, truncate and append an underscore
        if (xmlsafename.length() > 32) {
            xmlsafename = xmlsafename.substring(0, 31).concat("_");
        }

        xmlsafename = cleanseTagName(xmlsafename);
        return xmlsafename;
    }

    // USING
    public static String changeDuplicateName(String name, int attempts) {
        // Change name of label to make it unique
        String tack_on = "_" + (attempts);
        String previous_tack_on = "_" + (attempts - 1);

        if (attempts == 1) {
            previous_tack_on = "";
        }

        if (name.length() > 30) {
            name = name.substring(0, (32 - tack_on.length())).concat(tack_on);
        } else {
            //name = name.concat(tack_on);
            name = name.substring(0, (name.length() - previous_tack_on.length())).concat(tack_on);
        }

        return name;
    }

    public static String changeDuplicateName(String name, int attempts, int type) {
        if (type == FORMAT) {
            // Change name of label to make it unique
            if (attempts > 1) {
                String tack_on = "_" + (attempts - 1) + "_";
                if (name.length() > 30) {
                    name = name.substring(0, (32 - tack_on.length())).concat(tack_on);
                } else {
                    name = name.concat(tack_on);
                }
            }
        } else {
            // Change name of label to make it unique
            if (attempts > 1) {
                String tack_on = "_" + (attempts - 1);
                String previous_tack_on = "_" + (attempts - 2);
                if (attempts == 2) {
                    previous_tack_on = "";
                }
                if (name.length() > 31) {
                    name = name.substring(0, (32 - tack_on.length())).concat(tack_on);
                } else {
                    //name = name.concat(tack_on);
                    name = name.substring(0, (name.length() - previous_tack_on.length())).concat(tack_on);
                }
            }
        }
        return name;
    }

    public static void main(String[] args) {
        String name = "abcdefghijklmnopqrstuvwxyz789012345";
        //System.out.println("Original string: " + name + "\nModified: " + com.clearclinica.extract.Helper.createXMLTagName(name, com.clearclinica.extract.Helper.LABEL));
    }
}
