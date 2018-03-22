(ns com.sixsq.slipstream.db.binding)

(defprotocol Binding
  "This protocol defines the core interface to the underlying database.
   All of the functions accept and return native clojure data structures.
   The functions must handle all necessary conversions for the database.

   For those functions that have a data argument, the id of the document
   is taken from the value of the :id attribute in the data.

   On errors, the functions must throw an ex-info with an error ring
   response.  This simplifies the logic and code of the client using this
   protocol."

  (initialize
    [this collection-id options]
    "This function initialize the given resource into the database.")

  (add
    [this data options]
    [this collection-id data options]
    "This function adds the given resource to the database.  The resource
     must not already exist in the database.

     The older 4-argument function that includes the collection-id is
     deprecated.  The collection id is included in the document id, so
     the separate argument isn't needed.

     On success, the function must return a 201 ring response with the
     relative URL of the new resource as the Location.

     On failure, the function must throw an ex-info containing the error
     ring response.  The error must be 409 (conflict) if the resource
     exists already.  Other appropriate error codes can also be thrown.")

  (retrieve
    [this id options]
    "This function retrieves the identified resource from the database.

     On success, this returns the clojure map representation of the
     resource.  The response must not be embedded in a ring response.

     On failure, this function must throw an ex-info containing the error
     ring response. If the resource doesn't exist, use a 404 status.")

  (edit
    [this data options]
    "This function updates (edits) the given resource in the database.
     The resource must already exist in the database.

     On success, the function returns the data stored in the database.
     This must NOT be embedded in a ring response.

     On failure, the function must throw an ex-info containing the error
     ring response.  The error must be 404 (not-found) if the resource
     does not exist.  Other appropriate error codes can also be thrown.")

  (delete
    [this data options]
    "This function removes the given resource in the database. Note that
     you can remove a document by id by providing data of the form:
     {:id \"collection/uuid\"}.

     On success, the function must return a 200 ring response with a map
     containing status, message, and resource ID.

     On failure, the function must throw an ex-info containing the error
     ring response.  If the resource does not exist, then a 404 response
     should be returned.  Other appropriate error codes can also be thrown.")

  (query
    [this collection-id options]
    "This function returns metadata and resources, where the collection-id
     corresponds to the name of a Collection.

     On success, the function must return a two-element tuple. The first
     element is metadata concerning the query (usually with the count and
     aggregations). The second element is a list of the returned resources.
     This list may possibly be empty. The list must not be embedded in a ring
     response.

     On failure, the function must throw an ex-info containing the error
     ring response.  If the resource-id does not correspond to a Collection,
     then a 400 (bad-request) response must be returned.  Other appropriate
     error codes can also be thrown."))

