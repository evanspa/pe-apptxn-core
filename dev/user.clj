(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.java.jdbc :as j]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.test :as test]
            [clojure.stacktrace :refer (e)]
            [pe-core-utils.core :as ucore]
            [pe-user-core.core :as usercore]
            [pe-jdbc-utils.core :as jcore]
            [pe-apptxn-core.test-utils :refer [db-spec-without-db
                                               db-spec
                                               db-spec-fn]]
            [pe-user-core.ddl :as uddl]
            [pe-apptxn-core.ddl :as ddl]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]))

(def dev-db-name "dev")

(def db-spec-dev (db-spec-fn dev-db-name))

(defn refresh-dev-db
  []
  (jcore/drop-database db-spec-without-db dev-db-name)
  (jcore/create-database db-spec-without-db dev-db-name)
  (j/db-do-commands db-spec-dev
                    true
                    uddl/schema-version-ddl
                    uddl/v0-create-user-account-ddl
                    uddl/v0-add-unique-constraint-user-account-email
                    uddl/v0-add-unique-constraint-user-account-username
                    uddl/v0-create-authentication-token-ddl
                    uddl/v0-add-column-user-account-updated-w-auth-token
                    ddl/v0-create-apptxn-usecase-ddl
                    ddl/v0-create-apptxn-usecase-log-ddl)
  (jcore/with-try-catch-exec-as-query db-spec-dev
    (uddl/v0-create-updated-count-inc-trigger-function-fn db-spec-dev))
  (jcore/with-try-catch-exec-as-query db-spec-dev
    (uddl/v0-create-user-account-updated-count-trigger-fn db-spec-dev)))
