# Harvest Workflow #

Based on discussion 2014-11-14, Steve Richard and Burak Ozyurt

1. get file from source
2. Interrogate. Determine if the file contains content that can be processed. 
3. If it can't be processed, send file to an error queue. Done
4. If file contains content that can be processed, start ingest process
5. convert input file to JSON using a reversible XML or text to JSON mapping. If input is JSON, don't need to convert.
6. Create new MongoDb metadata document (MMD), insert the JSON representation of the original source document (OSD) into the originalDoc key in the MMD. push PROV record.
7. Calculate originalRecordIdentifier value according to the originalRecordIdentiferSpec element of the sourceConfiguration.json file, and insert in the MMD
8. Interrogate the OSD JSON to determine what metadataLoader consumer process needs to be run to load the metadata content from the ODS into the MMD.metadataProperties, MMD.resourceDescription, and MMD.extras keys.
9. Run metadata loader, update MongoDb, push Prov record or appropriate error log messages
10. Run other enhancers according to the workflow defined in the documentProcessing key in the sourceConfiguration.json file.  Each enhancer pushes PROV records as necessary.
11. Index new MMD
12. push MMD in appropriate format to other endpoints (pivotViewers, WAF for harvest by other catalogs, etc.)