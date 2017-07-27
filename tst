#!/bin/bash
# Remove XML files from previous runs
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR
rm *xml
sn="NIMO"
mf="$sn"".map.xml"
xf="$sn"".xml"
java -jar export.jar -h localhost -p 5432 -sname "$sn" -U clinica -P clinica -D nimo2 -mapFile "$mf" -xmlFile "$xf"
sed -i s/mmddyy10/IS8601DA/ *.xml
echo "Now zipping the files"
zf="$sn".z
zip   "$zf"  *xml
bcc="dl@software.co.il"
to="rivkar@clearclinica.com"
#
echo "Greetings from the weekly SAS export for $sn.  Download and unzip the attached file to extract the xml and map files" | mutt -s "$sn SAS export" "$to"  -a "$zf" -b "$bcc" -e 'my_hdr From:ClearClinica<no-reply@repnets.com>'
rm *.xml
exit 0
