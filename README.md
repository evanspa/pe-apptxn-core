# pe-apptxn-core

[![Build Status](https://travis-ci.org/evanspa/pe-apptxn-core.svg)](https://travis-ci.org/evanspa/pe-apptxn-core)

A Clojure library encapsulating the server-side data model and data access functions of the
[PEAppTransaction Logging Framework](#peapptransaction-logging-framework).  [Datomic](http://www.datomic.com) is used as the underlying data store.

If you're looking for a REST interface,
[pe-apptxn-restsupport](https://github.com/evanspa/pe-apptxn-restsupport) exists
to provide a REST API on top of the functionality provided by pe-apptxn-core.

pe-apptxn-core is part of the
[pe-* Clojure Library Suite](#pe--clojure-library-suite).

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**
- [PEAppTransaction Logging Framework](#peapptransaction-logging-framework)
  - [Motivation](#motivation)
- [Documentation](#documentation)
- [Installation](#installation)
- [pe-* Clojure Library Suite](#pe--clojure-library-suite)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## PEAppTransaction Logging Framework

The PEAppTransaction Logging Framework (PELF) is a framework for capturing important
events within your applications.  It is comprised of 3 main tiers: (1)
the core data layer, (2)
the [web service layer](https://github.com/evanspa/pe-apptxn-restsupport)
and (3) client libraries ([currently only iOS](https://github.com/evanspa/PEAppTransaction-Logger)).

The PELF provides a small set of functions to log what are termed "application transactions."  An application transaction is not a transaction in a database sense; it is meant more to model application-level actions.  For example, a user creating a new purchase order within your application would be termed as an application transaction.  A user signing in to your application would be an application transaction.  A user signing out, another.  

The PELF provides both client-side and server-side libraries.  pe-apptxn-core (*this library*), is a server-side library encapsulating the model and data access.  PELF client-side libraries are used by applications to locally record user-initiated application transactions and transaction events.  Transaction events are simply timestamped events associated with an application transaction.  E.g., if "create a purchase order" is an application transaction, a transaction event might be: "user clicks 'New PO' button to initiate transaction."  Another event might be: "web service request initiated to submit new PO data to server".  And another: "web service response received".  All of this log data is saved locally on the client, and then later pushed to the server for permanent storage (and offline analysis).

### Motivation

The motivation behind the creation of the PEAppTransaction logging framework is
simple: to systematically track important events about the goings-on within an
application (*an end-user application or otherwise*).  You can use the framework
to record information about your application, such as:
+ How long a user takes to fill-out a form (or more generally, a call-to-action).
+ What the round-trip time is for making web service calls.
+ How often a user leaves a screen in your app without fulfilling the
call-to-action.

The set of use cases for which to use the framework to log metadata is
open-ended.  You can use it for basic A/B testing, tracking performance-related
metrics and many more.

## Documentation

* [API Docs](http://evanspa.github.com/pe-apptxn-core)

## Installation

pe-apptxn-core is available from Clojars.  Add the following dependency to your
`project.clj` file:

```
[pe-apptxn-core "0.0.2"]
```

## pe-* Clojure Library Suite
The pe-* Clojure library suite is a set of Clojure libraries to aid in the
development of Clojure and Java based applications.
*(Each library is available on Clojars.)*
+ **[pe-core-utils](https://github.com/evanspa/pe-core-utils)**: provides a set
  of various collection-related, date-related and other helpers functions.
+ **[pe-datomic-utils](https://github.com/evanspa/pe-datomic-utils)**: provides
  a set of helper functions for working with [Datomic](https://www.datomic.com).
+ **[pe-datomic-testutils](https://github.com/evanspa/pe-datomic-testutils)**: provides
  a set of helper functions to aid in unit testing Datomic-enabled functions.
+ **[pe-user-core](https://github.com/evanspa/pe-user-core)**: provides
  a set of functions for modeling a generic user, leveraging Datomic as a
  backend store.
+ **[pe-user-testutils](https://github.com/evanspa/pe-user-testutils)**: a set of helper functions to aid in unit testing
  code that depends on the functionality of the pe-user-* libraries
  ([pe-user-core](https://github.com/evanspa/pe-user-core) and [pe-user-rest](https://github.com/evanspa/pe-user-rest)).
+ **pe-apptxn-core**: this library.
+ **[pe-rest-utils](https://github.com/evanspa/pe-rest-utils)**: provides a set
  of functions for building easy-to-version hypermedia REST services (built on
  top of [Liberator](http://clojure-liberator.github.io/liberator/).
+ **[pe-rest-testutils](https://github.com/evanspa/pe-rest-testutils)**: provides
  a set of helper functions for unit testing web services.
+ **[pe-user-rest](https://github.com/evanspa/pe-user-rest)**: provides a set of
  functions encapsulating an abstraction modeling a user within a REST API
  and leveraging [Datomic](http://www.datomic.com).
+ **[pe-apptxn-restsupport](https://github.com/evanspa/pe-apptxn-restsupport)**:
  provides a set of functions implementing the server-side REST layer of the
  PEAppTransaction Logging Framework.
