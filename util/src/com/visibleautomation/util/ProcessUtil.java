package com.visibleautomation.util;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class ProcessUtil {
	public static String pipeCommand(String command) throws Exception {
        String line;
        String[] cmd = { "/bin/sh", "-c", command };
        BufferedReader in = null;
        StringBuffer sb = new StringBuffer();
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            in.close();
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return sb.toString();
    }	
}
