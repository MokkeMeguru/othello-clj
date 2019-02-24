(ns othello.events
  (:use-macros [cljs.core.async.macros :only [go]])
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [cljs.core.async :refer [<! timeout]]))

;;dispatchers

(rf/reg-event-db
  :navigate
  (fn [db [_ page]]
    (assoc db :page page)))

(rf/reg-event-db
  :set-docs
  (fn [db [_ docs]]
    (assoc db :docs docs)))

(rf/reg-event-db
 :set-othello-board
 (fn [db [_ response]]
   (merge db response)))

(rf/reg-event-db
 :update-othello-board
 (fn [db [_ _]]
   (assoc db :pre-result @(rf/subscribe [:othello-board]))))


(rf/reg-event-fx
  :fetch-docs
  (fn [_ _]
    {:http-xhrio {:method          :get
                  :uri             "/docs"
                  :response-format (ajax/raw-response-format)
                  :on-success       [:set-docs]}}))

(rf/reg-event-fx
 :init-othello
 (fn [_ _]
   {:http-xhrio
    {:method :post
     :uri "/othello-api/put"
     :params {:input -1 :board [0]}
     :format (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success [:set-othello-board]}}))

(rf/reg-event-fx
 :send-input
 (fn [_ [_ input]]
   {:http-xhrio
    {:method :post
     :uri "/othello-api/put"
     :params {:input input
              :board @(rf/subscribe [:othello-board])}
     :format (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success [:set-othello-board]
     :on-failure nil}}))

(rf/reg-event-db
  :common/set-error
  (fn [db [_ error]]
    (assoc db :common/error error)))

;;subscriptions

(rf/reg-sub
  :page
  (fn [db _]
    (:page db)))

(rf/reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(rf/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))

(rf/reg-sub
 :message
 (fn [db _]
   (:message db)))


(rf/reg-sub
 :othello-board
 (fn [db _]
   (:result db)))

(rf/reg-sub
 :pre-othello-board
 (fn [db _]
   (:pre-result db)))

(rf/reg-sub
 :can-put
 (fn [db _]
   (:can-put db)))
