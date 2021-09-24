
# WoT Hive
[![Version](https://img.shields.io/badge/Version-0.1.3-orange)](https://github.com/oeg-upm/wot-jtd/releases)] [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![GitHub stars](https://img.shields.io/github/stars/Naereen/StrapDown.js.svg?style=social&label=Star&maxAge=2592000)](https://github.com/oeg-upm/wot-jtd/stargazers)

WoT Hive is an implementation of a [W3C Web of Things directory ](https://www.w3.org/TR/wot-discovery/). This implementation is compliant with the standard specification but aims at providing enriched features thanks to the usage of other W3C standards related to Semantic Web technologies.

**Checkout our [wiki](https://github.com/oeg-upm/wot-hive/wiki) for more documentation**

##  Docker quick start 
Copy this receipt in a *docker-compose.yml* file
````
version: '2'
services:
  triplestore:
    image: acimmino/helio-cloud-rdf4jstore:latest
    ports:
      - '4567:4567'
  wothive:
    image: acimmino/wot-hive:latest
    ports:
      - '9000:9000'
````
Run the docker command
> docker-compose up

##  Jar quick start  
##### `Requires a triple store service publishing a SPARQL endpoint to store the Thing Descriptions`
#### 1. Download the WoT Hive service
Download the latest release of WoT Hive into a folder. Notice that the releases have several files that must be downloaded and placed in the same folder:
* **log4j.properties** allows to customise the logs of the service
* **schema.json** allows to perform JSON schema validation over the Thing Descriptions
* **shape.ttl** allows to perform SHACL shapes validation over the Thing Descriptions
* **wothive.jar** is the jar of the service

Once downloaded all the resources in the same folder the service can be ran using the command
> java -jar wothive.jar

When the service is up and running a file called *configuration.json* will be created in the directory of the jar.  The service will run by default in port 9000. 
#### 2. Set up the triple store
In order to connect the WoT Hive to a remote triple store a `POST` request must be sent to `/configuration/triplestore` containing the in the body the following JSON

````
{
    "updateEnpoint": "http://localhost:4567/sparql",
    "queryEnpoint": "http://localhost:4567/sparql",
    "queryUsingGET": true
}
````
Notice that `"queryEndpoint"`and `"updateEndpoint"` must have as value the correct endpoints of the triple store for either querying or inserting data. Finally, if the triple store implements the SPARQL protocol through `GET` requests then leave `"queryUsingGET": true`, otherwise, for using `POST`set it to false `"queryUsingGET": false`.

## WoT Hive API

| Endpoint 	| Method 	| Headers 	| Reference 	| Description 	|
|---	|---	|---	|---	|---	|
| `/.well-known/wot-thing-description` 	| `GET` 	| `N/A` 	| [Introduction Mechanim](https://w3c.github.io/wot-discovery/#introduction-well-known) 	| Provides the Thing Description of the WoT Hive directory 	|
| `/configuration` 	| `GET` 	| `N/A` 	| [Management](https://w3c.github.io/wot-discovery/#exploration-directory-api-management) 	| Provides a JSON with the all the configurations of the WoT Hive 	|
| `/configuration` 	| `POST` 	| `N/A` 	| [Management](https://w3c.github.io/wot-discovery/#exploration-directory-api-management) 	| The body of the request must contain a JSON with all the configurations of the WoT Hive. 	|
| `/api/things{?offset,limit,sort_by,sort_order}` 	| `GET` 	| `Accept`: `application/ld+json`or `text/turtle`  	| [Listing](https://w3c.github.io/wot-discovery/#exploration-directory-api-registration-listing) 	| Provides a listing of the stored Thing Descriptions in JSON-LD framed or Turtle 	|
| `/api/things` 	| `POST` 	| `Content-Type`: `application/ld+json` 	| [Creation (Anonymous)](https://w3c.github.io/wot-discovery/#exploration-directory-api-registration-creation) 	| Creates an [anonymous Thing Description](https://w3c.github.io/wot-discovery/#dfn-wot-anonymous-thing-description), provided in the body as JSON-LD framed. The generated `:id` is output in the response headers  	|
| `/api/things/{:id}` 	| `GET` 	| `Accept`: `application/ld+json`or `text/turtle` 	| [Retrieval](https://w3c.github.io/wot-discovery/#exploration-directory-api-registration-retrieval) 	| Retrieves the Thing Description with the provided id, in either JSON-LD framed or turtle 	|
| `/api/things/{:id}` 	| `PUT` 	| `Content-Type`: `application/ld+json`or `text/turtle` 	| [Creation](https://w3c.github.io/wot-discovery/#exploration-directory-api-registration-creation) or [Update](https://w3c.github.io/wot-discovery/#exploration-directory-api-registration-update) 	| Creates an Thing Description, provided in the body as JSON-LD framed or turtle 	|
| `/api/things/{:id}` 	| `PATCH` 	| `Content-Type`: `application/merge-patch+json` 	| [Partial Update](https://w3c.github.io/wot-discovery/#exploration-directory-api-registration-update) 	| Partially updates an existing Thing Description, the updates must be provided in JSON-LD framed 	|
| `/api/things/{:id}` 	| `DELETE` 	| `N/A` 	| [Deletion](https://w3c.github.io/wot-discovery/#exploration-directory-api-registration-deletion) 	| Partially updates an existing Thing Description, the updates must be provided in JSON-LD framed 	|
| `api/search/jsonpath{?query}` 	| `GET` 	| `N/A` 	| [JSON path search](https://w3c.github.io/wot-discovery/#jsonpath-semantic) 	| Filters existing Thing Descriptions based on the provided JSON path, the output will be always in JSON-LD framed 	|
| `api/search/sparql{?query}` 	| `GET` 	| `Accept` : `application/sparql-results+json`, `application/sparql-results+xml`, `text/csv`, or `text/tab-separated-values` 	| [SPARQL search](https://w3c.github.io/wot-discovery/#search-semantic) 	| Solves a SPARQL query following the [standard](https://www.w3.org/TR/sparql11-protocol/<br>), results format are in JSON by default if no header is specified. Otherwise available formats are JSON(application/sparql-results+json), XML (application/sparql-results+xml), CSV (text/csv), or TSV (text/tab-separated-values)  	|
| `api/events{?diff}` 	| `GET` 	| `N/A` 	| [Notifications](https://w3c.github.io/wot-discovery/#exploration-directory-api-notification) 	| Subscribe to all the events of the service (`create`, `update`, and `delete`) using the Server-Sends-Events (SSE) protocol 	|
| `api/events/create{?diff}` 	| `GET` 	| `N/A` 	| [Notifications](https://w3c.github.io/wot-discovery/#exploration-directory-api-notification) 	| Subscribe to all the `create` events of the service using the Server-Sends-Events (SSE) protocol 	|
| `api/events/update{?diff}` 	| `GET` 	| `N/A` 	| [Notifications](https://w3c.github.io/wot-discovery/#exploration-directory-api-notification) 	| Subscribe to all the `update` events of the service using the Server-Sends-Events (SSE) protocol 	|
| `api/events/delete{?diff}` 	| `GET` 	| `N/A` 	| [Notifications](https://w3c.github.io/wot-discovery/#exploration-directory-api-notification) 	| Subscribe to all the `delete` events of the service using the Server-Sends-Events (SSE) protocol 	|

[Validation](https://w3c.github.io/wot-discovery/#validation) can  be configured to ran using the JSON schema of the Thing Descriptions and/or their SHACL shapes.
