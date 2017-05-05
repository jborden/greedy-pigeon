(ns greedy-pigeon.xhr
  (:require-macros [reagent.interop :refer [$ $!]])
  (:require [goog.net.XhrIo]))

(def XhrIo goog.net.XhrIo)

(defn send-xhr
  "Send a xhr to url using callback and HTTP method."
  [url callback method & [data headers timeout]]
  ($ XhrIo send url callback method data headers timeout))

(defn xhrio-wrapper
  "A callback for processing the xhrio response event. If
  response.target.isSuccess() is true, call f on the json response"
  [f response]
  (let [target ($ response :target)]
    (if ($ target isSuccess)
      (f ($ target getResponseJson))
      ($ js/console log (str "xhrio-wrapper error:" ($ target :lastError_))))))

(defn retrieve-url
  "Retrieve and process json response with f from url using HTTP method and json
  data. Optionally, define a timeout in ms."
  [url method data f & [timeout]]
  (let [header (clj->js {"Content-Type" "application/json"})]
    (send-xhr url f method data header timeout)))

(defn process-json
  "Take a response, convert it a clj map and call f on the resulting map."
  [f response]
  (f (js->clj response :keywordize-keys true)))

(defn process-json-response
  "Assuming the server will respond with JSON, convert the response to JSON
  and call f on it."
  [f]
  (partial xhrio-wrapper (partial process-json f)))
