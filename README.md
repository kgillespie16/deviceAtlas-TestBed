# deviceAtlas-TestBed

This project provides a test bed for device atlas upgrade to version 2.1.1.
This helps in finding the differences in device make/model identification 
between device atlas version 1.7 and version 2.1.1.

## Usage

To compile and run the project

javac Demo.java

java Demo --inputFileName deviceAtlas/user-agents--2016-07-19 --outputFileName output.csv --readLimit 1000

## Inputs

This takes two mandatory inputs:

1) The name of input user strings file to be tested.

2) The name of output csv file to be produced which contains all the differences between old and new version.
   The output files contains keys and their corresponding values for both old and new version of device atlas.
   
3) (optional) Number of lines to be read. If this option is not provided then the program will process complete file.


