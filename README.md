# hive-fressian

Generic [Fressian](https://github.com/clojure/data.fressian) envelope codec
with an OCP handler-domain registry, packaged as a library-kind
[hive-addon](https://github.com/hive-agi/hive-addon) (`IAddon` 0.2.0).

## What it does

- `hive-fressian.registry` — register a **domain** (a named bundle of
  write/read handlers for one type family) once; every codec call sees the
  merged handlers. New type family = one `register!`, codec untouched.
- `hive-fressian.codec` — durable envelope files: `header` map + item
  stream. `write-envelope!` / `read-envelope`.
- `hive-fressian.addon` — `IAddon` record advertising the
  `:serialization/fressian` capability (no tools; consumers call the
  namespaces directly).

## Usage

```clojure
(require '[hive-fressian.registry :as registry]
         '[hive-fressian.codec :as codec])

(registry/register! :my/domain
  {:write-handlers {MyType {"my.Type" (reify WriteHandler ...)}}
   :read-handlers  {"my.Type" (reify ReadHandler ...)}})

(codec/write-envelope! "out.fressian" {:kind :my-snapshot} items)
(codec/read-envelope "out.fressian")
;; => {:header {..} :items [..]}
```

## Tests

```bash
clj -M:test -e "(require 'hive-fressian.codec-test)(let [r (clojure.test/run-tests 'hive-fressian.codec-test)](shutdown-agents)(System/exit (+ (:fail r)(:error r))))"
```
