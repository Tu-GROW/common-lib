package com.iris.common.lib.utils;

import static com.iris.common.lib.utils.Helpers.convertObjectToJson;
import static com.iris.common.lib.utils.Helpers.convertStringToObject;
import static com.iris.common.lib.utils.Helpers.readFromResourcesFolder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

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

  // ── convertObjectToJson(ObjectMapper, Object, String) ─────────────────────

  @Test
  void givenValidObject_whenConvertObjectToJsonWithMapper_returnJsonString() {
    ObjectMapper mapper = new ObjectMapper();
    RandomObject obj = new RandomObject("age", 10);

    String result = convertObjectToJson(mapper, obj, "{}");

    assertEquals("{\"key\":\"age\",\"value\":10}", result);
  }

  @Test
  void givenUnserializableObject_whenConvertObjectToJsonWithMapper_returnFallback() {
    // ObjectMapper fails on empty beans by default — Object() has no properties
    ObjectMapper mapper = new ObjectMapper();

    String result = convertObjectToJson(mapper, new Object(), "{}");

    assertEquals("{}", result);
  }

  // ── convertStringToObject(ObjectMapper, String, TypeReference, T) ─────────

  @Test
  void givenValidJson_whenConvertStringToObjectWithTypeRef_returnDeserializedMap() {
    ObjectMapper mapper = new ObjectMapper();
    String json = "{\"status\":\"active\",\"amount\":150}";

    Map<String, Object> result = convertStringToObject(
            mapper, json, new TypeReference<>() {}, new LinkedHashMap<>());

    assertEquals("active", result.get("status"));
    assertEquals(150, result.get("amount"));
  }

  @Test
  void givenNullJson_whenConvertStringToObjectWithTypeRef_returnFallback() {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> fallback = new LinkedHashMap<>();

    Map<String, Object> result = convertStringToObject(
            mapper, null, new TypeReference<>() {}, fallback);

    assertEquals(fallback, result);
  }

  @Test
  void givenBlankJson_whenConvertStringToObjectWithTypeRef_returnFallback() {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> fallback = new LinkedHashMap<>();

    Map<String, Object> result = convertStringToObject(
            mapper, "   ", new TypeReference<>() {}, fallback);

    assertEquals(fallback, result);
  }

  @Test
  void givenInvalidJson_whenConvertStringToObjectWithTypeRef_returnFallback() {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> fallback = new LinkedHashMap<>();

    Map<String, Object> result = convertStringToObject(
            mapper, "not valid json", new TypeReference<>() {}, fallback);

    assertEquals(fallback, result);
  }

}
