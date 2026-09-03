package com.se196693.mvc.utils;

public final class FileUtils {
    private FileUtils(){}

    public static String getExtension(String filename){
        if (filename == null || filename.isBlank()){
            return "";
        }

        int lastDotIndex = filename.lastIndexOf(".");

        if (lastDotIndex == -1 || lastDotIndex == filename.length()-1){
            return "";
        }

        return filename.substring(lastDotIndex+1).toLowerCase();
    }
}
