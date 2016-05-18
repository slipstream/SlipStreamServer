(ns com.sixsq.slipstream.ssclj.db.binding
  (:refer-clojure :exclude [update]))

(defprotocol Binding
  "This protocol defines the core interface to the underlying database.
   All of the functions accept and return native clojure data structures.
   The functions must handle all necessary conversions for the database.

   On errors, the functions must throw an ex-info with an error ring
   response.  This simplifies the logic and code of the client using this
   protocol."

  (add
    [this collection-id data]
    "This function adds the given resource to the database.  The resource
     must not already exist in the database.

     On success, the function must return a 201 ring response with the
     relative URL of the new resource as the Location.

     On failure, the function must throw an ex-info containing the error
     ring response.  The error must be 409 (conflict) if the resource
     exists already.  Other appropriate error codes can also be thrown.")

  (retrieve
    [this id]
    "This function retrieves the identified resource from the database.

     On success, this returns the clojure map representation of the
     resource.  The response must not be embedded in a ring response.

     On failure, this function must throw an ex-info containing the error
     ring response. If the resource doesn't exist, use a 404 status.")

  (edit
    [this data]
    "This function updates (edits) the given resource in the database.
     The resource must already exist in the database.

     On success, the function returns the data stored in the database.
     This must NOT be embedded in a ring response.

     On failure, the function must throw an ex-info containing the error
     ring response.  The error must be 404 (not-found) if the resource
     does not exist.  Other appropriate error codes can also be thrown.")

  (delete
    [this data]
    "This function removes the given resource in the database.

     On success, the function must return a 200 ring response with a map
     containing status, message, and resource ID.

     On failure, the function must throw an ex-info containing the error
     ring response.  If the resource does not exist, then a 404 response
     should be returned.  Other appropriate error codes can also be thrown.")

  (query
    [this collection-id options]
    "This function returns a list of resources, where the collection-id
     corresponds to the name of a Collection.

     On success, the function must return a list of the given resources.
     This list may possibly be empty.  The list must not be embedded in
     a ring response.

     On failure, the function must throw an ex-info containing the error
     ring response.  If the resource-id does not correpond to a Collection,
     then a 400 (bad-request) response must be returned.  Other appropriate
     error codes can also be thrown.")
  )

