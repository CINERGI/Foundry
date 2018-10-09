#!/bin/sh

rm -rf  /data/apache-tomcat-7.0.57/webapps/foundry*
cp target/foundry.war /data/apache-tomcat-7.0.57/webapps

