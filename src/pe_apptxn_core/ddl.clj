(ns pe-apptxn-core.ddl
  (:require [clojure.java.jdbc :as j]
            [pe-jdbc-utils.core :as jcore]
            [pe-user-core.ddl :as uddl]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Table names
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def tbl-apptxn-usecase     "apptxn_usecase")
(def tbl-apptxn-usecase-log "apptxn_usecase_log")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DDL vars
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v0-create-apptxn-usecase-ddl
  (str (format "CREATE TABLE IF NOT EXISTS %s (" tbl-apptxn-usecase)
       "id                       serial  PRIMARY KEY, "
       (format "user_id          integer NULL REFERENCES %s (id), " uddl/tbl-user-account)
       "trace_id                 text    UNIQUE NOT NULL, "
       "uc_code                  integer NOT NULL)"))

(def v0-create-apptxn-usecase-log-ddl
  (str (format "CREATE TABLE IF NOT EXISTS %s (" tbl-apptxn-usecase-log)
       "id                       serial      PRIMARY KEY, "
       (format "uc_id            integer     NOT NULL REFERENCES %s (id), " tbl-apptxn-usecase)
       (format "user_id          integer     NULL     REFERENCES %s (id), " uddl/tbl-user-account)
       (format "auth_token_id    integer     NULL     REFERENCES %s (id), " uddl/tbl-auth-token)
       "user_agent_device_manu   text        NULL, "
       "user_agent_device_model  text        NULL, "
       "user_agent_device_os     integer     NULL, "
       "user_agent_device_os_ver text        NULL, "
       "browser_user_agent       text        NULL, "
       "logged_at                timestamptz NOT NULL, "
       "event_type               integer     NOT NULL, "
       "in_ctx_err_code          integer     NULL, "
       "in_ctx_err_desc          text        NULL)"))
