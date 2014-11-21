# DVEnabler

Adds DocValues to existing non-DocValued fields in a Solr index, without full re-index

Fields in Solr can be indexed, stored and DocValued. DocValued fields are used for functions requiring fast bulk access to field content. Faceting and sorting are examples.  DocValues is enabled on a per-field basis. Normally a full re-index of the Solr index is required if one want to enable DocValues for a field.

## Solr index fields

Field content can be stored (at least) three ways in Solr:

 * docValues=true stores the input value verbatim. Works well for faceting &amp; sorting and is the goal for this utility
 * stored=true stores the input value verbatim. If present, this utility will construct DocValues for the field using the stored values. This is a "perfect" conversion; the end result is the same as if DocValues had been enabled from the beginning.
 * indexed=true analyzes and transforms the input value. It is memory- and CPU-intensive to UnInvert and the end result will only be correct if the analyzing &amp; transform phase has not changed the input. Support for converting indexed values to DocValues might be added later to this utility.

## Operation

## Contact

Developed by Thomas Egense (teg@statsbiblioteket.dk) and Toke Eskildsen (te@statsbiblioteket.dk) 2014.
Feel free to send emails with comments and questions.
