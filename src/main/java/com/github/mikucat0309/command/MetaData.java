package com.github.mikucat0309.command;

import java.util.Arrays;
import java.util.Objects;

public class MetaData {

    String id;
    String name = "";
    String version = "";
    String description = "";
    String url = "";
    String[] authors = new String[0];

    public MetaData(String id) {
        this.id = id;
    }

    public MetaData(String id,
            String name,
            String version,
            String description,
            String url,
            String[] authors) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.url = url;
        this.authors = authors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetaData metaData = (MetaData) o;
        return id.equals(metaData.id) &&
                name.equals(metaData.name) &&
                version.equals(metaData.version) &&
                description.equals(metaData.description) &&
                url.equals(metaData.url) &&
                Arrays.equals(authors, metaData.authors);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, name, version, description, url);
        result = 31 * result + Arrays.hashCode(authors);
        return result;
    }
}