
The web service is deployed for experiments at `http://photon.sdsc.edu:8080/cinergi/SpatialEnhancer`

Here is a sample invocation using curl:
```
curl -v -X POST --data-binary "@/scratch/slocal/cycore/tomcat6-beta/webapps/cinergi/WEB-INF/metadata/samples/NCDC_GIS/00C2609C-C2E2-4DFB-A9BB-E687F8B42E24.xml" -H"Content-Type: application/xml" -H"Accept: application/json" http://photon.sdsc.edu:8080/cinergi/SpatialEnhancer
```
