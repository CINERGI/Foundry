package org.neuinfo.foundry.common.config;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.neuinfo.foundry.common.util.Utils;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigLoaderTests {
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
 }


 @ParameterizedTest
 @ValueSource(strings = {
         "../dispatcher/src/main/resources/dev/dispatcher-cfg.xml" })
 public void testLoadWithEnv(String value) throws Exception{

 Configuration config = ConfigLoader.loadFromFile(value);
 assertNotNull(config);

 }
}
