package com.harryawoodworth.vantage_java_assignment.models;

/**
 * Review class represents a review from the Cochrane Library.
 * Each review has a url link to it's page in the library, a
 * topic category, a title, an author name, and a date of publication.
 * Contains equals() and hashCode() overrides for adding to a Set
 * Contains a toString() to format according to the project specifications
 */
public class Review {

    private String url;
    private String topic;
    private String title;
    private String author;
    private String date;

    // Review class. Takes a url, topic, title, author, and publishing date.
    public Review(String url,
                  String topic,
                  String title,
                  String author,
                  String date
    ) {
        this.url = url;
        this.topic = topic;
        this.title = title;
        this.author = author;
        // Convert the date format
        this.date = this.convertDate(date);
    }

    // Convert String date from 'dd Month yyyy' to 'yyyy-mm-dd' format
    private String convertDate(String date) {
        String[] breakdown = date.trim().split(" ");
        String delim = "-";
        String month = "";
        switch(breakdown[1]) {
            case "January": month = "01"; break;
            case "February": month = "02"; break;
            case "March": month = "03"; break;
            case "April": month = "04"; break;
            case "May": month = "05"; break;
            case "June": month = "06"; break;
            case "July": month = "07"; break;
            case "August": month = "08"; break;
            case "September": month = "09"; break;
            case "October": month = "10"; break;
            case "November": month = "11"; break;
            case "December": month = "12"; break;
            default: month = "01";
        }
        return String.join(delim, breakdown[2], month, breakdown[0]);
    }

    // Getters
    public String getUrl() { return this.url; }
    public String getTopic() { return this.topic; }
    public String getTitle() { return this.title; }
    public String getAuthor() { return this.author; }
    public String getDate() { return this.date; }

    // ToString override to format it to project specifications
    @Override public String toString() {
        String delim = "|";
        return String.join(delim, this.url, this.topic, this.title, this.author, this.date);
    }

    // Equals override
    @Override public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Review))
            return false;
        Review other = (Review) obj;
        return other.url.equals(this.url);
    }

    // Hashcode override
    // Override to only use the URL, since HashSet uses hascode for object equality
    @Override public int hashCode() {
       int hash = 13;
       hash = 31 * hash + this.url.hashCode();
       return hash;
   }

}
