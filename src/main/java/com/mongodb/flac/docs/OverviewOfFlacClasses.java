package com.mongodb.flac.docs;

/**
 * Documentation only class that gives a high level overview of the class files.
 *
 * <p>Intro the source files:
 * </p>
 *
 * <p> Classes  RedactedDBCollection  and UserSecurityAttributesMap  are the main classes that you will work with.   Class UserSecurityAttributesMap is designed to be subclassed as needed.  E.g. we also have a UserSecurityAttributesMapCapco class that implements CAPCO behavior such as clearance TS also means a person with TS also inherits clearances (S, C, and U).
 * </p>
 *
 * <p>
 Classes that you use:
 </p>
 <b>RedactedDBCollection</b>                        wrap a DBCollection such that all find/aggregate operations are safe because they first pass thru a security check using $redact
 in mongodb
 </p>
 <p>

 <b>UserSecurityAttributesMap</b>
 (or a subclass like UserSecurityAttributesMapCapco)   stores the current user permissions
 </p>

 <p>
 We have other utility classes that may be useful to make it easy to inject security permissions into a UserSecurityAttributesMap  on an annotation based approach.
 See the code in package com.mongodb.flac.converter that defines an annotation called FLACProperty and various helper classes to make it easy to find and process these annotations.
 </p>
 */
public class OverviewOfFlacClasses {
}
