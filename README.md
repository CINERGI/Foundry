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

    mvn -Pdev clean install

Here dev profile is used. There are production `prod` and `dev` profiles for differrent configurations for development and production environments.

The configuration files are located under each sub-project. For example,
the configuration files for the dispatcher component are located under
`$HOME/Foundry/dispatcher/src/main/resources`.


```
$HOME/Foundry/dispatcher/src/main/resources
├── dev
│   └── dispatcher-cfg.xml
└── prod
    └── dispatcher-cfg.xml
```

When you use `-Pdev` argument, configuration file from the `dev` directory is included in the jar file.

All subsystem configuration files are generated from a master configuration file in YAML format.
An example master configuration file can be found at `$HOME/Foundry/bin/config-spec.yml.example`.
Once you create a master config file named say `config.yml` run the following to generate all configuration files for the subsystems (for dev profile)

```
cd $HOME/Foundry/bin
./config_gen.sh -c config.yml  -f $HOME/Foundry -p dev

```

```
./config_gen.sh -h
usage: ConfigGenerator
 -c <cfg-spec-file>      Full path to the Foundry config spec YAML file
 -f <foundry-root-dir>
 -h                      print this message
 -p <profile>            Maven profile ([dev]|prod)
```

After each configuration file generation you need to run maven to move the configs to their target locations

    mvn -Pdev install

MongoDB
--------

The system uses MongoDB as its backend. Both 2.x and 3.x versions of MongoDB are tested with the system. If you are using MongoDB 3.x, preferred storage engine is wiredTiger.



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

Running the system
------------------

The system consists of a dispatcher, a consumer head and a CLI manager interface.
The dispatcher listens to the MongoDB changes and using
its configured workflow dispatches messages to the message queue for the
listening consumer head(s). The consumer head coordinates a set of configured
consumers that do a prefined operation of a document indicated by the message
they receive from the dispatcher and ingestors. The ingestors are specialized
consumers that are responsible for the retrieval of the original data as
configured by harvest descriptor JSON file of the corresponding source.
They are triggered by the manager application.

## Initial Setup

Before any processing the MongoDB needs to be populated with the source descriptors using
the `$HOME/Foundry/bin/ingest_src_cli.sh`. 

```
./ingest_src_cli.sh -h
usage: SourceIngestorCLI
  -c <config-file>        config-file e.g. ingestor-cfg.xml (default)
  -d                      delete the source given by the source-json-file
  -h                      print this message
  -j <source-json-file>   harvest source description file
  -u                      update the source given by the source-json-file
```

Example source descriptors are under `$HOME/Foundry/consumers/etc`.

An example usage for inserting IEDA Earthchem Library source descriptor document to the `sources` collection is show below

 ```
./ingest_src_cli.sh -j $HOME/Foundry/consumers/etc/cinergi-0023.json
```

Source descriptors are JSON files. To create a new source descriptor for a new resource, copy one of the example source descriptors to a new file and edit it. For more information about a source/harvest descriptor see [Harvest Description](doc/harvest_desc.md).


## Dispatcher

The script for the dispatcher component `dispatcher.sh` is located in 
`$HOME/Foundry_ES/bin`. By default it uses `dispatcher-cfg.xml` file for the 
profile specified during the build. This needs to run in its own process. 
To stop it, use `Ctrl-C`.

```
 ./dispatcher.sh -h
 usage: Dispatcher
  -c <config-file>   config-file e.g. dispatcher-cfg.xml (default)
  -h                 print this message
```


## Consumer Head
The script for the consumer head component `consumer_head.sh` is located in 
`$HOME/Foundry_ES/bin`. By default it uses `consumers-cfg.xml` file for the 
profile specified during the build. For production use you need to specify 
 `-f` option. This needs to run in its own process. To stop it, use `Ctrl-C`.


 ```
 ./consumer_head.sh -h
 usage: ConsumerCoordinator
  -c <config-file>          config-file e.g. consumers-cfg.xml (default)
  -cm                       run in consumer mode (no ingestors)
   -f                        full data set default is 100 documents
   -h                        print this message
   -n <max number of docs>   Max number of documents to ingest
   -p                        send provenance data to prov server
   -t                        run ingestors in test mode
```

## Manager

Manager is an interactive command line application for sending ingestion start messages for resources to the consumer head(s). 
It also have some convenience functions to cleanup MongoDB data for a given resource and delete ElasticSearch indices. 
By default manager app uses `dispatcher-cfg.xml` file for the profile specified 
during the build. 

```
./manager.sh -h
usage: ManagementService
 -c <config-file>   config-file e.g. dispatcher-cfg.xml (default)
 -h                 print this message
```

---------------------

 * [System Architecture](doc/architecture.md)
 * [Developer Guide](doc/dev_guide.md) 
 * [Guide](doc/guide.md) 
 * [Document Ingestion](doc/doc_ingestion.md)
 * [Management Interface Web Services](doc/management_ws.md)
 * [Harvest Description](doc/harvest_desc.md)

