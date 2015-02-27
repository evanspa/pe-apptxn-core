(ns pe-apptxn-core.test-utils)

(def apptxn-schema-files ["apptxn-logging-schema-updates-0.0.1.dtm"])

(def db-uri "datomic:mem://apptxns")

(def apptxn-partition
  "The name of the Datomic partition of the application transactions."
  :apptxn)
