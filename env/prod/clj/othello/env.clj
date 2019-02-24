(ns othello.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[othello started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[othello has shut down successfully]=-"))
   :middleware identity})
