(ns pe-apptxn-core.core-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [pe-core-utils.core :as ucore]
            [pe-core-testutils.core :as tucore]
            [pe-apptxn-core.test-utils :refer [apptxn-schema-filename
                                               db-uri
                                               apptxn-partition]]
            [pe-apptxn-core.core :refer :all]
            [clojure.java.io :refer [resource]]
            [datomic.api :as d]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [clj-time.core :as t]))

(def conn (atom nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fixtures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(use-fixtures :each (tucore/make-db-refresher-fixture-fn db-uri
                                                         conn
                                                         apptxn-partition
                                                         apptxn-schema-filename))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest Loading-and-Saving-Transactions
  (testing "Save a new app transaction set."
    (let [apptxnset (keywordize-keys
                     (-> (json/read-str (slurp (resource "apptxnset.json")))
                         (ucore/rfc7231str-dates->instants)))
          apptxnset-txn (save-apptxnset-txnmaps @conn apptxn-partition nil apptxnset)]
      @(d/transact @conn apptxnset-txn)
      (let [apptxn-id "TXN1-3BB05024-B7F6-4B66-838B-F6982FCB08DF"
            apptxn-entid (find-apptxn-by-id @conn apptxn-id)]
        (is (not (nil? apptxn-entid)))
        (let [apptxn-ent (d/entity (d/db @conn) apptxn-entid)]
          (is (not (nil? apptxn-ent)))
          (is (= (:apptxn/usecase apptxn-ent) 1))
          (is (= (:apptxn/user-agent-device-make apptxn-ent) "x86_64"))
          (is (= (:apptxn/user-agent-device-os apptxn-ent) "iPhone OS"))
          (is (= (:apptxn/user-agent-device-os-version apptxn-ent) "8.1"))
          (let [apptxnlogs (apptxnlogs-for-apptxnid @conn apptxn-id)]
            (is (= 2 (count apptxnlogs)))
            (let [apptxnlog-entid (ffirst apptxnlogs)
                  apptxnlog-ent (d/entity (d/db @conn) apptxnlog-entid)]
              (is (= 1 (:apptxnlog/usecase-event apptxnlog-ent)))
              (is (nil? (:apptxnlog/in-ctx-err-code apptxnlog-ent)))
              (is (nil? (:apptxnlog/in-ctx-err-desc apptxnlog-ent)))
              (is (nil? (:apptxnlog/edn-ctx apptxnlog-ent)))
              (is (= (ucore/rfc7231str->instant "Thu, 15 Jan 2015 13:11:42 EST")
                     (:apptxnlog/timestamp apptxnlog-ent))))
            (let [apptxnlog-entid (first (second apptxnlogs))
                  apptxnlog-ent (d/entity (d/db @conn) apptxnlog-entid)]
              (is (= 2 (:apptxnlog/usecase-event apptxnlog-ent)))
              (is (= 42 (:apptxnlog/in-ctx-err-code apptxnlog-ent)))
              (is (= "Baad!" (:apptxnlog/in-ctx-err-desc apptxnlog-ent)))
              (is (= "{:request {}}" (:apptxnlog/edn-ctx apptxnlog-ent)))
              (is (= (ucore/rfc7231str->instant "Fri, 16 Jan 2015 13:11:42 EST")
                     (:apptxnlog/timestamp apptxnlog-ent)))))))
      (let [apptxn-id "TXN6-C1F56AD9-0685-4D55-88A9-509B94B9FB1A"
            apptxn-entid (find-apptxn-by-id @conn apptxn-id)]
        (is (not (nil? apptxn-entid)))
        (let [apptxn-ent (d/entity (d/db @conn) apptxn-entid)]
          (is (not (nil? apptxn-ent)))
          (is (= (:apptxn/usecase apptxn-ent) 6))
          (is (= (:apptxn/user-agent-device-make apptxn-ent) "x86_64"))
          (is (= (:apptxn/user-agent-device-os apptxn-ent) "iPhone OS"))
          (is (= (:apptxn/user-agent-device-os-version apptxn-ent) "8.0.4"))
          (let [apptxnlogs (apptxnlogs-for-apptxnid @conn apptxn-id)
                apptxnlogs (map #(d/entity (d/db @conn) (first %)) apptxnlogs)
                apptxnlogs (sort-by :apptxnlog/timestamp (vec apptxnlogs))]
            (is (= 3 (count apptxnlogs)))
            (let [[apptxnlog-ent] apptxnlogs]
              (is (= 3 (:apptxnlog/usecase-event apptxnlog-ent)))
              (is (nil? (:apptxnlog/in-ctx-err-code apptxnlog-ent)))
              (is (nil? (:apptxnlog/in-ctx-err-desc apptxnlog-ent)))
              (is (nil? (:apptxnlog/edn-ctx apptxnlog-ent)))
              (is (= (ucore/rfc7231str->instant "Mon, 12 Jan 2015 13:11:42 EST")
                     (:apptxnlog/timestamp apptxnlog-ent))))
            (let [[_ apptxnlog-ent] apptxnlogs]
              (is (= 1 (:apptxnlog/usecase-event apptxnlog-ent)))
              (is (= 99 (:apptxnlog/in-ctx-err-code apptxnlog-ent)))
              (is (= "Mmmkay" (:apptxnlog/in-ctx-err-desc apptxnlog-ent)))
              (is (nil? (:apptxnlog/edn-ctx apptxnlog-ent)))
              (is (= (ucore/rfc7231str->instant "Tue, 13 Jan 2015 13:11:42 EST")
                     (:apptxnlog/timestamp apptxnlog-ent))))
            (let [[_ _ apptxnlog-ent] apptxnlogs]
              (is (= 0 (:apptxnlog/usecase-event apptxnlog-ent)))
              (is (nil? (:apptxnlog/in-ctx-err-code apptxnlog-ent)))
              (is (nil? (:apptxnlog/in-ctx-err-desc apptxnlog-ent)))
              (is (nil? (:apptxnlog/edn-ctx apptxnlog-ent)))
              (is (= (ucore/rfc7231str->instant "Wed, 14 Jan 2015 13:11:42 EST")
                     (:apptxnlog/timestamp apptxnlog-ent)))))))))
  (testing "Save a new app transaction along with a couple transaction-logs."
    (let [apptxn-id "TXN89102349XTR001"]
      (is (nil? (find-apptxn-by-id @conn apptxn-id)))
      @(d/transact @conn [(save-apptxn-txnmap apptxn-partition
                                              apptxn-id
                                              15
                                              "iPhone"
                                              "iOS"
                                              "8.1.2")])
      (let [apptxn-entid (find-apptxn-by-id @conn apptxn-id)]
        (is (not (nil? apptxn-entid)))
        (let [apptxn-ent (d/entity (d/db @conn) apptxn-entid)]
          (is (not (nil? apptxn-ent)))
          (is (= (:apptxn/usecase apptxn-ent) 15))
          (is (= (:apptxn/user-agent-device-make apptxn-ent) "iPhone"))
          (is (= (:apptxn/user-agent-device-os apptxn-ent) "iOS"))
          (is (= (:apptxn/user-agent-device-os-version apptxn-ent) "8.1.2"))
          (let [apptxnlogs (apptxnlogs-for-apptxnid @conn apptxn-id)]
            (is (not (nil? apptxnlogs)))
            (is (= (count apptxnlogs) 0))
            @(d/transact @conn (save-apptxnlog-txn @conn
                                                   apptxn-partition
                                                   apptxn-id
                                                   4
                                                   (.toDate (t/date-time 1985 11 05))
                                                   nil
                                                   nil
                                                   nil))
            (let [apptxnlogs (apptxnlogs-for-apptxnid @conn apptxn-id)]
              (is (= (count apptxnlogs) 1))
              (let [apptxnlog-entid (ffirst apptxnlogs)
                    apptxnlog-ent (d/entity (d/db @conn) apptxnlog-entid)]
                (is (not (nil? apptxnlog-ent)))
                (is (= (:apptxnlog/usecase-event apptxnlog-ent) 4))
                (is (= (:apptxnlog/timestamp apptxnlog-ent) (.toDate (t/date-time 1985 11 05))))
                (is (nil? (:apptxnlog/in-ctx-err-code apptxnlog-ent)))
                (is (nil? (:apptxnlog/in-ctx-err-desc apptxnlog-ent)))
                (is (nil? (:apptxnlog/edn-ctx apptxnlog-ent)))
                @(d/transact @conn (save-apptxnlog-txn @conn
                                                       apptxn-partition
                                                       apptxn-id
                                                       5
                                                       (.toDate (t/date-time 1985 11 06))
                                                       99
                                                       "some err desc"
                                                       nil))
                (let [apptxnlogs (apptxnlogs-for-apptxnid @conn apptxn-id)]
                  (is (= (count apptxnlogs) 2))))))))))
  (testing "Save a new app transaction-log, w/non-existent app transaction."
    (let [apptxn-id "TXN0281211YZZ20199"]
      (is (nil? (find-apptxn-by-id @conn apptxn-id)))
      (is (= (count (apptxnlogs-for-apptxnid @conn apptxn-id)) 0))
      @(d/transact @conn (save-apptxnlog-txn @conn
                                             apptxn-partition
                                             apptxn-id
                                             15
                                             "iPhone"
                                             "iOS"
                                             "8.1.2"
                                             4
                                             (.toDate (t/date-time 1985 11 05))
                                             99
                                             "err desc"
                                             "{:req []}"))
      (is (not (nil? (find-apptxn-by-id @conn apptxn-id))))
      (is (= (count (apptxnlogs-for-apptxnid @conn apptxn-id)) 1))))
  (testing "Attempt to save a new app transaction w/insufficient args."
    ))
