(ns com.sixsq.slipstream.ssclj.resources.run.state-machine
  "This namespace uses ZooKeeper to create a distributed system to host the state of a run. Each VM is represented
  by an index node under its corresponding node. As each VM reports its current state completed, the corresponding
  index znode (i.e. ZooKeeper node) is removed from the structure.  When no node remains, it means all VMs have
  reported, which means the state machine can move to its next state.

  This design includes the following important goals:
  1. state completion should be idem potent.  This is important such that if the client has a doubt about the
     completness of its call to report completion, it can be performed again, without risk in changing the the state
     machine (e.g. reporting as completed the next state in the run)
  2. no pulling is required to get the next state transition, since the system includes a notification mechanism.
  3. the system is fast, with no loops nor expensive state investigation. It is even constant wrt the size of the run
  4. works in a share nothing pattern, meaning that this namespace can be distributed over the network. For this, the
     new buddy-circle namespace is used to ensure automatic recovery of partial transition, is case of failure.  This
     means this namespace can be packaged as a micro-service, and deployed/removed as required, with no special
     coordination required.

  The topology of the run is persisted as an edn structure inside the ./topology znode.  This can be changed as nodes
  come and go, for scalable deployments."
  (:require [zookeeper :as zk]))

;; State machine states
(def initial-state "init")
(def valid-transitions
  {initial-state ["provisioning"]
   "provisioning" ["executing"]
   "executing" ["sending report"]
   "sending report" ["ready"]
   "ready" ["provisioning" ]
   "finalyzing" ["terminated"]
   "terminated" []})
