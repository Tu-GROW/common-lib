package com.iris.common.lib.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.ResourceUtils;


@Slf4j
public class Helpers {

    public String maskMsisdn(String string) {
        try {
            Pattern pattern = Pattern.compile("\\b254\\w{4}");
            Matcher matcher = pattern.matcher(string);
            while (matcher.find()) {
                String orig = string.substring(matcher.start(), matcher.end());
                string = string.replaceAll(orig, "254****");
                matcher = pattern.matcher(string);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return string;
    }

    public static String date() {
        String pattern = "yyyyMMddHHmmss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(new java.util.Date());
    }

    /**
     * Converts a given object to a json string.
     *
     * @param obj object to be converted to json
     * @return Object as json string
     */
    public static String convertObjectToJson(Object obj) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(
                String.format("Failed to convert %s to Json String", obj.toString()));
        }

    }

    /**
     * @param filePath the path of the file from resources directory
     * @return string of contents of the file
     */
    public static String readFromResourcesFolder(String filePath) {
        try {
            File file = ResourceUtils.getFile(filePath);
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException exception) {
            throw new RuntimeException(
                String.format("Could not read %s from resources folder [%s]", filePath, exception.getMessage()));
        }
    }

    public static <T> T convertStringToObject(String jsonString, Class<T> classObject){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(jsonString, classObject);
        } catch (Exception e) {
            throw new RuntimeException("Could not convert ["+jsonString+"] to an object", e);
        }
    }

    /**
     * Serializes an object to a JSON string using a provided {@link ObjectMapper}.
     * Returns {@code defaultOnFailure} and logs a warning if serialization fails,
     * so the caller's flow is never interrupted by a logging/metadata write error.
     *
     * @param mapper          the configured ObjectMapper to use
     * @param obj             the object to serialize
     * @param defaultOnFailure value to return when serialization fails (e.g. {@code "{}"})
     * @return JSON string, or {@code defaultOnFailure} on error
     */
    public static String convertObjectToJson(ObjectMapper mapper, Object obj, String defaultOnFailure) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return defaultOnFailure;
        }
    }

    /**
     * Deserializes a JSON string to a complex generic type using a {@link TypeReference}.
     * Returns {@code defaultOnFailure} and logs a warning if deserialization fails.
     * Use this overload when the target type involves generics (e.g. {@code Map<String, Object>})
     * that cannot be expressed with a plain {@link Class}.
     *
     * @param mapper          the configured ObjectMapper to use
     * @param json            the JSON string to deserialize
     * @param typeRef         TypeReference describing the target generic type
     * @param defaultOnFailure value to return when deserialization fails (e.g. {@code new LinkedHashMap<>()})
     * @return deserialized object, or {@code defaultOnFailure} on error
     */
    public static <T> T convertStringToObject(ObjectMapper mapper, String json, TypeReference<T> typeRef, T defaultOnFailure) {
        if (json == null || json.isBlank()) return defaultOnFailure;
        try {
            return mapper.readValue(json, typeRef);
        } catch (IOException e) {
            log.warn("Failed to deserialize JSON string: {}", e.getMessage());
            return defaultOnFailure;
        }
    }
}
