**API Routes description**
----
  _Note that this API doesn't allow any modification, it's just used to read values and compute prices_

* **URL**

  /price/entity

* **Method:**
  
  `POST`
  
*  **URL Params**

   _N/A_ 

* **Data Params**

   _JSON formatted queries_

   **Required:**
 
   `cloudname=[string]`

   `resourcetype=[string] > see https://olivierbelli.github.io/#resourceType`

   `resourcename=[string]`

   `quantity=[array of quantity objects*]`

        *quantity object={
           timeCode=[string] > see https://olivierbelli.github.io/#timeCode
           sample=[positive integer]
           values=[array of positive integers]
        }




* **Success Response:**
  * **Code:** 200 <br />
    **Content:** `float value`

* **Error Response:**

  _An error in the call will almost always result with one of the following message. Note that this is temporary._

  * **Code:** 500 INTERNAL SERVER ERROR <br />
    **Content:** `Problem with the parameters`



  * **Code:** 400 BAD REQUEST<br />
    **Content:** `Malformed JSON in request body`




* **Sample Calls:**

* * _Request the price of an Exoscale Micro instance for 1 hour_

            curl -H "Content-Type: application/json" -X POST -d '{"cloudname" : "exoscale-ch-gva", "resourcetype" : "vm", "resourcename" : "Micro", "quantity" : [{"timeCode" : "HUR", "sample" : 1, "values" : [1]}]}' http://gw-hepiacloud.hesge.ch:10138/price/entity -D -

* * _Request the price of an Exoscale Micro instance for 1/2 hour using a 10minutes sample_

            curl -H "Content-Type: application/json" -X POST -d '{"cloudname" : "exoscale-ch-gva", "resourcetype" : "vm", "resourcename" : "Micro", "quantity" : [{"timeCode" : "MIN", "sample" : 10, "values" : [1, 1, 1]}]}' http://gw-hepiacloud.hesge.ch:10138/price/entity -D -


* * _Request the price of 5 Exoscale Micro instances for 1 hour and then 2 instances for the following 30minutes using a 30 minutes sample_

            curl -H "Content-Type: application/json" -X POST -d '{"cloudname" : "exoscale-ch-gva", "resourcetype" : "vm", "resourcename" : "Micro", "quantity" : [{"timeCode" : "MIN", "sample" : 30, "values" : [5, 5, 2]}]}' http://gw-hepiacloud.hesge.ch:10138/price/entity -D -


* * _Request the price of 3 weeks of outgoing network utilization with Exoscale. The traffic for the 3 weeks is divided as follow : 30GB for the first week, 100GB for the second week and 35GB for the third week_

            curl -H "Content-Type: application/json" -X POST -d '{"cloudname" : "exoscale-ch-gva", "resourcetype" : "network_out", "resourcename" : "Exoscale outgoing network", "quantity" : [{"timeCode" : "WEE", "sample" : 1, "values" : [30,100,35]}]}' http://gw-hepiacloud.hesge.ch:10138/price/entity -D -

* * _Request the price of a month of object storage utilization. The usage is composed of 400GB of storage, 10000 type A requests and 100000 type B requests. :warning: Note that using this command won't work since AWS prices are not currently in the database, its purpose is just to illustrate associated prices_

            curl -H "Content-Type: application/json" -X POST -d '{"cloudname" : "aws-us-west-1", "resourcetype" : "objectstorage", "resourcename" : "AWS object storage", "quantity" : [{"timeCode" : "MON", "sample" : 1, "values" : [400]},{"timeCode" : "MON", "sample" : 1, "values" : [10000]},{"timeCode" : "MON", "sample" : 1, "values" : [100000]}]}' http://gw-hepiacloud.hesge.ch:10138/price/entity -D -


* **Notes:**
  
 It is important to note that, due to the way that providers are charging their products, requesting prices on small time periods such as minutes for VM could give the same price as a full hour request. For example, using an instance on Amazon for 1 minute will cost the same price as running it for an hour since their smallest unit of billing time is hourly, which is not the case with exoscale that is minute based.

 Two routes are still missing for the clojure version. The first one is the route to compute the price of a run given its `run-uid`. 

 The second one is to provide a matching given the desired parameters (i.e. I want a 2 GB RAM, 4vcpu instance, what are the possibilities for the different cloud providers and their prices ?)

 The server in the examples (http://gw-hepiacloud.hesge.ch:10138) is just for demo purposes. It is not guaranteed to be up and running and its values are not updated frequently. 


 * **API upcoming modifications ideas:**

 The main problem is the way to pass associatedCosts quantities. It has to be in the right order for now. This behaviour should be changed. The idea is to replace the array by a dictionary to nest the associatedCosts :

            curl -H "Content-Type: application/json" -X POST -d '{"cloudname" : "aws-us-west-1", "resourcetype" : "objectstorage", "resourcename" : "AWS object storage", "quantity" : {"timeCode" : "MON", "sample" : 1, "values" : [400], "associatedCosts" : [{"name" : "typeAreq", "timeCode" : "MON", "sample" : 1, "values" : [10000]},{"name" : "typeBreq", "timeCode" : "MON", "sample" : 1, "values" : [100000]}]}' http://gw-hepiacloud.hesge.ch:10138/price/entity -D -


The resource list api should be created : it would allow to list all resources for a cloud, list type of resources and get a single resource by name/cloudname.