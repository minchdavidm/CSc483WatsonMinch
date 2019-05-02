#Building (a part of) Watson

#Author: David Minch (minchdavidm)

Spec reference: https://d2l.arizona.edu/d2l/le/content/762856/viewContent/7110423/View

Summary: This program is a Wikipedia parser built to showcase the core functionality of IBM's Watson. This will be done by evaluating Jeopardy answers and responding with questions built from the titles of the Wikipedia pages that best match the Jeopardy answer given. This program is made as a programming project for CSc 483: Text Retrieval and Web Search, a course at the University of Arizona, Spring 2019 semester, under the direction of instructor Mihai Surdeanu.

#Fundamentals

This repository contains a project built on Maven. The base directory and the automatically generated pom.xml file were generated with the command:

`mvn archetype:generate -DgroupId=CSc483 -DartifactId=BuildingWatsonMinchDavidM -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4 -DinteractiveMode=false`


#Using the code

The code can be run by importing this project onto a machine capable of running maven and java. 

The program can be run with the command:

./runDefaultBehavior.sh

` `

See the PDF for a full explanation as to the workings of the code.

