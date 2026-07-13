(ns hive-fressian.registry
  "Handler-domain registry — the OCP swap point of hive-fressian.

   A DOMAIN is a named bundle of Fressian handlers for one family of types:
     {:write-handlers {Class {\"tag\" org.fressian.handlers.WriteHandler}}
      :read-handlers  {\"tag\" org.fressian.handlers.ReadHandler}}

   Consumers register their domain once (register!); every codec built
   afterwards sees the merged handler maps. New type family = new domain,
   zero change to the codec."
  (:require [clojure.data.fressian :as fress]))

(defonce ^:private domains (atom {}))

(defn register!
  "Register (or replace) handler `domain` under `domain-key` (keyword).
   Returns the domain-key."
  [domain-key domain]
  (swap! domains assoc domain-key domain)
  domain-key)

(defn deregister!
  [domain-key]
  (swap! domains dissoc domain-key)
  nil)

(defn registered
  "Registered domain keys."
  []
  (set (keys @domains)))

(defn merge-domains
  "Pure merge of domain maps (later domains win per key)."
  [domain-maps]
  {:write-handlers (into {} (mapcat :write-handlers) domain-maps)
   :read-handlers (into {} (mapcat :read-handlers) domain-maps)})

(defn write-handler-lookup
  "Fressian write-handler lookup: clojure defaults + every registered domain."
  []
  (-> (merge fress/clojure-write-handlers
             (:write-handlers (merge-domains (vals @domains))))
      fress/associative-lookup
      fress/inheritance-lookup))

(defn read-handler-lookup
  "Fressian read-handler lookup: clojure defaults + every registered domain."
  []
  (fress/associative-lookup
   (merge fress/clojure-read-handlers
          (:read-handlers (merge-domains (vals @domains))))))
