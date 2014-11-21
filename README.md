# DVEnabler

Adds DocValues to existing non-DocValued fields in a Lucene/Solr index, without full re-index

Fields in Lucene/Solr can be indexed, stored and DocValued. DocValued fields are used for functions requiring fast
bulk access to field content. Faceting and sorting are examples.  DocValues is enabled on a per-field basis.
Normally a full re-index of the Lucene/Solr index is required if one want to enable DocValues for a field.

## Requirements

Only fields with stored values can be converted to DocValues.

The conversion requires all unique values of DV-needing String fields to be stored in-memory in a TreeSet.
This is fairly memory-intensive.

## Status

Currently at the Proof of Concept stage: The unit test exposes a single stored String field as a DocValued field.

## Contact

Developed by Thomas Egense (teg@statsbiblioteket.dk) and Toke Eskildsen (te@statsbiblioteket.dk) 2014.
Feel free to send emails with comments and questions.
