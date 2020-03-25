package com.harryawoodworth.vantage_java_assignment.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import com.harryawoodworth.vantage_java_assignment.util.Logger;

/**
 * Helper class has toFile() to allow writing a string to a file
 */
public class OutputHelper {

    // Taking a filepath, filename, and output string, output the string to the file
    public static void toFile(String filepath, String filename, String outputStr) {

        Logger.logI("Outputting formatted string to file...");

        FileOutputStream writer = null;
        File outputFile = null;

        // Open a file output stream and write contents to file
        try {
            outputFile = new File(filepath + filename);
            writer = new FileOutputStream(outputFile);

            // If the file does not exist, create it
            if (!outputFile.exists())
				outputFile.createNewFile();

            // Covert the output string to bytes
            byte[] bytes = outputStr.getBytes();

            // Write bytes to file
            writer.write(bytes);
            writer.flush();

        } catch(IOException e) {
            Logger.logE("Failed to write output file '" + filename + "'", e);
        } finally {
            // Close the file output stream
            try {
                if(writer != null)
                    writer.close();
            } catch (IOException e) {
                Logger.logE("Failed to close output stream", e);
            }

        }
    }

}
