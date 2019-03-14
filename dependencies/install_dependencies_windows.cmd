
call mvn install:install-file -Dfile=lib/bnlpkit-0.5.11.jar -DgroupId=bnlp -DartifactId=bnlpkit -Dversion=0.5.11 -Dpackaging=jar
call mvn install:install-file -Dfile=lib/commonlib-1.4.jar -DgroupId=bnlp -DartifactId=commonlib -Dversion=1.4 -Dpackaging=jar
call mvn install:install-file -Dfile=lib/mallet.jar -DgroupId=bnlp -DartifactId=mallet -Dversion=1.0 -Dpackaging=jar
call mvn install:install-file -Dfile=lib/mallet-deps.jar -DgroupId=bnlp -DartifactId=mallet-deps -Dversion=1.0 -Dpackaging=jar
call mvn install:install-file -Dfile=lib/guilib_0.4.jar -DgroupId=bnlp -DartifactId=guilib -Dversion=0.4 -Dpackaging=jar
call mvn install:install-file -Dfile=lib/jcckit.jar -DgroupId=jcckit -DartifactId=jcckit -Dversion=1.1 -Dpackaging=jar
call mvn install:install-file -Dfile=lib/jsvmlight.jar -DgroupId=bnlp -DartifactId=jsvmlight -Dversion=0.1 -Dpackaging=jar
call mvn install:install-file -Dfile=lib/bnlpkit-cinergi-models-0.2.jar -DgroupId=bnlp -DartifactId=bnlpkit-cinergi-models -Dversion=0.2 -Dpackaging=jar
call mvn install:install-file -Dfile=lib/prov-json-0.5.1-SNAPSHOT.jar -DgroupId=org.openprovenance.prov -DartifactId=prov-json -Dversion=0.5.1-SNAPSHOT -Dpackaging=jar
call mvn install:install-file -Dfile=lib/prov-model-0.5.1-SNAPSHOT.jar -DgroupId=org.openprovenance.prov -DartifactId=prov-model -Dversion=0.5.1-SNAPSHOT -Dpackaging=jar
call mvn install:install-file -Dfile=lib/prov-xml-0.5.1-SNAPSHOT.jar -DgroupId=org.openprovenance.prov -DartifactId=prov-xml -Dversion=0.5.1-SNAPSHOT -Dpackaging=jar
call mvn install:install-file -Dfile=$PWD/lib/geoportal-commons-gpt-client-2.6.2-SNAPSHOT.jar -DgroupId=com.esri.geoportal -DartifactId=geoportal-harvester-cli -Dversion=2.6.2-SNAPSHOT -Dpackaging=jar