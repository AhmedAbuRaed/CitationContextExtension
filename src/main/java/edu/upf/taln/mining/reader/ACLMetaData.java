package edu.upf.taln.mining.reader;

import java.util.ArrayList;

/**
 * Created by Ahmed on 2/15/17.
 */
public class ACLMetaData {
    private String id;
    private ArrayList<String> authors;
    private String title;
    private String venue;
    private String year;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<String> getAuthors() {
        return authors;
    }

    public void setAuthors(ArrayList<String> authors) {
        this.authors = authors;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }
}
