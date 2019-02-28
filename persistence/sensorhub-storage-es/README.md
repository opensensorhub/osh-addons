# Elasticsearch storage implementation

This is a storage module allowing one to store and retrieve data to/from an elasticsearch server. It uses the elasticsearch
Java API 5.2 [link](https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/index.html).

Three main OSH interfaces have been implemented: 
1. IRecordStorageModule
2. IObsStorageModule
3. IMultiSourceStorage

## Main classes

![alt text][classdiagram]

An iterator wrapper class has been used to wrap scroll response without specify the scroll id every times. The ESIterator takes care 
about making new requests with the specify scrollID when it necessary. 

A bulk processor is in charge of sending create/update/delete requests. The BulkProcessor class offers a simple interface to flush bulk operations automatically based on the number or size of requests, or after a given period. 

A transport client The TransportClient connects remotely to an Elasticsearch cluster using the transport module. It does not join the cluster, 
but simply gets one or more initial transport addresses and communicates with them in round robin fashion on each action.

Some settings are available through the ESBasicStorageConfig class:
1. scrollMaxDuration: when scrolling, the maximum duration ScrollableResults will be usable if no other results are fetched from, in ms (default is 6000)
2. scrollFetchSize: when scrolling, the number of results fetched by each Elasticsearch call (default is 2)
3. scrollBacktrackingWindowSize: when scrolling, the minimum number of previous results kept in memory at any time  (default is 10000)
4. nodeUrls: list of nodes under the format <host>:<port>
5. ignoreClusterName: set to true to ignore cluster name validation of connected nodes (default is false)
6. pingTimeout: the time to wait for a ping response from a node (default is 5)
7. nodeSamplerInterval: how often to sample / ping the nodes listed and connected (default is 5)
8. transportSniff: enable sniffing (default is false)
9. bulkConcurrentRequests: set the number of concurrent requests (default is 10)
10. bulkActions: we want to execute the bulk every n requests (default is 10000)
11. bulkSize: we want to flush the bulk every n mb (default is 10)
12. bulkFlushInterval: We want to flush the bulk every n seconds whatever the number of requests (default is 10)

A unique identifier is used as an index for the storage. Inside this index, we have three "types" of data:
1. desc: where we store the AbstractProcess object
2. info: where we store the stream information such as the name, the record description and the recommended encoding
3. data: where we store the raw data

Elasticsearch allows to use custom mapping to store the different kind of data. We use the Kryo de/serializer to de/serialize the objets before sending them to the server.
The mapping used in that case is a "binary" datatype.

## Mappings

There are the different mappings depending on the storage used:
1. basic storage mapping
```json
"mappings": {
      "info": {
        "properties": {
          "blob": {
            "type": "binary"
          }
        }
      },
      "data": {
        "properties": {
          "blob": {
            "type": "binary"
          },
          "producerID": {
            "type": "keyword"
          },
          "recordType": {
            "type": "keyword"
          },
          "timestamp": {
            "type": "double"
          }
        }
      }
    }
  }
```
2. obs storage mapping
```json
"mappings": {
      "info": {
        "properties": {
          "blob": {
            "type": "binary"
          }
        }
      },
      "data": {
        "_parent": {
          "type": "foi"
        },
        "_routing": {
          "required": true
        },
        "properties": {
          "blob": {
            "type": "binary"
          },
          "foiID": {
            "type": "keyword"
          },
          "geom": {
            "type": "geo_shape"
          },
          "producerID": {
            "type": "keyword"
          },
          "recordType": {
            "type": "keyword"
          },
          "timestamp": {
            "type": "double"
          }
        }
      },
      "foi": {
        "properties": {
          "blob": {
            "type": "binary"
          },
          "foiID": {
            "type": "keyword"
          },
          "geom": {
            "type": "geo_shape"
          },
          "producerID": {
            "type": "keyword"
          }
        }
      },
      "geobounds": {
        "properties": {
          "blob": {
            "type": "binary"
          }
        }
      }
    }
  }
```
A geo_shape datatype is used to store geometry. For now, only PolygonJTS, PointJTS and EnvelopeJTS are supported. To compute the extent, an extra envelope geometry (geobounds) is stored and 
is updated whenever a new record is stored. The envelope is recomputed and can be retrieved only once.

## Obs storage
To make a link between data and foi, the obs storage data datatype is a parent of the basic storage datatype. Thus the parent_id query can be used to find child documents which belong to the data type. 
This link allows one to make request on multiple, parent and child, types using the *QueryBuilders.hasParentQuery*.

## Mapping relationship

![alt text][mappingrelation]


## Sequence

### Time filtering

![alt text][sequencetime]

### Foi filtering

![alt text][sequencefoi]

[classdiagram]: https://github.com/opensensorhub/osh-persistence/raw/elastic-search/sensorhub-storage-es/doc/resources/ES-class.png "Main class diagram"
[mappingrelation]: https://github.com/opensensorhub/osh-persistence/raw/elastic-search/sensorhub-storage-es/doc/resources/mapping-relation.png "Mapping relationship"
[sequencetime]: https://github.com/opensensorhub/osh-persistence/raw/elastic-search/sensorhub-storage-es/doc/resources/ES-sequence-time-filter.png "Sequence time filtering diagram"
[sequencefoi]: https://github.com/opensensorhub/osh-persistence/raw/elastic-search/sensorhub-storage-es/doc/resources/ES-sequence-foi-filter.png "Sequence FOI filtering diagram"
