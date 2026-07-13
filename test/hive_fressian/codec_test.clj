(ns hive-fressian.codec-test
  "Envelope roundtrip + registry OCP contracts.

   Hand-written throughout: the interesting properties are stateful
   (registry) and IO-shaped (file roundtrip) — not schema shapes."
  (:require [clojure.test :refer [deftest testing is]]
            [hive-fressian.codec :as codec]
            [hive-fressian.registry :as registry])
  (:import [org.fressian.handlers WriteHandler ReadHandler]
           [java.io File]))

(defn- tmp-path []
  (let [f (File/createTempFile "hive-fressian-test" ".fressian")]
    (.deleteOnExit f)
    (.getPath f)))

(deftest plain-data-roundtrip
  (let [path (tmp-path)
        items [{:a 1} [:b "c"] #{42} "plain" 7]
        rep (codec/write-envelope! path {:kind :test} items)]
    (is (= (count items) (:count rep)))
    (let [{:keys [header items*]} (let [{:keys [header items]} (codec/read-envelope path)]
                                    {:header header :items* items})]
      (is (= :test (:kind header)))
      (is (= codec/format-version (:envelope/version header)))
      (is (= items items*)))))

(deftest empty-envelope-roundtrip
  (let [path (tmp-path)]
    (codec/write-envelope! path {} [])
    (is (= [] (:items (codec/read-envelope path))))))

;; ── OCP: a new type family = one register! call, codec untouched ────────────

(defrecord Point [x y])

(def ^:private point-domain
  {:write-handlers
   {Point {"test.Point"
           (reify WriteHandler
             (write [_ w p]
               (.writeTag w "test.Point" 1)
               (.writeList w [(:x p) (:y p)])))}}
   :read-handlers
   {"test.Point"
    (reify ReadHandler
      (read [_ r _tag _cnt]
        (let [[x y] (.readObject r)]
          (->Point x y))))}})

(deftest registered-domain-roundtrips-custom-type
  (registry/register! :test/point point-domain)
  (try
    (let [path (tmp-path)
          items [(->Point 1 2) (->Point 3 4)]]
      (codec/write-envelope! path {:kind :points} items)
      (is (= items (:items (codec/read-envelope path)))))
    (finally
      (registry/deregister! :test/point))))

(deftest registry-contract
  (registry/register! :test/point point-domain)
  (try
    (testing "registered set reflects registration"
      (is (contains? (registry/registered) :test/point)))
    (finally
      (registry/deregister! :test/point)))
  (is (not (contains? (registry/registered) :test/point))))

(deftest merge-domains-later-wins
  (let [d1 {:write-handlers {String {"s1" :h1}} :read-handlers {"t" :r1}}
        d2 {:read-handlers {"t" :r2}}
        merged (registry/merge-domains [d1 d2])]
    (is (= :r2 (get-in merged [:read-handlers "t"])))
    (is (= {"s1" :h1} (get-in merged [:write-handlers String])))))
