(ns com.sixsq.slipstream.ssclj.usage.state-machine)

;;
;; simplistic finish state machine
;; as the state is provided each time, it's not really a state machine
;; but this helps to understand the different cases for the usage-records
;;
;; 
(def ^:private state-transitions
  { :initial  { :start  :insert-start            
                :stop   :severe-wrong-transition}

    :started  { :start  :severe-wrong-transition 
                :stop   :close-record}

    :stopped  { :start  :reset-end 
                :stop   :severe-wrong-transition}})

(defn action
  [state trigger]
  (get-in state-transitions [state trigger]))
