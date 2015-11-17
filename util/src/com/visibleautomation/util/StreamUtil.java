package com.visibleautomation.util;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

/**
 * Grab bag of utilities to read and write data from and to streams.
 */
public class StreamUtil {
    private static final int BUFFER_SIZE = 256;

    public static String readStream(InputStream is, int length) throws IOException {
        byte[] buffer = new byte[length];
        int pos = 0;
        while (length > 0) {
            int nbytes = is.read(buffer, pos, length);
            if (nbytes > 0) {
                pos += nbytes;
                length -= nbytes;
            } else {
                break;
            }
        }
        return new String(buffer);
    }

    public static String readToString(InputStream is) throws IOException {
	byte[] buffer = new byte[BUFFER_SIZE];
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	int nbytes = 0;
	do {
	    nbytes = is.read(buffer, 0, BUFFER_SIZE);
	    if (nbytes > 0) {
		bos.write(buffer, 0, nbytes);
	    }
	} while (nbytes > 0);
	return bos.toString();
    }
}

