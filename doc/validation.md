**Validation operation**

Validation enhancer:

1. get record from mongo
1. convert from JSON to ISO19139 XML
1. invoke TEAM engine validator
1. Get response; parse to determine if validation was successful. populate metadataObject/metadataProperties/metadataConformanceTests with the following values:
    a. success:  
        conformanceTestName: "TEAM engine validation, <ruleSetName>"
        conformanceResult:"Pass"
    b. failure:
        conformanceTestName: "TEAM engine validation, <ruleSetName>"
        conformanceResult:"Fail"
        conformanceTestNote:"[[CDATA]] <xml content returned from TEAM engine> [[CDATA]]"
        
Subsequent enhancer pipeline will need to run to handle validation fails

1. filter Staging db for metadataObject/harvestInformation/BatchID=<the correct batchID> and conformanceTestNote="Fail"; get list of metadata UUIDs
2. enhancer takes list of UUIDS, passes {UUID,Title} tuples to a user interface that allows the user to pick the record to work on (?enable fancy filtering to look for particular content in the conformanceTestNote?)
3. pull record from mongo, convert to ISO19139 XML, pass to form-based UI that lets curator inspect the validation report a edit appropriately
4. when done, 'commit' the record; pushes back to enhancer wrapper
5. wrapper coverts to JSON, updates MongoDb metadata object, which triggers new 'import pipeline' workflow; hopefully the edited record now validates.
6. remove record from list of invalid records
7. user pick next record to work on.


Encoding of TEAM engine results in ISO19139:

```
<gmd:dataQualityInfo>
      <gmd:DQ_DataQuality>
         <gmd:scope>
            <gmd:DQ_Scope>
               <gmd:level><gmd:MD_ScopeCode codeList="http://  iso codelist url" codeListValue="dataset"></gmd:MD_ScopeCode>
              </gmd:level>
            </gmd:DQ_Scope>
         </gmd:scope>
         <gmd:report>
            <!-- which report type is selected is sort of a moot point; none of them fits perfectly, let's just use this -->
            <gmd:DQ_FormatConsistency>
               <gmd:result>
                  <gmd:DQ_ConformanceResult>
                     <gmd:specification>
                        <gmd:CI_Citation>
                           <gmd:title>
                              <gco:CharacterString>TEAM engine, ruleset name here</gco:CharacterString>
                           </gmd:title>
                           <gmd:date gco:nilReason="missing"/>
                              <!--  ruleset should have a date, put that here-->
                        </gmd:CI_Citation>
                     </gmd:specification>
                     <gmd:explanation>
                        <gco:CharacterString>
                           [[CDDATA]] put the xml returned by the TEAM engine here; put in CDATA section so it doesn't try to validate[[CDATA]]
                        </gco:CharacterString>
                     </gmd:explanation>
                     <gmd:pass>
                        <!-- true or false for pass, fail -->
                        <gco:Boolean>false</gco:Boolean>
                     </gmd:pass>
                  </gmd:DQ_ConformanceResult>
               </gmd:result>
            </gmd:DQ_FormatConsistency>
         </gmd:report>
      </gmd:DQ_DataQuality>
   </gmd:dataQualityInfo>
```
   