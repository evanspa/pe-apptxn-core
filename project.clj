(defproject pe-apptxn-core "0.1.0-SNAPSHOT"
  :description "A Clojure library encapsulating the data model and data access functions of the PEAppTransaction Logging framework."
  :url "https://github.com/evanspa/pe-apptxn-core"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :plugins [[lein-pprint "1.1.2"]
            [codox "0.8.10"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [pe-core-utils "0.0.10"]
                 [pe-jdbc-utils "0.0.2"]
                 [pe-user-core "0.1.5"]
                 [ch.qos.logback/logback-classic "1.0.13"]
                 [org.slf4j/slf4j-api "1.7.5"]
                 [clj-time "0.8.0"]
                 [org.clojure/java.jdbc "0.3.6"]]
  :resource-paths ["resources"]
  :codox {:exclude [user]
          :src-dir-uri "https://github.com/evanspa/pe-apptxn-core/blob/0.1.0/"
          :src-linenum-anchor-prefix "L"}
  :profiles {:dev {:source-paths ["dev"]
                   :plugins [[cider/cider-nrepl "0.9.0-SNAPSHOT"]]
                   :dependencies [[org.clojure/tools.namespace "0.2.7"]
                                  [org.clojure/java.classpath "0.2.2"]
                                  [org.clojure/data.json "0.2.5"]
                                  [org.postgresql/postgresql "9.4-1201-jdbc41"]
                                  [org.clojure/tools.nrepl "0.2.7"]]
                   :resource-paths ["test-resources"]}
             :test {:resource-paths ["test-resources"]}}
  :jvm-opts ["-Xmx1g" "-DAPPTXNCORE_LOGS_DIR=logs"]
  :repositories [["releases" {:url "https://clojars.org/repo"
                              :creds :gpg}]])
