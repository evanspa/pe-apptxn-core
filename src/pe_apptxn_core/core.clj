(ns pe-apptxn-core.core
  "The set of functions encapsulating the data model and data access functions
  of the PEAppTransaction Logging Framework."
  (:require [pe-jdbc-utils.core :as jcore]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.java.jdbc :as j]
            [pe-apptxn-core.ddl :as ddl]
            [pe-core-utils.core :as ucore]
            [clojure.tools.logging :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn next-apptxn-usecase-id
  [db-spec]
  (jcore/seq-next-val db-spec "apptxn_usecase_id_seq"))

(defn next-apptxn-usecase-log-id
  [db-spec]
  (jcore/seq-next-val db-spec "apptxn_usecase_log_id_seq"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Saving an individual apptxn usecase
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn save-new-apptxn-usecase
  ([db-spec
    new-id
    apptxn-usecase]
   (save-new-apptxn-usecase db-spec
                            nil
                            new-id
                            apptxn-usecase))
  ([db-spec
    user-id
    new-id
    apptxn-usecase]
   (j/insert! db-spec
              :apptxn_usecase
              {:id new-id
               :user_id user-id
               :trace_id (:apptxn/trace-id apptxn-usecase)
               :uc_code (:apptxn/usecase apptxn-usecase)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Saving an individual apptxn usecase log w/out parent-existence check
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn save-new-apptxn-usecase-log
  [db-spec user-id auth-token-id uc-id new-id apptxn-usecase-log]
  (let [])
  (let [apptxn-usecase-id (j/query db-spec
                                   [(format "select id from %s where trace_id = ?" ddl/tbl-apptxn-usecase) ])])
  (j/insert! db-spec
             :apptxn_usecase_log
             {:id new-id
              :user_id user-id
              :auth_token_id auth-token-id
              :uc_id uc-id
              :user_agent_device_manu   (:apptxnlog/ua-device-manu apptxn-usecase-log)
              :user_agent_device_model  (:apptxnlog/ua-device-model apptxn-usecase-log)
              :user_agent_device_os     (:apptxnlog/ua-device-os apptxn-usecase-log)
              :user_agent_device_os_ver (:apptxnlog/ua-device-os-ver apptxn-usecase-log)
              :browser_user_agent       (:apptxnlog/browser-ua apptxn-usecase-log)
              :logged_at                (c/to-timestamp (:apptxnlog/logged-at apptxn-usecase-log))
              :event_type               (:apptxnlog/event-type apptxn-usecase-log)
              :error_desc               (:apptxnlog/error-desc apptxn-usecase-log)}))

(defn save-new-apptxn-usecase-log
  ([db-spec
    uc-id
    new-id
    apptxn-usecase-log]
   (save-new-apptxn-usecase-log db-spec
                                nil
                                uc-id
                                new-id
                                apptxn-usecase-log))
  ([db-spec
    user-id
    uc-id
    new-id
    apptxn-usecase-log]
   (save-new-apptxn-usecase-log db-spec
                                user-id
                                nil
                                uc-id
                                new-id
                                apptxn-usecase-log))
  ([db-spec
    user-id
    auth-token-id
    uc-id
    new-id
    apptxn-usecase-log]
   (let [insert-map {:id new-id
                     :user_id         user-id
                     :uc_id           uc-id
                     :logged_at       (c/to-timestamp (:apptxnlog/logged-at apptxn-usecase-log))
                     :event_type      (:apptxnlog/event-type apptxn-usecase-log)
                     :in_ctx_err_code (:apptxnlog/in-ctx-err-code apptxn-usecase-log)
                     :in_ctx_err_desc (:apptxnlog/in-ctx-err-desc apptxn-usecase-log)}
         insert-map (merge insert-map
                           (if auth-token-id
                             {:auth_token_id auth-token-id}
                             {:user_agent_device_manu   (:apptxnlog/ua-device-manu apptxn-usecase-log)
                              :user_agent_device_model  (:apptxnlog/ua-device-model apptxn-usecase-log)
                              :user_agent_device_os     (:apptxnlog/ua-device-os apptxn-usecase-log)
                              :user_agent_device_os_ver (:apptxnlog/ua-device-os-ver apptxn-usecase-log)
                              :browser_user_agent       (:apptxnlog/browser-ua apptxn-usecase-log)}))]
     (j/insert! db-spec :apptxn_usecase_log insert-map))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Saving an apptxn usecase log(s) with parent-existence check
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


#_(declare apptxnlogs-for-apptxnid)
#_(declare save-apptxnlog-txn)

#_(defn all-apptxns
  "Returns the set of application transactions found in the database."
  [conn]
  (let [apptxns-rel (q '[:find (pull ?apptxn [*]) :where [?apptxn :apptxn/id]] (db conn))
        apptxns (first apptxns-rel)]
    (map
     (fn [txn-tuple]
       (let [txn-ent (first txn-tuple)
             txnlogs-rel (apptxnlogs-for-apptxnid conn (:apptxn/id txn-ent))
             loaded-txnlogs (map
                             (fn [txnlog-tuple]
                               (let [txnlog-entid (first txnlog-tuple)]
                                 (into {} (d/entity (db conn) txnlog-entid))))
                             txnlogs-rel)]
         (assoc txn-ent :apptxn/logs loaded-txnlogs)))
     apptxns-rel)))

#_(defn find-apptxn-by-id
  "Returns the application transaction instance with the given id."
  [conn apptxn-id]
  (ffirst (q '[:find ?apptxn
               :in $ ?apptxn-id
               :where [?apptxn :apptxn/id ?apptxn-id]]
             (db conn)
             apptxn-id)))

#_(defn save-apptxnset-txnmaps
  "Saves (transacts) the given apptxnset for the given user entity ID, to
  partition."
  [conn partition user-entid apptxnset]
  (let [apptxns (:apptxns apptxnset)]
    (vec (reduce (fn [overalltxn apptxn]
                   (concat overalltxn
                           (let [{apptxn-id :apptxn/id
                                  apptxn-usecase :apptxn/usecase
                                  apptxn-user-agent-device-make :apptxn/user-agent-device-make
                                  apptxn-user-agent-device-os :apptxn/user-agent-device-os
                                  apptxn-user-agent-device-os-version :apptxn/user-agent-device-os-version} apptxn
                                  apptxnlogs (:apptxn/logs apptxn)
                                  first-apptxnlog (first apptxnlogs)
                                  {apptxnlog-usecase-event :apptxnlog/usecase-event
                                   apptxnlog-timestamp :apptxnlog/timestamp
                                   apptxnlog-in-ctx-err-code :apptxnlog/in-ctx-err-code
                                   apptxnlog-in-ctx-err-desc :apptxnlog/in-ctx-err-desc
                                   apptxnlog-edn-ctx :apptxnlog/edn-ctx} first-apptxnlog
                                   txn (save-apptxnlog-txn conn
                                                           partition
                                                           apptxn-id
                                                           apptxn-usecase
                                                           apptxn-user-agent-device-make
                                                           apptxn-user-agent-device-os
                                                           apptxn-user-agent-device-os-version
                                                           apptxnlog-usecase-event
                                                           apptxnlog-timestamp
                                                           apptxnlog-in-ctx-err-code
                                                           apptxnlog-in-ctx-err-desc
                                                           apptxnlog-edn-ctx)
                                   apptxn-entid (if (= 1 (count txn))
                                                  (:apptxnlog/txn (first txn))
                                                  (:apptxnlog/txn (second txn)))
                                   rest-apptxnlogs (rest apptxnlogs)]
                             (reduce (fn [txn apptxnlog]
                                       (conj txn (merge (ucore/remove-nils apptxnlog)
                                                        {:db/id (d/tempid partition)
                                                         :apptxnlog/txn apptxn-entid})))
                                     txn
                                     rest-apptxnlogs))))
                 []
                 apptxns))))

#_(defn save-apptxn-txnmap
  "Returns a application transaction map for the given partition, suitable for
  inclusion in a Datomic transaction.  The parameters are as follows:
  apptxn-usecase - The use case to be associated with the application
  transaction.
  apptxn-user-agent-device-make - The originating user-agent device make name.
  apptxn-user-agent-device-os - The originating user-agent device operating
  system name.
  apptxn-user-agent-device-os-version - The originating user-agent device
  operation system version."
  [partition
   apptxn-id
   apptxn-usecase
   apptxn-user-agent-device-make
   apptxn-user-agent-device-os
   apptxn-user-agent-device-os-version]
  (let [newapptxn-tempid (d/tempid partition)]
    (merge {:db/id newapptxn-tempid
            :apptxn/id apptxn-id}
           (when apptxn-usecase
             {:apptxn/usecase apptxn-usecase})
           (when apptxn-user-agent-device-make
             {:apptxn/user-agent-device-make apptxn-user-agent-device-make})
           (when apptxn-user-agent-device-os
             {:apptxn/user-agent-device-os apptxn-user-agent-device-os})
           (when apptxn-user-agent-device-os-version
             {:apptxn/user-agent-device-os-version apptxn-user-agent-device-os-version}))))

#_(defn save-apptxnlog-txn
  "Returns a transaction vector.  If the given application transaction ID is
   found in the database, the transaction vector returned consists of a map for
   the creation of the application transaction event log, as well as a map for
   associating the new event log with the found transaction entity.

   If the given application transaction ID is NOT found, the transaction vector
   returned consists of a map for the creation of the transaction entity, as
   well as a map for the creation of the event log and a map for associating the
   new event log with the transaction."
  ([conn
    partition
    apptxn-id
    apptxnlog-usecase-event
    apptxnlog-event-timestamp
    apptxnlog-event-in-ctx-err-code
    apptxnlog-event-in-ctx-err-desc
    apptxnlog-event-edn-ctx]
   (save-apptxnlog-txn conn
                       partition
                       apptxn-id
                       nil
                       nil
                       nil
                       nil
                       apptxnlog-usecase-event
                       apptxnlog-event-timestamp
                       apptxnlog-event-in-ctx-err-code
                       apptxnlog-event-in-ctx-err-desc
                       apptxnlog-event-edn-ctx))
  ([conn
    partition
    apptxn-id
    apptxn-usecase
    apptxn-user-agent-device-make
    apptxn-user-agent-device-os
    apptxn-user-agent-device-os-version
    apptxnlog-usecase-event
    apptxnlog-event-timestamp
    apptxnlog-event-in-ctx-err-code
    apptxnlog-event-in-ctx-err-desc
    apptxnlog-event-edn-ctx]
   (let [newapptxnlog-tempid (d/tempid partition)
         apptxn-entid (find-apptxn-by-id conn apptxn-id)
         newapptxnlog-txnmap (merge {:db/id newapptxnlog-tempid}
                                    (when apptxnlog-usecase-event
                                      {:apptxnlog/usecase-event apptxnlog-usecase-event})
                                    (when apptxnlog-event-timestamp
                                      {:apptxnlog/timestamp apptxnlog-event-timestamp})
                                    (when apptxnlog-event-in-ctx-err-code
                                      {:apptxnlog/in-ctx-err-code apptxnlog-event-in-ctx-err-code})
                                    (when apptxnlog-event-in-ctx-err-desc
                                      {:apptxnlog/in-ctx-err-desc apptxnlog-event-in-ctx-err-desc})
                                    (when apptxnlog-event-edn-ctx
                                      {:apptxnlog/edn-ctx apptxnlog-event-edn-ctx}))]
     (if (not (nil? apptxn-entid))
       [(merge newapptxnlog-txnmap {:apptxnlog/txn apptxn-entid})]
       (cond
         (nil? apptxn-usecase) (throw (IllegalArgumentException. "apptxn-usecase cannot be nil"))
         (nil? apptxn-user-agent-device-make) (throw (IllegalArgumentException. "apptxn-user-agent-device-make cannot be nil"))
         (nil? apptxn-user-agent-device-os) (throw (IllegalArgumentException. "apptxn-user-agent-device-os cannot be nil"))
         (nil? apptxn-user-agent-device-os-version) (throw (IllegalArgumentException. "apptxn-user-agent-device-os-version cannot be nil"))
         :else (let [newapptxn-txnmap (save-apptxn-txnmap partition
                                                          apptxn-id
                                                          apptxn-usecase
                                                          apptxn-user-agent-device-make
                                                          apptxn-user-agent-device-os
                                                          apptxn-user-agent-device-os-version)
                     {newapptxn-entid :db/id} newapptxn-txnmap]
                 [newapptxn-txnmap           ; New app txn
                  (merge newapptxnlog-txnmap ; New app txn log w/parent txn associated with it
                         {:apptxnlog/txn newapptxn-entid})]))))))

#_(defn apptxnlogs-for-apptxnid
  "Returns the set of application transaction logs for the given application
  transaction entity ID."
  [conn apptxn-id]
  (q '[:find ?apptxnlog
       :in $ ?apptxn-id
       :where [$ ?apptxn :apptxn/id ?apptxn-id]
              [$ ?apptxnlog :apptxnlog/txn ?apptxn]]
     (db conn)
     apptxn-id))
