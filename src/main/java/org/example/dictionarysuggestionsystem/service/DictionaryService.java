package org.example.dictionarysuggestionsystem.service;

import org.example.dictionarysuggestionsystem.algorithms.Levenshtein;
import org.example.dictionarysuggestionsystem.algorithms.TfidfRanker;
import org.example.dictionarysuggestionsystem.model.DictionaryEntry;
import org.example.dictionarysuggestionsystem.repository.JsonDictionaryRepository;
import org.example.dictionarysuggestionsystem.trie.Trie;
import org.example.dictionarysuggestionsystem.utils.NormalizerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DictionaryService {
    private static final Logger log = LoggerFactory.getLogger(DictionaryService.class);

    private final JsonDictionaryRepository repo;
    private final Trie trie = new Trie();
    private final TfidfRanker tfidf = new TfidfRanker();
    private List<DictionaryEntry> allEntries = new ArrayList<>();

    public DictionaryService(File dataFile) {
        this.repo = new JsonDictionaryRepository(dataFile);
        try {
            ensureSeedIfEmpty(dataFile);
            reload();
        } catch (IOException e) {
            log.error("Failed to load dictionary", e);
        }
    }

    private void ensureSeedIfEmpty(File dataFile) throws IOException {
        boolean needSeed = !dataFile.exists();
        if (!needSeed) {
            List<DictionaryEntry> current = repo.loadAll();
            needSeed = current == null || current.isEmpty();
        }
        if (needSeed) {
            try (InputStream is = DictionaryService.class.getResourceAsStream("/dictionary-seed.json")) {
                if (is == null) {
                    log.warn("Seed file not found in resources: /dictionary-seed.json");
                    return;
                }
                ObjectMapper mapper = new ObjectMapper();
                List<DictionaryEntry> seed = mapper.readValue(is, new TypeReference<List<DictionaryEntry>>(){});
                repo.saveAll(seed);
                log.info("Seeded dictionary with {} entries", seed.size());
            }
        }
    }

    public synchronized void reload() throws IOException {
        this.allEntries = repo.loadAll();
        rebuildTrie();
    }

    private void rebuildTrie() {
        for (DictionaryEntry e : allEntries) {
            trie.insert(e.getWord());
        }
    }

    public static class TimedResult<T> {
        public final T value; public final long micros;
        public TimedResult(T value, long micros) { this.value = value; this.micros = micros; }
    }

    public TimedResult<List<String>> suggestByPrefix(String prefix, int limit, boolean bfs) {
        long t0 = System.nanoTime();
        List<String> res = trie.prefixSuggest(prefix, limit, bfs);
        long t1 = System.nanoTime();
        return new TimedResult<>(res, (t1 - t0) / 1000);
    }

    public TimedResult<List<String>> suggestByLevenshtein(String query, int limit) {
        long t0 = System.nanoTime();
        String nq = NormalizerUtil.normalize(query);
        List<String> words = allEntries.stream().map(DictionaryEntry::getWord).collect(Collectors.toList());
        List<String> sorted = words.stream()
            .sorted(Comparator.comparingInt(w -> Levenshtein.distance(nq, NormalizerUtil.normalize(w))))
            .limit(limit)
            .collect(Collectors.toList());
        long t1 = System.nanoTime();
        return new TimedResult<>(sorted, (t1 - t0) / 1000);
    }

    public TimedResult<List<DictionaryEntry>> suggestByTfidf(String query, int limit) {
        long t0 = System.nanoTime();
        List<DictionaryEntry> ranked = tfidf.rankByQuery(query, allEntries, limit);
        long t1 = System.nanoTime();
        return new TimedResult<>(ranked, (t1 - t0) / 1000);
    }

    public TimedResult<List<String>> suggestByLevenshteinFiltered(String query, int limit, int maxDistance) {
        long t0 = System.nanoTime();
        String nq = NormalizerUtil.normalize(query);
        List<String> words = allEntries.stream().map(DictionaryEntry::getWord).collect(Collectors.toList());
        List<String> filtered = words.stream()
            .map(w -> new AbstractMap.SimpleEntry<>(w, Levenshtein.distance(nq, NormalizerUtil.normalize(w))))
            .filter(e -> e.getValue() <= maxDistance)
            .sorted(Comparator.comparingInt(AbstractMap.SimpleEntry::getValue))
            .limit(limit)
            .map(AbstractMap.SimpleEntry::getKey)
            .collect(Collectors.toList());
        long t1 = System.nanoTime();
        return new TimedResult<>(filtered, (t1 - t0) / 1000);
    }

    public synchronized void add(DictionaryEntry entry) throws IOException {
        allEntries.removeIf(e -> e.getWord().equalsIgnoreCase(entry.getWord()));
        allEntries.add(entry);
        repo.saveAll(allEntries);
        trie.insert(entry.getWord());
    }

    public synchronized void delete(String word) throws IOException {
        allEntries.removeIf(e -> e.getWord().equalsIgnoreCase(word));
        repo.saveAll(allEntries);
        rebuildTrie();
    }

    public synchronized void edit(DictionaryEntry entry) throws IOException {
        delete(entry.getWord());
        add(entry);
    }

    public synchronized void incrementFrequency(String word) throws IOException {
        DictionaryEntry existing = allEntries.stream()
            .filter(e -> e.getWord().equalsIgnoreCase(word))
            .findFirst()
            .orElse(null);
        if (existing != null) {
            DictionaryEntry updated = new DictionaryEntry(
                existing.getWord(),
                existing.getMeaning(),
                existing.getFrequency() + 1,
                existing.getTags()
            );
            allEntries.removeIf(e -> e.getWord().equalsIgnoreCase(word));
            allEntries.add(updated);
            repo.saveAll(allEntries);
        }
    }

    public List<DictionaryEntry> getAllEntries() { return Collections.unmodifiableList(allEntries); }
}


