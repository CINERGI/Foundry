Document Ingestion Management
=============================

An application with a REST interface for document metadata and document ingestion can be used to manage document ingestion in a systematic way. 
As used by Elasticsearch and Solr a  REST web service based interface would allow flexible data management at ingestion level. 
Documents in XML, CSV or JSON formats  can be ingested through the REST API. 
The Document Ingestion Management System (DIMS) is responsible to transform the original document to JSON (if not already in JSON) , validate it, 
wrap it with standard document components such as processing, provenance etc 
and persist to MongoDB. 
For each source, first source meta-data needs to be ingested via the REST interface. 

## Source Metadata

The object model of the Source metadata is defined in `common` subproject 
`org.neuinfo.foundry.common.model.Source` class. The 
class also allows conversion to or from JSON format and has a Builder 
to build `Source` objects. 
Below is the JSON representation of the source metadata in MongoDB [Burak's original JSON object]

```JSON
{
    nifId:""
    name:"<source-name>"
    description:"",
    schema:"<JSON-Schema-for-documents-for-this source>",
    primaryKey:"<primary key pointer to the element(s) in the document in JSON Path format>",
    provenance:"<provenance record in PROV-DM format for the source metadata ingestion>",
    batchInfos:"<A list of internal records to manage multiple ingestion batches>"
}
```

A draft schema and example JSON object for a richer source representation is in the json-schemas/HarvestSourceObjectSchema.js file in this repository. This JSON object design used namespace qualifiers to allow JSON elements to be qualified (globally unique) to enable the objects to be encoded using JSON-LD for interoperability.  Element names have been expanded to make them less ambiguous when taken out of context. 

### Primary Key Representation

While each MongoDB ingested document is provided with a unique synthetic id, to identify the document a more natural primary key is also needed. 
The value of a field inside the JSON document can be used as the natural primary key. A pointer to that field is represented in JSONPath (similar to XPath) 
and stored in the source metadata JSON document in MongoDB (See above). 
A JSONPath processor is implemented by `org.neuinfo.foundry.common.util.JSONPathProcessor` class in `common` subproject.

An alternate approach is to finger print the incoming document using a hash based on the text in the document. This hash would uniquely fingerprint the harvested bitstream.

## Document Wrapper JSON

The object model of the document wrapper JSON representation to wrap the original document and manage processing of the document until Elasticsearch indexing is defined in `common` subproject 
`org.neuinfo.foundry.common.model.DocWrapper` class. 

Below is the JSON representation of the document wrapper that is stored in MongoDB.

```JSON
{
   "primaryKey":"<>",
   "Version":"<version-number>",
   "CrawlDate":"",
   "indexDate":"",
   "SourceInfo": { 
      "SourceID":"<nif-id of the source>",
      "ViewID":"",
      "Name":"<source-name>",
   },   
   "OriginalDoc":"<original doc as JSON>",
   "Processing": {
     "Status":"<process-status used to guide message oriented document processors>",
   },
   "History": {
    "provenance":"<provenance records in PROV-DM JSON format for the document ingestion and processing>",
    "batchId":"<>",
   },
   "Data":{}
}
```

This basic JSON object has been elaborated in json-schemas/MetadataObjectJSONSchema.js in the Foundry GitHub repository. Element names are expanded to make them unambiguous (mostly) outside the context of the JSON object, allowing subsets of the elements to be assembled into flat (not nested) simple JSON views of a subset of the content. Element names are also namespace qualified with a URI to make them globally unique when used in a qualified format. This enables the JSON objects to be used in JSON-LD applications.

## Validation

A generic way of validating JSON documents for variety of sources is usage of JSON Schema (http://json-schema.org/). JSON schema is analogous to XML schema and there are various JSON schema validators and schema generators available. 

For feasilibility testing, I have generated an initial JSON schema from Open Source Brain Projects XML file using a web based JSON schema generator tool (http://www.jsonschema.net/) and edited it to specify required fields, id type checks via regular expressions and date/time format checking.
Then using a java based JSON schema validator, I tested Open Source Brain Project JSON documents against the schema by introducing errors etc.

In the source metadata document for each source , we can store the JSON schema that the documents from that source need to adhere. Then during the document ingestion phase the document will be validated by this schema and if it is valid then it will be ingested.

The `ingestor` subproject implements JSON schema based validation using 
third party library `https://github.com/fge/json-schema-validator`.
The `org.neuinfo.foundry.ingestor.service DocumentIngestionService` class allows ingestion of a JSON document. During the ingestion, if the source of 
document has a JSON schema associated with it and if the user wants to validate 
against the schema , the document is validated against the schema and will not be ingested if it fails validation. 

Below is the JSON schema for Open Source Brain Project

```JSON
{
   "type":"object",
   "$schema": "http://json-schema.org/draft-03/schema",
   "required":true,
   "properties":{
      "project": {
         "type":"object",
         "required":false,
         "properties":{
            "created_on": {
               "type":"object",
               "required":false,
               "properties":{
                  "_$": {
                     "type":"string",
                     "format":"date-time",
                     "required":true
                  }
               }
            },
            "custom_fields": {
               "type":"object",
               "required":false,
               "properties":{
                  "@type": {
                     "type":"string",
                     "required":false
                  },
                  "custom_field": {
                     "type":"array",
                     "required":false,
                     "items": {
                        "type":"object",
                        "required":false,
                        "properties":{
                           "@id": {
                              "type":"string",
                              "pattern":"^\\d+$",
                              "required":true
                           },
                           "@name": {
                              "type":"string",
                              "required":true
                           },
                           "value": {
                              "type":"object",
                              "required":false,
                              "properties":{
                                 "_$": {
                                    "type":"string",
                                    "required":false
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            },
            "description": {
               "type":"object",
               "required":false,
               "properties":{
                  "_$": {
                     "type":"string",
                     "required":false
                  }
               }
            },
            "id": {
               "type":"object",
               "required":false,
               "properties":{
                  "_$": {
                     "type":"string",
                     "pattern":"^\\d+$",
                     "required":true
                  }
               }
            },
            "identifier": {
               "type":"object",
             "required":false,
             "properties":{
                "_$": {
                   "type":"string",
                   "required":true
                }
             }
            },
            "name": {
               "type":"object",
               "required":false,
               "properties":{
                  "_$": {
                     "type":"string",
                     "required":false
                  }
               }
            },
            "status": {
               "type":"object",
               "required":false,
               "properties":{
                  "_$": {
                     "type":"string",
                     "required":false
                  }
               }
            },
            "updated_on": {
               "type":"object",
               "required":false,
               "properties":{
                  "_$": {
                     "type":"string",
                     "format":"date-time",
                     "required":true
                  }
               }
            }
         }
      }
   }
}
```

## Provenance

PROV-DM specification (http://www.w3.org/TR/2013/REC-prov-dm-20130430/) based ProvToolBox (https://github.com/lucmoreau/ProvToolbox) libraries are used in foundry-common module to build a representation independent Java model of provenance and save is in PROV-JSON format (http://www.w3.org/Submission/2013/SUBM-prov-json-20130424/).

In `foundry-common module` there is a builder for provenance record hiding the details of JSON-DM. Below is an example, using Java fluent API style, to state that a document `doc1` entity is processed by  software agent `docIdAssigner` that assigns a document id to the entity. The activity took place at May 16 2014, 16:05 lasting 1 second.

```java
final ProvenanceRec.Builder builder = new ProvenanceRec.Builder("http://example.org", "foundry");

ProvenanceRec provenanceRec = builder.docEntity("doc1", "document")
                .softwareAgent("docIdAssigner")
                .activity("assignId", "doc-id-assigment",  "2014-05-16T16:05:00",
                        "2014-05-16T16:05:01")
                .wasAssociatedWith("assignId","docIdAssigner")
                .used("assignId","doc1")
                .build();

provenanceRec.save("/tmp/doc_id_assigment_prov.json");
```

Below is the PROV-JSON document generated and also validated against PROV-JSON JSON schema ()

```JSON
{
  "wasAssociatedWith": {
    "_:wAW2": {
      "prov:activity": "foundry:assignId",
      "prov:agent": "foundry:docIdAssigner"
    }
  },
  "entity": {
    "foundry:doc1": {
      "prov:label": "document"
    }
  },
  "prefix": {
    "xsd": "http://www.w3.org/2001/XMLSchema",
    "prov": "http://www.w3.org/ns/prov#",
    "foundry": "http://example.org"
  },
  "used": {
    "_:u2": {
      "prov:activity": "foundry:assignId",
      "prov:entity": "foundry:doc1"
    }
  },
  "agent": {
    "foundry:docIdAssigner": {
      "prov:type": {
        "$": "prov:SoftwareAgent",
        "type": "xsd:string"
      }
    }
  },
  "activity": {
    "foundry:assignId": {
      "prov:type": {
        "$": "doc-id-assigment",
        "type": "xsd:string"
      },
      "prov:startTime": "2014-05-16T16:05:00-07:00",
      "prov:endTime": "2014-05-16T16:05:01-07:00"
    }
  }
}
```

## Ingestion WEB Services

I have setup some web services to ingest CINERGI NOAA_NGDC from
hydro10.sdsc.edu for test purposes and comments on tavi.neuinfo.org:8080
open to the UCSD network domain only currently.
Below is some documentation how to use the ingestion and retrieval web
services. The web services are implemented in `ingestor-web` sub-project.


### To get all registered sources (metadata about the metadata XML files) as JSON

```
http://tavi.neuinfo.org:8080/foundry/api/ingestor/sources
```

### To get the CINERGI_NOAA_NGDC source metadata
```
http://tavi.neuinfo.org:8080/foundry/api/ingestor/sources/nlx_999998
```

The metadata includes also JSON schema generated from a
http://hydro10.sdsc.edu/metadata/NOAA_NGDC document used for syntax
validation of the ingested documents.

## To ingest metadata XML documents

The ingestion is assumed to be done in batches, however each document is
ingested individually. Thus at each document ingestion, a parameter named
updateStatus needs to be provided to indicate the batch start and end.

For example to ingest  a group of documents using curl, the start of batch is
indicated by the parameter ingestStatus=start and the end is indicated by the
parameter ingestStatus=end. All other documents in the batch must have
ingestStatus parameter set to `in_process`.
In the commands below the ingested xml files are assumed to be in the current
directory. The URL pattern is
```
http://tavi.neuinfo.org:8080/foundry/api/ingestor/docs/{source-nifId}/{docId}
```
A batchId in the format of 'YYYYMMDD' and an apiKey (shown below) is also
required.

Examples: (This are already ingested to the MongoDB)

```shell
curl
http://tavi.neuinfo.org:8080/foundry/api/ingestor/docs/nlx_999998/0006697F9
74-44DD-80A1-C6F05B250848 -F "file=@0006697F-0974-44DD-80A1-
C6F05B250848.xml" -F "batchId=20140717" -F "ingestStatus=start" -F
"apiKey=<api-key>" -v

curl
http://tavi.neuinfo.org:8080/foundry/api/ingestor/docs/nlx_999998/053B250F-
3EAB-4FA5-B7D0-52ED907A6526 -F "file=@053B250F-3EAB-4FA5-B7D0-
52ED907A6526.xml" -F "batchId=20140717" -F "ingestStatus=end" -F
"apiKey=<api-key>" -v
```

### To get the ingested documents back in XML format

The URL format for getting the original document from the MongoDB and
convert from JSON to original XML format

```
http://tavi.neuinfo.org:8080/foundry/api/ingestor/docs/{source-nifId}/{docId}
```

For example the following retrieve the document ingested by curl commands
above
```
http://tavi.neuinfo.org:8080/foundry/api/ingestor/docs/nlx_999998/0006697F9
74-44DD-80A1-C6F05B250848

http://tavi.neuinfo.org:8080/foundry/api/ingestor/docs/nlx_999998/053B250F-
3EAB-4FA5-B7D0-52ED907A6526
```

