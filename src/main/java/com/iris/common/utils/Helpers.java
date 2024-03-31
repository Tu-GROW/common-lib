package com.iris.common.utils;


import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
}
