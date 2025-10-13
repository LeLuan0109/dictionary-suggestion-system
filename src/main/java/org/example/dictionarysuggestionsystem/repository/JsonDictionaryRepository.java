package org.example.dictionarysuggestionsystem.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dictionarysuggestionsystem.model.DictionaryEntry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JsonDictionaryRepository {
    private final File file;
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonDictionaryRepository(File file) {
        this.file = file;
    }

    public synchronized List<DictionaryEntry> loadAll() throws IOException {
        if (!file.exists()) return new ArrayList<>();
        return mapper.readValue(file, new TypeReference<List<DictionaryEntry>>(){});
    }

    public synchronized void saveAll(List<DictionaryEntry> entries) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, entries);
    }

    public synchronized Optional<DictionaryEntry> findByWord(String word) throws IOException {
        return loadAll().stream().filter(e -> e.getWord().equalsIgnoreCase(word)).findFirst();
    }
}


