(ns user
  (:require [othello.config :refer [env]]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [othello.figwheel :refer [start-fw stop-fw cljs]]
            [othello.core :refer [start-app]]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start []
  (mount/start-without #'othello.core/repl-server))

(defn stop []
  (mount/stop-except #'othello.core/repl-server))

(defn restart []
  (stop)
  (start))


