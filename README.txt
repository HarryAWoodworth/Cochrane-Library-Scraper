TO BUILD:

$ cd cochrane_library_crawler
$ mvn clean package

TO RUN:

$ cd target/
$ java -jar cochrane_library_crawler-1.0-jar-with-dependencies.jar PATH_TO_OUTPUT cochrane_reviews.txt 1

First argument is the path to the output file.
For example my vantage_java_assignment directory was placed directly in my C: drive,
so my PATH_TO_OUTPUT arg on Windows is C:\\vantage_java_assignment\\output\\cochrane_reviews.txt

Second argument is the name of the output file. If the file does not exist it will be
crated. If file does exist it will be overwritten with new scraped data.

Third argument is the number of topics to be scraped. Topics are scraped in alphabetical order. If
0 is the value, all of the topics will be scraped. If the number is higher than the number of topics,
all topics will be scraped.

RESULT:

The formatted string of review entries from the selected topics will be written to the output file.
