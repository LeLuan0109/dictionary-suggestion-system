package org.example.dictionarysuggestionsystem.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class DictionaryEntry {
    private final String word;
    private final String meaning;
    private final long frequency;
    private final List<String> tags;

    @JsonCreator
    public DictionaryEntry(
            @JsonProperty("word") String word,
            @JsonProperty("meaning") String meaning,
            @JsonProperty("frequency") long frequency,
            @JsonProperty("tags") List<String> tags) {
        this.word = word;
        this.meaning = meaning;
        this.frequency = frequency;
        this.tags = tags;
    }

    public String getWord() { return word; }
    public String getMeaning() { return meaning; }
    public long getFrequency() { return frequency; }
    public List<String> getTags() { return tags; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DictionaryEntry that = (DictionaryEntry) o;
        return Objects.equals(word, that.word);
    }

    @Override
    public int hashCode() {
        return Objects.hash(word);
    }
}


