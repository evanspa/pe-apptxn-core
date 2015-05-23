(ns pe-apptxn-core.core-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [pe-core-utils.core :as ucore]
            [pe-user-core.core :as usercore]
            [pe-apptxn-core.test-utils :refer [db-spec-without-db
                                               db-spec
                                               db-name]]
            [clojure.java.jdbc :as j]
            [pe-jdbc-utils.core :as jcore]
            [pe-apptxn-core.core :as core]
            [clojure.java.io :refer [resource]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [pe-user-core.ddl :as uddl]
            [pe-apptxn-core.ddl :as ddl]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fixtures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(use-fixtures :each (fn [f]
                      (jcore/drop-database db-spec-without-db db-name)
                      (jcore/create-database db-spec-without-db db-name)
                      (j/db-do-commands db-spec
                                        true
                                        uddl/schema-version-ddl
                                        uddl/v0-create-user-account-ddl
                                        uddl/v0-add-unique-constraint-user-account-email
                                        uddl/v0-add-unique-constraint-user-account-username
                                        uddl/v0-create-authentication-token-ddl
                                        uddl/v0-add-column-user-account-updated-w-auth-token
                                        ddl/v0-create-apptxn-usecase-ddl
                                        ddl/v0-create-apptxn-usecase-log-ddl)
                      (jcore/with-try-catch-exec-as-query db-spec
                        (uddl/v0-create-updated-count-inc-trigger-function-fn db-spec))
                      (jcore/with-try-catch-exec-as-query db-spec
                        (uddl/v0-create-user-account-updated-count-trigger-fn db-spec))
                      (f)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest Saving-Individual-Transaction-Use-Cases-and-Logs
  (testing "Simplest Case - single use case and logs sequentially"
    (j/with-db-transaction [conn db-spec]
      (let [new-user-id (usercore/next-user-account-id conn)
            new-apptxn-uc-id (core/next-apptxn-usecase-id conn)
            new-apptxn-uc-log-id-1 (core/next-apptxn-usecase-log-id conn)
            new-apptxn-uc-log-id-2 (core/next-apptxn-usecase-log-id conn)
            t1 (t/now)
            new-token-id (usercore/next-auth-token-id conn)
            trace-id (str (java.util.UUID/randomUUID))
            uc-code 17
            t2 (t/now)
            t3 (t/now)]
        (usercore/save-new-user conn
                                new-user-id
                                {:user/username "smithj"
                                 :user/email "smithj@test.com"
                                 :user/name "John Smith"
                                 :user/created-at t1
                                 :user/password "insecure"})
        (usercore/create-and-save-auth-token conn new-user-id new-token-id)
        (core/save-new-apptxn-usecase conn
                                      new-user-id
                                      new-apptxn-uc-id
                                      {:apptxn/trace-id trace-id
                                       :apptxn/usecase uc-code})
        (let [apptxn-rs (j/query conn
                                 [(format "select * from %s where id = ?"
                                          ddl/tbl-apptxn-usecase)
                                  new-apptxn-uc-id]
                                 :result-set-fn first)]
          (is (not (nil? apptxn-rs)))
          (is (= new-apptxn-uc-id (:id apptxn-rs)))
          (is (= new-user-id (:user_id apptxn-rs)))
          (is (= trace-id (:trace_id apptxn-rs)))
          (is (= uc-code (:uc_code apptxn-rs))))
        (core/save-new-apptxn-usecase-log conn
                                          new-user-id
                                          new-token-id
                                          new-apptxn-uc-id
                                          new-apptxn-uc-log-id-1
                                          {:apptxnlog/logged-at t2
                                           :apptxnlog/event-type 32
                                           :apptxnlog/in-ctx-err-desc "some err"
                                           :apptxnlog/in-ctx-err-code 4})
        (let [apptxn-log-rs (j/query conn
                                     [(format "select * from %s where id = ?"
                                              ddl/tbl-apptxn-usecase-log)
                                      new-apptxn-uc-log-id-1]
                                     :result-set-fn first)]
          (is (not (nil? apptxn-log-rs)))
          (is (= new-user-id (:user_id apptxn-log-rs)))
          (is (= new-token-id (:auth_token_id apptxn-log-rs)))
          (is (= new-apptxn-uc-log-id-1 (:id apptxn-log-rs)))
          (is (= new-apptxn-uc-id (:uc_id apptxn-log-rs)))
          (is (= t2 (c/from-sql-date (:logged_at apptxn-log-rs))))
          (is (= 32 (:event_type apptxn-log-rs)))
          (is (= 4 (:in_ctx_err_code apptxn-log-rs)))
          (is (= "some err" (:in_ctx_err_desc apptxn-log-rs)))
          (is (nil? ())))
        (core/save-new-apptxn-usecase-log conn
                                          new-user-id
                                          new-apptxn-uc-id
                                          new-apptxn-uc-log-id-2
                                          {:apptxnlog/logged-at t3
                                           :apptxnlog/event-type 33
                                           :apptxnlog/ua-device-manu "Samsung"
                                           :apptxnlog/ua-device-model "Galaxy S5"
                                           :apptxnlog/ua-device-os usercore/uados-cyanogenmod
                                           :apptxnlog/ua-device-os-ver "1.2.5"
                                           :apptxnlog/browser-ua "Gecko/Mozilla"})
        (let [apptxn-log-rs (j/query conn
                                     [(format "select * from %s where id = ?"
                                              ddl/tbl-apptxn-usecase-log)
                                      new-apptxn-uc-log-id-2]
                                     :result-set-fn first)]
          (is (not (nil? apptxn-log-rs)))
          (is (= new-user-id (:user_id apptxn-log-rs)))
          (is (nil? (:auth_token_id apptxn-log-rs)))
          (is (= new-apptxn-uc-log-id-2 (:id apptxn-log-rs)))
          (is (= new-apptxn-uc-id (:uc_id apptxn-log-rs)))
          (is (= t3 (c/from-sql-date (:logged_at apptxn-log-rs))))
          (is (= 33 (:event_type apptxn-log-rs)))
          (is (nil? (:in_ctx_err_code apptxn-log-rs)))
          (is (nil? (:in_ctx_err_desc apptxn-log-rs))))))))


#_(deftest Loading-and-Saving-Transactions
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
