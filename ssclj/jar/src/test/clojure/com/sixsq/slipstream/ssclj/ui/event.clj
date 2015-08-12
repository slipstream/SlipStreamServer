(ns com.sixsq.slipstream.ssclj.ui.event
  (:require
    [clojure.test               :refer :all]
    [slipstream.ui.views.representation :as r]))

(def events
  "{
  \"events\" : [ {
    \"content\" : {
      \"state\" : \"Done\",
      \"resource\" : {
        \"href\" : \"run/7fa1e339-f863-4ecb-b8ac-ffe6b0e343e3\"
      }
    },
    \"updated\" : \"2015-06-11T10:16:07.470Z\",
    \"type\" : \"state\",
    \"created\" : \"2015-06-11T10:16:07.470Z\",
    \"id\" : \"Event/083e13c6-2dcc-4711-b72e-2880d12b989d\",
    \"severity\" : \"medium\",
    \"acl\" : {
      \"owner\" : {
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      },
      \"rules\" : [ {
        \"right\" : \"ALL\",
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      }, {
        \"right\" : \"ALL\",
        \"type\" : \"ROLE\",
        \"principal\" : \"ADMIN\"
      } ]
    },
    \"operations\" : [ {
      \"rel\" : \"http://sixsq.com/slipstream/1/Action/delete\",
      \"href\" : \"Event/083e13c6-2dcc-4711-b72e-2880d12b989d\"
    } ],
    \"resourceURI\" : \"http://sixsq.com/slipstream/1/Event\",
    \"timestamp\" : \"2015-06-11T10:16:07.457Z\"
  }, {
    \"content\" : {
      \"state\" : \"Finalizing\",
      \"resource\" : {
        \"href\" : \"run/7fa1e339-f863-4ecb-b8ac-ffe6b0e343e3\"
      }
    },
    \"updated\" : \"2015-06-11T10:15:44.287Z\",
    \"type\" : \"state\",
    \"created\" : \"2015-06-11T10:15:44.287Z\",
    \"id\" : \"Event/381c0d0e-f863-4fbc-a52c-bda755e0d7fa\",
    \"severity\" : \"medium\",
    \"acl\" : {
      \"owner\" : {
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      },
      \"rules\" : [ {
        \"right\" : \"ALL\",
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      }, {
        \"right\" : \"ALL\",
        \"type\" : \"ROLE\",
        \"principal\" : \"ADMIN\"
      } ]
    },
    \"operations\" : [ {
      \"rel\" : \"http://sixsq.com/slipstream/1/Action/delete\",
      \"href\" : \"Event/381c0d0e-f863-4fbc-a52c-bda755e0d7fa\"
    } ],
    \"resourceURI\" : \"http://sixsq.com/slipstream/1/Event\",
    \"timestamp\" : \"2015-06-11T10:15:44.274Z\"
  }, {
    \"content\" : {
      \"state\" : \"Executing\",
      \"resource\" : {
        \"href\" : \"run/7fa1e339-f863-4ecb-b8ac-ffe6b0e343e3\"
      }
    },
    \"updated\" : \"2015-06-11T10:10:58.323Z\",
    \"type\" : \"state\",
    \"created\" : \"2015-06-11T10:10:58.323Z\",
    \"id\" : \"Event/40727af8-63cb-479e-a52c-921b049362a0\",
    \"severity\" : \"medium\",
    \"acl\" : {
      \"owner\" : {
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      },
      \"rules\" : [ {
        \"right\" : \"ALL\",
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      }, {
        \"right\" : \"ALL\",
        \"type\" : \"ROLE\",
        \"principal\" : \"ADMIN\"
      } ]
    },
    \"operations\" : [ {
      \"rel\" : \"http://sixsq.com/slipstream/1/Action/delete\",
      \"href\" : \"Event/40727af8-63cb-479e-a52c-921b049362a0\"
    } ],
    \"resourceURI\" : \"http://sixsq.com/slipstream/1/Event\",
    \"timestamp\" : \"2015-06-11T10:10:58.311Z\"
  }, {
    \"content\" : {
      \"state\" : \"Terminated\",
      \"resource\" : {
        \"href\" : \"run/7fa1e339-f863-4ecb-b8ac-ffe6b0e343e3\"
      }
    },
    \"updated\" : \"2015-06-11T10:15:44.184Z\",
    \"type\" : \"state\",
    \"created\" : \"2015-06-11T10:15:44.184Z\",
    \"id\" : \"Event/66ae06e5-1084-4e31-b01c-d29f2b9c1ca5\",
    \"severity\" : \"medium\",
    \"acl\" : {
      \"owner\" : {
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      },
      \"rules\" : [ {
        \"right\" : \"ALL\",
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      }, {
        \"right\" : \"ALL\",
        \"type\" : \"ROLE\",
        \"principal\" : \"ADMIN\"
      } ]
    },
    \"operations\" : [ {
      \"rel\" : \"http://sixsq.com/slipstream/1/Action/delete\",
      \"href\" : \"Event/66ae06e5-1084-4e31-b01c-d29f2b9c1ca5\"
    } ],
    \"resourceURI\" : \"http://sixsq.com/slipstream/1/Event\",
    \"timestamp\" : \"2015-06-11T10:15:44.174Z\"
  }, {
    \"content\" : {
      \"state\" : \"SendingReports\",
      \"resource\" : {
        \"href\" : \"run/7fa1e339-f863-4ecb-b8ac-ffe6b0e343e3\"
      }
    },
    \"updated\" : \"2015-06-11T10:12:49.690Z\",
    \"type\" : \"state\",
    \"created\" : \"2015-06-11T10:12:49.690Z\",
    \"id\" : \"Event/92890f1d-cbc8-4962-a96d-d0be1da0ba72\",
    \"severity\" : \"medium\",
    \"acl\" : {
      \"owner\" : {
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      },
      \"rules\" : [ {
        \"right\" : \"ALL\",
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      }, {
        \"right\" : \"ALL\",
        \"type\" : \"ROLE\",
        \"principal\" : \"ADMIN\"
      } ]
    },
    \"operations\" : [ {
      \"rel\" : \"http://sixsq.com/slipstream/1/Action/delete\",
      \"href\" : \"Event/92890f1d-cbc8-4962-a96d-d0be1da0ba72\"
    } ],
    \"resourceURI\" : \"http://sixsq.com/slipstream/1/Event\",
    \"timestamp\" : \"2015-06-11T10:12:49.677Z\"
  }, {
    \"content\" : {
      \"state\" : \"Ready\",
      \"resource\" : {
        \"href\" : \"run/7fa1e339-f863-4ecb-b8ac-ffe6b0e343e3\"
      }
    },
    \"updated\" : \"2015-06-11T10:12:49.925Z\",
    \"type\" : \"state\",
    \"created\" : \"2015-06-11T10:12:49.925Z\",
    \"id\" : \"Event/a49fef34-6cb9-4f07-b25d-a55e99b79f07\",
    \"severity\" : \"medium\",
    \"acl\" : {
      \"owner\" : {
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      },
      \"rules\" : [ {
        \"right\" : \"ALL\",
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      }, {
        \"right\" : \"ALL\",
        \"type\" : \"ROLE\",
        \"principal\" : \"ADMIN\"
      } ]
    },
    \"operations\" : [ {
      \"rel\" : \"http://sixsq.com/slipstream/1/Action/delete\",
      \"href\" : \"Event/a49fef34-6cb9-4f07-b25d-a55e99b79f07\"
    } ],
    \"resourceURI\" : \"http://sixsq.com/slipstream/1/Event\",
    \"timestamp\" : \"2015-06-11T10:12:49.918Z\"
  }, {
    \"content\" : {
      \"state\" : \"Provisioning\",
      \"resource\" : {
        \"href\" : \"run/7fa1e339-f863-4ecb-b8ac-ffe6b0e343e3\"
      }
    },
    \"updated\" : \"2015-06-11T10:10:42.928Z\",
    \"type\" : \"state\",
    \"created\" : \"2015-06-11T10:10:42.928Z\",
    \"id\" : \"Event/e1586eb9-9186-46ad-80bb-fe0286d3fda3\",
    \"severity\" : \"medium\",
    \"acl\" : {
      \"owner\" : {
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      },
      \"rules\" : [ {
        \"right\" : \"ALL\",
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      }, {
        \"right\" : \"ALL\",
        \"type\" : \"ROLE\",
        \"principal\" : \"ADMIN\"
      } ]
    },
    \"operations\" : [ {
      \"rel\" : \"http://sixsq.com/slipstream/1/Action/delete\",
      \"href\" : \"Event/e1586eb9-9186-46ad-80bb-fe0286d3fda3\"
    } ],
    \"resourceURI\" : \"http://sixsq.com/slipstream/1/Event\",
    \"timestamp\" : \"2015-06-11T10:10:42.920Z\"
  }, {
    \"content\" : {
      \"state\" : \"Initializing\",
      \"resource\" : {
        \"href\" : \"run/7fa1e339-f863-4ecb-b8ac-ffe6b0e343e3\"
      }
    },
    \"updated\" : \"2015-06-11T10:10:14.349Z\",
    \"type\" : \"state\",
    \"created\" : \"2015-06-11T10:10:14.349Z\",
    \"id\" : \"Event/f63e3d8a-9f34-4669-888d-e73d07c237f4\",
    \"severity\" : \"medium\",
    \"acl\" : {
      \"owner\" : {
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      },
      \"rules\" : [ {
        \"right\" : \"ALL\",
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      }, {
        \"right\" : \"ALL\",
        \"type\" : \"ROLE\",
        \"principal\" : \"ADMIN\"
      } ]
    },
    \"operations\" : [ {
      \"rel\" : \"http://sixsq.com/slipstream/1/Action/delete\",
      \"href\" : \"Event/f63e3d8a-9f34-4669-888d-e73d07c237f4\"
    } ],
    \"resourceURI\" : \"http://sixsq.com/slipstream/1/Event\",
    \"timestamp\" : \"2015-06-11T10:10:14.339Z\"
  }, {
    \"content\" : {
      \"state\" : \"Something critical\",
      \"resource\" : {
        \"href\" : \"run/7fa1e339-f863-4ecb-b8ac-ffe6b0e343e3\"
      }
    },
    \"updated\" : \"2015-06-11T01:10:14.349Z\",
    \"type\" : \"example\",
    \"created\" : \"2015-06-11T01:10:14.349Z\",
    \"id\" : \"Event/45859383-183e-4bcb-9356-8f2a6d0382ab\",
    \"severity\" : \"critical\",
    \"acl\" : {
      \"owner\" : {
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      },
      \"rules\" : [ {
        \"right\" : \"ALL\",
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      }, {
        \"right\" : \"ALL\",
        \"type\" : \"ROLE\",
        \"principal\" : \"ADMIN\"
      } ]
    },
    \"operations\" : [ {
      \"rel\" : \"http://sixsq.com/slipstream/1/Action/delete\",
      \"href\" : \"Event/bedd9fec-25e9-4ee6-bea8-d8ab383bda37\"
    } ],
    \"resourceURI\" : \"http://sixsq.com/slipstream/1/Event\",
    \"timestamp\" : \"2015-06-11T01:10:14.339Z\"
  }, {
    \"content\" : {
      \"state\" : \"Something high\",
      \"resource\" : {
        \"href\" : \"run/7fa1e339-f863-4ecb-b8ac-ffe6b0e343e3\"
      }
    },
    \"updated\" : \"2015-06-11T00:10:14.349Z\",
    \"type\" : \"example\",
    \"created\" : \"2015-06-11T00:10:14.349Z\",
    \"id\" : \"Event/670d2def-7c49-4281-a03e-289fb0b1f9f4\",
    \"severity\" : \"high\",
    \"acl\" : {
      \"owner\" : {
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      },
      \"rules\" : [ {
        \"right\" : \"ALL\",
        \"type\" : \"USER\",
        \"principal\" : \"bob\"
      }, {
        \"right\" : \"ALL\",
        \"type\" : \"ROLE\",
        \"principal\" : \"ADMIN\"
      } ]
    },
    \"operations\" : [ {
      \"rel\" : \"http://sixsq.com/slipstream/1/Action/delete\",
      \"href\" : \"Event/bedd9fec-25e9-4ee6-bea8-d8ab383bda37\"
    } ],
    \"resourceURI\" : \"http://sixsq.com/slipstream/1/Event\",
    \"timestamp\" : \"2015-06-11T00:10:14.339Z\"
  } ],
  \"operations\" : [ {
    \"rel\" : \"http://sixsq.com/slipstream/1/Action/add\",
    \"href\" : \"Event\"
  } ],
  \"acl\" : {
    \"rules\" : [ {
      \"type\" : \"ROLE\",
      \"right\" : \"ALL\",
      \"principal\" : \"ANON\"
    } ],
    \"owner\" : {
      \"type\" : \"ROLE\",
      \"principal\" : \"ADMIN\"
    }
  },
  \"resourceURI\" : \"http://sixsq.com/slipstream/1/EventCollection\",
  \"id\" : \"Event\",
  \"count\" : 8
}")

(deftest event-to-html
  (is (= "abc" (r/-toHtml events "events" nil))))


