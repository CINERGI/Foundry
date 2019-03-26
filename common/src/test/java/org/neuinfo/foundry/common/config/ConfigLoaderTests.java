package org.neuinfo.foundry.common.config;
import org.junit.Rule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.neuinfo.foundry.common.util.Utils;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigLoaderTests {
 @Rule
 public final EnvironmentVariables environmentVariables
         = new EnvironmentVariables();

 @ParameterizedTest
 @ValueSource(strings = { "home",
         "${nohome:home}" })
 public void testEnvParser(String value) {
  String result = ConfigLoader.envVarParser(value);
  assertTrue  (result.equals("home"));
 }
 @ParameterizedTest
 @ValueSource(strings = {
         "${HOME:home}" })
 public void testEnvParser2(String value){

  String result =ConfigLoader.envVarParser(value);
  assertFalse(result.equals("home"));
 }
 @Test
 void testThisThing() {
  environmentVariables.set("F_BROKERURL", "tcp:scigraph");
  environmentVariables.set("F_PLUGINDIR", "/var/temp");
  environmentVariables.set("MONGODBUSER", "NOUSER");
  environmentVariables.set("F_SCIGRAPH_STOPWORDSURL", "file:///stope.txt");
  environmentVariables.set("F_WAF_OUTPUTDIR", "/example/test");
  Map<String, String> env = System.getenv();
  assertEquals("/example/test", System.getenv("F_WAF_OUTPUTDIR"));
 }


 @ParameterizedTest
 @ValueSource(strings = {
         "../consumers/src/main/resources/dev/consumers-cfg.xml",
         "../dispatcher/src/main/resources/dev/dispatcher-cfg.xml" })
 public void testLoadWithEnv(String value) throws Exception{
  environmentVariables.set("F_BROKERURL", "tcp:scigraph");
  environmentVariables.set("F_PLUGINDIR", "/var/temp");
  environmentVariables.set("MONGODBUSER", "NOUSER");
  environmentVariables.set("F_DB_DB", "ANYDB");
  environmentVariables.set("F_DB_COLLECTION", "ANYCOLL");

  environmentVariables.set("F_SCIGRAPH_STOPWORDSURL", "file:///stope.txt");
  environmentVariables.set("F_WAF_OUTPUTDIR", "/example/test");
  Map<String, String> env = System.getenv();
  assertEquals("/example/test", System.getenv("F_WAF_OUTPUTDIR"), "TEST WILL FAIL. ENV not being set");
 Configuration config = ConfigLoader.loadFromFile(value);
 assertNotNull(config);
 assertEquals("tcp:scigraph", config.brokerURL);
  assertEquals("ANYDB", config.mongoDBName);
  assertEquals("ANYCOLL", config.collectionName);
  List<WorkflowMapping> wfm = config.getWorkflowMappings();
  List<Workflow> wf = config.getWorkflows();



  List<ServerInfo> siL = config.getMongoServers();

  assertEquals("NOUSER",siL.get(0).getUser() );
 config.getWorkflowMappings();
 config.getWorkflows();


 }
}
