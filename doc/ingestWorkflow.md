when a harvested metadata record is ingested,

1. Check if record is already in the staging dB. Use the unique identifier for the record as specified in the harvestSourceObject. These identifiers must be unique within the particular CINERGI HarvestSource.

2. Choice:

  - if new, push PROV record indicating new record
  - if not new Has it changed?  Test based on field by field comparison; determine JSON path for specific fields that have changed.
    - CHOICE: 
      - if changed it pushes PROV record documenting harvest of updated metadata record; the PROV content should record which fields changed, and should create successor/predessor links for version IDs on the Metadata UUID.
PROV  documents the start of the enhance workflow. 
      - if not changed, do nothing

3. New or changed metadata
    Convert to JSON. Save the harvested object in external file. Increment the MetadataObject/HarvestInformation/Version if have a changed metadata record input. Updated record will have same UUID (metadataObject/primaryKey), but a new version number.
    Write new record to MongoDb (keep old version-- is this done using MongoInternal management or by the CINERGI app?)

4. Ingest triggers Mongo status change in Mongo;


##NOTE:
AT this point the Ingest workflow joins with any other workflow that triggers a MongoDb status change event indicating that a metadata record has changed

  - dispatcher is watching MongoDb events; 
  5. dispatcher catches event, 
    - if the document status indicates Error, STOP process and exit
    - use metadataObject/processing/status value to determine where the record is in the workflow. Get the sourceHarvestObject for the affected record. The sourceHarvestObject content has link to a workflow object;  Dispatcher uses information in the processingWorkflow key in the HarvestSourceObject to get the workflow plan from the list of pre defined workflows that the harvester has been preconfigured to run.  Workflows are identified by ID's in the HarvestSourceObject and workflow process control has to be preconfigured in the system (in the dispatcher or in teh consumer head?).
    - if the document has reached the end of its current workflow process
    1. post PROV record indicating successful completion of process workflow
    2. update the metadatObject/metadataProperties/metadataRecordHistory key in the MongoMetadataObject to summarize the workflow process, including any info or warning messages posted during the process.
    3. End of processing workflow--EXIT
    
    - if the document has remaining processing steps in its workflow:
    - Dispatcher posts a job fpr the next process step on the document.
    - Available Consumer head picks up job
    - Consumer head triggers enhancer
    - enhancer runs     
       - on enhancer success: 
          POST to PROV; should include label, when, WHAT/how-statement of what was done, 
          enhanced MongoDb object is pushed back to MongoDb (no Prov information put in MongoDb object)
     that triggers new mongDb update event that dispatcher catches. 
     Loop back to number 5 above
     
On failure in enhancer:  Error catching... logging is happening on server. Enhancer puts note documenting Error in the metadatObject/metadataProperties/-metadataRecordHistory object processing.   push 'enhancer failed' prov record
Mongo throws update event, dispatcher sees error and stops the process. 
