(ns othello.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [othello.routes.services.othello :as othello]))

(def service-routes
  (api
    {:swagger {:ui "/swagger-ui"
               :spec "/swagger.json"
               :data {:info {:version "1.0.0"
                             :title "Sample API"
                             :description "Sample Services"}}}}
    (context "/othello-api" []
      :tags ["othello"]
      (POST "/put" []
        :body-params [input :- Long, board :- [Long]]
        :summary "input means position putting your stone\nboard meand the current board"
        (ok (if (= input -1)
              (let [init-board (othello/gen-board othello/BOARD-SIZE)]
                  {:message "init"
                   :result init-board
                   :pre-result init-board
                   :can-put (map
                             #(:pos %)
                             (othello/search-all-strategy
                              othello/BOARD-SIZE othello/player1 init-board))})
              (othello/put-stone input board)))))
    (context "/api" []
      :tags ["thingie"]
      
      (GET "/plus" []
        :return       Long
        :query-params [x :- Long, {y :- Long 1}]
        :summary      "x+y with query-parameters. y defaults to 1."
        (ok (+ x y)))

      (POST "/minus" []
        :return      Long
        :body-params [x :- Long, y :- Long]
        :summary     "x-y with body-parameters."
        (ok (- x y)))

      (GET "/times/:x/:y" []
        :return      Long
        :path-params [x :- Long, y :- Long]
        :summary     "x*y with path-parameters"
        (ok (* x y)))

      (POST "/divide" []
        :return      Double
        :form-params [x :- Long, y :- Long]
        :summary     "x/y with form-parameters"
        (ok (/ x y)))

      (GET "/power" []
        :return      Long
        :header-params [x :- Long, y :- Long]
        :summary     "x^y with header-parameters"
        (ok (long (Math/pow x y)))))))
