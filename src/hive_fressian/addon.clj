(ns hive-fressian.addon
  "IAddon boundary for hive-fressian (library-kind addon: contributes a
   serialization capability, no tools). Hosts discover the registry/codec
   through the capability; consumers call the namespaces directly."
  (:require [hive-addon.protocol :as addon]
            [hive-fressian.registry :as registry]))

(def addon-id-value "hive.fressian")

(defrecord HiveFressianAddon [seed]
  addon/IAddon
  (addon-id [_] addon-id-value)
  (addon-type [_] :native)
  (capabilities [_] #{:health-reporting :serialization/fressian})
  (initialize! [_ _config]
    {:success? true :errors [] :metadata {:domains (registry/registered)}})
  (shutdown! [_] nil)
  (tools [_] [])
  (schema-extensions [_] [])
  (health [_]
    {:status :ok :details {:domains (registry/registered)}})
  (excluded-tools [_] #{})
  (hooks [_] {}))

(defn init-as-addon!
  ([] (init-as-addon! {}))
  ([seed] (->HiveFressianAddon seed)))
