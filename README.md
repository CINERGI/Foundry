Foundry
=======

Getting the code
----------------

    cd $HOME
    git clone https://<username>@github.com/CINERGI/Foundry
    cd $HOME/Foundry

Building
--------

Before you start the build process, you need to install three libraries from `dependencies` directory to your local maven repository
    
    cd $HOME/Foundry/dependencies
    ./install_prov_xml_2mvn.sh
    ./install_prov_model_2mvn.sh
    ./install_prov_json__2mvn.sh
    ./install_bnlp_2mvn.sh
    ./install_bnlp_dependencies_2mvn.sh
    ./install_bnlp_model2mvn.sh

Afterwards

    mvn clean compile assembly:single
    cp dispatcher/target/foundry-dispatcher-1.0-SNAPSHOT-prod.jar bin
    cp consumers/target/foundry-consumers-1.0-SNAPSHOT-prod.jar bin/
    cp ingestor/target/foundry-ingestor-1.0-SNAPSHOT-prod.jar bin/

MongoDB Replicate Set
---------------------

```
mongod --replSet mongotest --port 27017 --dbpath /var/burak/mongodb/disco1 --smallfiles --oplogSize 128
mongod --replSet mongotest --port 27018 --dbpath /var/burak/mongodb/disco2 --smallfiles --oplogSize 128
mongod --replSet mongotest --port 27019 --dbpath /var/burak/mongodb/disco3 --smallfiles --oplogSize 128
```
## Replicate Set Configuration (One time)

Connect to first node and run `rs.initiate()` 
```
mongo --port 27017
>rs.initiate()
```
After that add the other two members (one being the arbiter node) to the just configured replica set. (Using the hostname of your machine instead of `burak.crbs.ucsd.edu` of course)

```
>rs.add("burak.crbs.ucsd.edu:27018")
{"ok":1}
>rs.add("burak.crbs.ucsd.edu:27019", {arbiterOnly:true})
{"ok":1}
```
Now you are done with the replicate set configuration and can exit MongoDB client via `quit()` command. 

You can connect to PRIMARY via CLI mongo client any time via

```
mongo --port 27017
```

ActiveMQ
--------

* Download and unpack [Apache ActiveMQ 5.10.0 Release](http://activemq.apache.org/activemq-5100-release.html) to a directory of your choosing (`$MQ_HOME`).

* To start message queue server at default port `61616`, go to `$MQ_HOME/bin` directory and run
```
    activemq start 
```
* To stop the activemq server
```
    activemq stop
```

ElasticSearch
-------------

    curl 'http://localhost:9200/?pretty'

Querying documents for responsible organisation fields that contains the term `Boulder`

    curl -XGET localhost:9200/nif/_search -d '{
       "query":{"match":{"cinergi.gmi:MI_Metadata.gmd:contact.gmd:CI_ResponsibleParty.gmd:organisationName.gco:CharacterString._$":"Boulder"}}}'


Further Documentation
---------------------

 * [System Architecture](doc/architecture.md)
 * [Developer Guide](doc/dev_guide.md) 
 * [Guide](doc/guide.md) 
 * [Document Ingestion](doc/doc_ingestion.md)
 * [Management Interface Web Services](doc/management_ws.md)
 * [Harvest Description](doc/harvest_desc.md)

