# cc-sas-extract
Java-based batch SAS XML extract 
Code originally written by Rick Watts rick.watts@ualberta.ca
WCHRI OpenClinica com.clearclinica.extract.Export README Last updated Jan 23, 2012

This package contains the following files:
export2Sas                      Script that automatically runs the program and corrects CRFDATE format( D. Lieberman)
export.pdf                      Documentation

export.jar                      The executable java code
postgresql-8.1-405.jdbc3.jar    Postgresql library
export.bat                      A simple bat file to invoke the export

export.map                      Sample map file
export.xml                      Sample data file
stdy1234.pdf                    Specimen CRF on which sample data is based

ocimport.sas                    Sample SAS program
importOcData.sas                SAS macro file

This program been developed by the WCHRI Clinical Research Informatics Core for internal purposes.

The export2Sas script was added by Danny Lieberman (dl@software.co.il) to fix a bug with the CRFDATE format.

The application and this documentation are made available to the OpenClinica/ClinCapture user community as is with no warrantee, implied or otherwise.
Licensed under LGPLv3.0

This program is free software; you can redistribute it and/or modify it under the terms of LGPL 3.0.

