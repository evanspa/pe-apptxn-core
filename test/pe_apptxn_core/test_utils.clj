(ns pe-apptxn-core.test-utils)

(def apptxn-schema-filename "apptxn-logging-schema.dtm")

(def db-uri "datomic:mem://apptxns")

(def apptxn-partition
  "The name of the Datomic partition of the application transactions."
  :apptxn)
