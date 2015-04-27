package com.visibleautomation.util;
import java.util.List;
import java.util.ArrayList;

public class StringUtil {
    // given a string with delimiters, return a list of strings split by the delimiter
    public static List<String> splitList(String s, String delim) {
        String[] array = s.split(delim);
        ArrayList<String> list = new ArrayList<String>(array.length);
        for (int i = 0; i < array.length; i++) {
            list.add(array[i]);
        }
        return list;
    }
}
