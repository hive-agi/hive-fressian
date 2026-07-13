(ns hive-fressian.codec
  "Envelope codec: one durable Fressian file = header map + item stream.

   Layout (in stream order): header, item-count, items. Handlers resolve
   through hive-fressian.registry at call time, so any domain registered
   before the call serializes transparently."
  (:require [clojure.data.fressian :as fress]
            [clojure.java.io :as io]
            [hive-fressian.registry :as registry]))

(def format-version 1)

(defn write-envelope!
  "Write `items` (seqable) under `header` (map) to `path`.
   Returns {:path .. :count ..}."
  [path header items]
  (let [file (io/file path)
        items (vec items)]
    (io/make-parents file)
    (with-open [out (io/output-stream file)]
      (let [w (fress/create-writer out :handlers (registry/write-handler-lookup))]
        (fress/write-object w (assoc header :envelope/version format-version))
        (fress/write-object w (count items))
        (doseq [item items] (fress/write-object w item))))
    {:path (.getPath file) :count (count items)}))

(defn read-envelope
  "Read an envelope from `path`. Returns {:header map :items [..]}."
  [path]
  (with-open [in (io/input-stream (io/file path))]
    (let [r (fress/create-reader in :handlers (registry/read-handler-lookup))
          header (fress/read-object r)
          n (long (fress/read-object r))
          items (mapv (fn [_] (fress/read-object r)) (range n))]
      {:header header :items items})))
