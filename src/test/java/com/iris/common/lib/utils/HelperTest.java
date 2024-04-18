package com.iris.common.lib.utils;

import static com.iris.common.lib.utils.Helpers.convertObjectToJson;
import static com.iris.common.lib.utils.Helpers.convertStringToObject;
import static com.iris.common.lib.utils.Helpers.readFromResourcesFolder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HelperTest {

  record RandomObject(String key, Integer value) {}

  @Test
  void givenValidObject_whenConvertObjectToJson_returnJsonString() {
    RandomObject randomObject = new RandomObject("age", 10);
    String jsonString = convertObjectToJson(randomObject);
    assertEquals("{\"key\":\"age\",\"value\":10}", jsonString);
  }

  @Test
  void givenInvalidObject_whenConvertObjectToJson_throwException() {
    Object obj = new Object();
    assertThrows(RuntimeException.class, () -> {
      convertObjectToJson(obj);
    }, "Failed to convert " + obj + " to Json String");
  }

  @Test
  void givenFileDoesNotExist_whenReadFromResourceFolder_throwException(){
    String filePath = "somefile.txt";
    assertThrows(RuntimeException.class, () -> {
     readFromResourcesFolder(filePath);
    }, "Could not read " + filePath + " from resources folder");
  }

  @Test
  void givenFileExist_whenReadFromResourceFolder_returnContent(){
    String fileContent = readFromResourcesFolder("classpath:test.txt");
    assertEquals("File Read Successful\n", fileContent);
  }

  @Test
  void givenJsonString_whenConvertToAnObject_returnJavaObject(){
    String jsonString = "{\"key\":\"age\",\"value\":10}";
    RandomObject object = convertStringToObject(jsonString, RandomObject.class);
    assertEquals("age",object.key );
    assertEquals(10, object.value );
  }

  @Test
  void givenJsonString_whenConvertToAnObject_throwException() {
    String jsonString = "{\"name\":\"test\",\"age\":10}";
    assertThrows(RuntimeException.class, () -> {
      convertStringToObject(jsonString, RandomObject.class);
    });
  }

}
