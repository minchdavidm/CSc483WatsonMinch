#!/usr/bin/ksh
#Korn shell is the best, as always
mvn compile
mvn exec:java -Dexec.mainClass="CSc483.WatsonMinch"
