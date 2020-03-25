package com.harryawoodworth.vantage_java_assignment;

import com.harryawoodworth.vantage_java_assignment.util.CochraneLibraryScraper;
import com.harryawoodworth.vantage_java_assignment.util.OutputHelper;
import com.harryawoodworth.vantage_java_assignment.util.Logger;
import java.lang.NumberFormatException;

import com.harryawoodworth.vantage_java_assignment.models.Review;

public class Runner {

    private static final String FILE_PATH = "C:\\vantage_java_assignment\\output\\";
    private static final String OUTPUT_FILE_NAME = "cochrane_reviews.txt";

    /**
     * Arg 1 - File Path (Not checked for formatting)
     * Arg 2 - File Name (Not checked for formatting)
     * Arg 3 - Number of topics to scrape (0 for all)
     */
    public static void main( String[] args ) {

        // Parse the arguments
        int numTopicsToScrape = 0;
        String filePath = null;
        String fileName = null;
        if(args.length == 3) {
            filePath = args[0];
            fileName = args[1];
            // Parse the int arg
            try {
                numTopicsToScrape = Integer.parseInt(args[2]);
                if(numTopicsToScrape < 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                Logger.logE("Third Arg:" + args[0] + " must be an integer >= 0 to declare how many topics to scrape.", e);
                return;
            }
        } else {
            Logger.logI("Usage: arg1-String, filepath for output | arg2-String, output file name | arg3-Int, number of topics to scrape (0 for all)");
            return;
        }

        // Scrape Cochrane Library reviews, get formatted string of results
        String formattedReviewResults = CochraneLibraryScraper.scrape(numTopicsToScrape);

        // Output to file
        OutputHelper.toFile(FILE_PATH, OUTPUT_FILE_NAME, formattedReviewResults);
        Logger.logI("Completed. Read results at " + filePath + fileName);
    }

}
