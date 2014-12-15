# DVEnabler

Adds or removes DocValues to or from existing fields in a Lucene/Solr index, without full re-index.

Fields in Lucene/Solr can be indexed, stored and DocValued. DocValued fields are used for functions requiring fast
bulk access to field content. Faceting and sorting are examples.  DocValues is enabled on a per-field basis.
Normally a full re-index of the Lucene/Solr index is required if one want to enable DocValues for a field.

## Requirements

 * JDK 1.7
 * Maven 3 (Maven 1 or 2 might also work)
 * A Lucene index

## Status

The current implementation is highly experimental!

All DocValues-types are now supported, but lo larger-scale conversions has been tried.

Only fields with stored values can currently be converted to DocValues.

The conversion requires all unique values of DV-needing String fields to be stored in-memory in a TreeSet.
This is fairly memory-intensive.

## Build and usage

Clone the repository and build a dependency including JAR with

`mvn install assembly:assembly`

To start the GUI, run

`java -Xmx512m -jar target/dvenabler-1.0-SNAPSHOT-jar-with-dependencies.jar`

To use the command line interface, call

`java -Xmx512m -cp target/dvenabler-1.0-SNAPSHOT-jar-with-dependencies.jar dk.statsbiblioteket.netark.dvenabler.Command -h`

## Contact

Developed by Thomas Egense (teg@statsbiblioteket.dk) and Toke Eskildsen (te@statsbiblioteket.dk) 2014.
Feel free to send emails with comments and questions.
