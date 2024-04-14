package com.iris.common.lib.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.ResourceUtils;


public class Helpers {
    public String maskMsisdn(String string){
        try {
            Pattern pattern = Pattern.compile("\\b254\\w{4}");
            Matcher matcher = pattern.matcher(string);
            while (matcher.find()) {
                String orig = string.substring(matcher.start(), matcher.end());
                string = string.replaceAll(orig, "254****");
                matcher = pattern.matcher(string);
            }
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
        return string;
    }

    public static String date(){
        String pattern = "yyyyMMddHHmmss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return  simpleDateFormat.format(new java.util.Date());
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
            return  new String(Files.readAllBytes(file.toPath()));
        } catch (IOException exception) {
            throw new RuntimeException(
                String.format("Could not read %s from resources folder [%s]", filePath, exception.getMessage()));
        }
    }
}
