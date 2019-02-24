(ns othello.core
  (:require [baking-soda.core :as b]
            [day8.re-frame.http-fx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [othello.ajax :as ajax]
            [othello.events]
            [secretary.core :as secretary])
  (:import goog.History))

; the navbar components are implemented via baking-soda [1]
; library that provides a ClojureScript interface for Reactstrap [2]
; Bootstrap 4 components.
; [1] https://github.com/gadfly361/baking-soda
; [2] http://reactstrap.github.io/

(defn nav-link [uri title page]
  [b/NavItem
   [b/NavLink
    {:href   uri
     :active (when (= page @(rf/subscribe [:page])) "active")}
    title]])

(defn navbar []
  (r/with-let [expanded? (r/atom true)]
    [b/Navbar {:light true
               :class-name "navbar-dark bg-primary"
               :expand "md"}
     [b/NavbarBrand {:href "/"} "othello"]
     [b/NavbarToggler {:on-click #(swap! expanded? not)}]
     [b/Collapse {:is-open @expanded? :navbar true}
      [b/Nav {:class-name "mr-auto" :navbar true}
       [nav-link "#/" "Home" :home]
       [nav-link "#/about" "About" :about]
       [nav-link "#/othello" "Othello" :othello]]]]))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src "/img/warning_clojure.png"}]]]])

(defn home-page []
  [:div.container
   (when-let [docs @(rf/subscribe [:docs])]
     [:div.row>div.col-sm-12
      [:div {:dangerouslySetInnerHTML
             {:__html (md->html docs)}}]])])

(defn view-othello-board [board]
  [:div.containter>div.row.text-center>table {:align :center}
   [:tbody {:style {:line-height "0"}}
    (map (fn [row]
           [:tr {:key row}
            (map (fn [column]
                   [:td {:key (+ column (* row 8))
                         :style {:border "0.2em solid black"
                                 :background "green"}}
                    [:span {:style {:background "green"
                                    :display :inline-block
                                    :width "2em"
                                    :height "2em"}
                            :on-click (fn [_]
                                        (when (some #(= (+ column (* row 8)) %) @(rf/subscribe [:can-put]))
                                         (rf/dispatch [:send-input (+ column (* row 8))])))}
                     (case (get board (+ column (* row 8)))
                       -1 [:span.disc {:style {:margin-top "0.25em"
                                               :background "white"
                                               :display :inline-block
                                               :border-radius "100%"
                                               :width "1.5em"
                                               :height "1.5em"}}]
                       1 [:span.disc {:style {:margin-top "0.25em"
                                              :background "black"
                                              :display :inline-block
                                              :border-radius "100%"
                                              :width "1.5em"
                                              :height "1.5em"}}]
                       [:p])]])
                 (range 8))])
         (range 8))]])

(defn othello-page []
  (when-let [message @(rf/subscribe [:message])]
    (let [pre-othello-board @(rf/subscribe [:pre-othello-board])
          othello-board @(rf/subscribe [:othello-board])
          ]
      (if (= pre-othello-board othello-board)
        (print "same board")
        (do (print "different board")
         (rf/dispatch [:update-othello-board])))
      (print "pre-othello-board")
      (print pre-othello-board)
      (print "othello-board")
      (print othello-board)
     [:div.container
      [:div.row>div.col-sm-12.text-center
       [:h3 "This is an othello page"]
       [:h4 "state : " message]
       [view-othello-board pre-othello-board]]])))

(def pages
  {:home #'home-page
   :about #'about-page
   :othello #'othello-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes

(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (rf/dispatch [:navigate :home]))

(secretary/defroute "/about" []
  (rf/dispatch [:navigate :about]))

(secretary/defroute "/othello" []
  (rf/dispatch [:navigate :othello]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:navigate :home])
  (ajax/load-interceptors!)
  (rf/dispatch [:init-othello])
  (rf/dispatch [:fetch-docs])
  (hook-browser-navigation!)
  (mount-components))
