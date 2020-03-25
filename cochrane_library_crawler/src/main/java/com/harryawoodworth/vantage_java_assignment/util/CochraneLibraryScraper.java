package com.harryawoodworth.vantage_java_assignment.util;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.HttpRequest;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.lang.StringBuilder;
import com.harryawoodworth.vantage_java_assignment.util.Logger;
import com.harryawoodworth.vantage_java_assignment.models.Review;

/**
 * Call scrape() to return a string of formatted review entry information from all
 * topic categories in the cochrane library.
 */
public class CochraneLibraryScraper {

    // Constants
    private static final String COCHRANE_LIBRARY_TOPICS_URL = "https://www.cochranelibrary.com/cdsr/reviews/topics";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36";
    private static final int MAX_CONNECTIONS = 40;
    private static Set<Review> reviewSyncSet;

    // Scrape the Cochrane Library topics page, and get each review and store it's
    // Info inside of a Review object. Store all of the Reviews inside of a synchronized
    // Set, and then return the formatted string of the Set's contents.
    public static String scrape(int numTopics) {

        // Synchronized set of Review objects
//        reviewSyncSet = Collections.synchronizedSet(new HashSet<Review>());
        // Unsynchronized set of Review objects
        reviewSyncSet = new HashSet<Review>();
        // Declare request outside of try/catch to close in finally block
        HttpGet topicRequest = null;
        // Create http client with a thread-safe connection manager
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(MAX_CONNECTIONS);
        final CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
        // Config for allowing circular redirects for http get requests
        final RequestConfig httpGetConfig = RequestConfig.custom().setCircularRedirectsAllowed(true).build();

        int numReviewsScraped = 0;

        // Create a GET request at the cochrane library topics url (using Chrome User-Agent)
        try {
            Logger.logI("Executing GET request on Cochrane Library topics page...");
            topicRequest = new HttpGet(COCHRANE_LIBRARY_TOPICS_URL);
            topicRequest.addHeader("User-Agent", USER_AGENT);

            // Execute the request using the http client
            CloseableHttpResponse response = null;
            try {
                response = httpClient.execute(topicRequest);
                int statusCode = response.getStatusLine().getStatusCode();
                if(statusCode != 200)
                    throw new RuntimeException("Http connection not ok: " + statusCode);
                HttpEntity entity = response.getEntity();
                String responseString = EntityUtils.toString(entity, "UTF-8");

                // Parse the response using Jsoup, grab the topic urls and names
                Logger.logI("Parsing topic names and links...");
                Document parsedPage = Jsoup.parse(responseString, "UTF-8");
                Elements topicLinks = parsedPage.select("li.browse-by-list-item > a");
                String[] topicNames = topicLinks.eachText().toArray(new String[0]);
                String[] topicUrls = topicLinks.eachAttr("abs:href").toArray(new String[0]);

                // Check if the topic names and urls are aligned (in case of UI change) or weren't scraped
                if(topicNames.length == 0 || topicNames.length != topicUrls.length)
                    throw new RuntimeException("Topic Names and URLs not aligned or not scraped: Topic Names: " + topicNames.length + " Topic URLs: " + topicUrls.length);

                // Set numTopics in case of 0 to scrape all topics
                if(numTopics == 0)
                    numTopics = topicNames.length;

                // Start a thread for each topic
                Logger.logI("Running threads to scrape topic reviews...");
//                ExecutorService threadPool = Executors.newFixedThreadPool(topicNames.length);
                for(int topicIndex = 0; topicIndex < numTopics; topicIndex++) {

                    // Save the topic name and url as final to access inside each thread
                    final String topicNameFinal = topicNames[topicIndex];
                    final String topicUrlFinal = topicUrls[topicIndex];

                    // Each thread scrapes all of the result pages under a topic
//                    threadPool.execute(new Runnable() { public void run() {
                        HttpGet threadTopicRequest = null;
                        String nextUrl = topicUrlFinal;
                        int page = 0;

                        Logger.logI("Scraping " + topicNameFinal);

                        // Loop through each results page and fill the synchronized set with review entry objects
                        while(nextUrl != null) {
                            page++;
                            Logger.logI("Page " + page);
                            threadTopicRequest = new HttpGet(nextUrl);
                            threadTopicRequest.addHeader("User-Agent", USER_AGENT);
                            threadTopicRequest.setConfig(httpGetConfig);
                            // Execute the request using the http client
                            CloseableHttpResponse threadResponse = null;
                            try {
                                threadResponse = httpClient.execute(threadTopicRequest);
                                int statusCodeThread = threadResponse.getStatusLine().getStatusCode();
                                if(statusCodeThread != 200)
                                    throw new RuntimeException("Http connection not ok in thread " + topicNameFinal + ": " + statusCodeThread);
                                //Logger.logD("Status code (" + topicNameFinal + "): " + statusCodeThread);
                                HttpEntity ent = threadResponse.getEntity();
                                String responseStr = EntityUtils.toString(ent, "UTF-8");

                                // Scrape the review entries for titles and urls
                                Document parsedReviewResults = Jsoup.parse(responseStr, "UTF-8");
                                Elements reviewLinks = parsedReviewResults.select("div.search-results-item-body > h3 > a");
                                String[] reviewTitles = reviewLinks.eachText().toArray(new String[0]);
                                String[] reviewUrls = reviewLinks.eachAttr("href").toArray(new String[0]);

                                // Scrape the Authors
                                Elements authors = parsedReviewResults.select("div.search-result-authors");
                                String[] reviewAuthors = authors.eachText().toArray(new String[0]);

                                // Scrape the Date
                                Elements publicationDates = parsedReviewResults.select("div.search-result-date");
                                String[] reviewDates = publicationDates.eachText().toArray(new String[0]);

                                // Replace the bad hyphen character (code 8208) in the titles and authors
                                for(int i = 0; i < reviewTitles.length; i++) {
                                    reviewTitles[i] = reviewTitles[i].replace(Character.toChars(8208)[0],'-');
                                    reviewAuthors[i] = reviewAuthors[i].replace(Character.toChars(8208)[0],'-');
                                }

                                // Check if the info arrays are aligned (in case of UI change) or weren't scraped
                                if(reviewTitles.length == 0 || !utilIntComp(new int[]{reviewTitles.length, reviewUrls.length, reviewAuthors.length, reviewDates.length}))
                                    throw new RuntimeException("Review Titles and URLs not aligned or scraped in topic "
                                        + topicNameFinal + "\n"
                                        + " at URL: " + nextUrl + "\n"
                                        + " at Page: " + page + "\n"
                                        + "Review Titles: " + reviewTitles.length + "\n"
                                        + "Review URLs: " + reviewUrls.length + "\n"
                                        + "Review Authors: " + reviewAuthors.length + "\n"
                                        + "Review Dates: " + reviewDates.length);

                                // Create Review objects using the scraped info and add them to the data structure
                                Review review_temp = null;
                                final String URL_APPEND = "https://www.cochranelibrary.com";
                                for(int i = 0; i < reviewTitles.length; i++) {
                                    review_temp = new Review(URL_APPEND + reviewUrls[i],
                                                             topicNameFinal,
                                                             reviewTitles[i],
                                                             reviewAuthors[i],
                                                             reviewDates[i]);
                                    reviewSyncSet.add(review_temp);
                                }

                                numReviewsScraped += reviewTitles.length;

                                // Find the url for the next page of review entries (if there is one)
                                nextUrl = null;
                                Elements nextUrlElements = parsedReviewResults.select("div.pagination-next-link > a");
                                if(!nextUrlElements.isEmpty()) {
                                    nextUrl = nextUrlElements.eachAttr("href").toArray(new String[0])[0];
                                }

                            } catch (RuntimeException e) {
                                Logger.logE("", e);
                            } catch(IOException e) {
                                Logger.logE("Failed to execute response in thread " + topicNameFinal, e);
                            } finally {
                                // Close the HTTP response
                                if(threadResponse != null) {
                                    try {
                                        threadResponse.close();
                                    } catch(IOException e) {
                                        Logger.logE("Failed to close HTTP Response in thread " + topicNameFinal, e);
                                    }
                                }
                            } // Thread HTTP Response try/catch/finally
                        } // Review results pages while loop
//                    }}); // Runnable/Run()
                } // Thread Creation Loop
//                threadPool.shutdown();
//                threadPool.awaitTermination(1000, TimeUnit.SECONDS);
//                Logger.logI("Thread pool shut down.");
            } catch(RuntimeException e) {
                Logger.logE("Bad http connection to topics page", e);
            } finally {
                // Close the HTTP response
                if(response != null) {
                    try {
                        response.close();
                    } catch(IOException e) {
                        Logger.logE("Failed to close HTTP Response in topic page", e);
                    }
                }
            } // HTTP Response try/catch/finally
        } catch(UnknownHostException e) {
            Logger.logE("Unable to connect to Cochrane Library, Internet may be disconnected", e);
        } finally {
            // Close the HTTP client
            Logger.logI("Closing HTTP client...");
            try {
                httpClient.close();
            } catch(IOException e) {
                Logger.logE("Failed to close HTTP Client", e);
            }

            // Print number of reviews number of reviews scraped
            Logger.logI("Scraped " + numReviewsScraped + " Reviews");

            // Return output
            return toOutputString(reviewSyncSet);

        } // Main try/catch/finally GET request
    } // Scrape()

    // Output a string containing the formatted information of all review entries in the sync set
    private static String toOutputString(Set<Review> set) {
        StringBuilder formattedString = new StringBuilder();
        Review[] reviews = set.toArray(new Review[0]);
        for(int i = 0; i < reviews.length-1; i++) {
            formattedString.append(reviews[i].toString());
            formattedString.append("\n\n");
        }
        formattedString.append(reviews[reviews.length-1].toString());
        return formattedString.toString();
    }

    // Return true if an array of ints are all equal
    private static boolean utilIntComp(int[] nums) {
        if(nums == null || nums.length <= 0) return false;
        int first = nums[0];
        for(int i = 1; i < nums.length; i++)
            if(first != nums[i]) return false;
        return true;
    }

} // End Class CochraneLibraryScraper
