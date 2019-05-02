#!/usr/bin/ksh
mvn compile
str=""
for ele in $@; do
  str="${str} ${ele}"
done

mvn exec:java -Dexec.mainClass="CSc483.WatsonMinch" -Dexec.args="-q $str"

