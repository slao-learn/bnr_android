package com.bignerdranch.android.photogallery;

/**
 * Created by slao on 1/21/16.
 */
public class GalleryItem {
    private String title;
    private String id;
    private String url_s;

    GalleryItem() {
    }

    @Override
    public String toString() {
        return title;
    }

    public String getCaption() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url_s;
    }
}
