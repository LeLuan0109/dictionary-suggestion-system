package org.example.dictionarysuggestionsystem.algorithms;

import org.example.dictionarysuggestionsystem.model.DictionaryEntry;
import org.example.dictionarysuggestionsystem.utils.NormalizerUtil;

import java.util.*;
import java.util.stream.Collectors;

public class TfidfRanker {
    public List<DictionaryEntry> rankByQuery(String query, List<DictionaryEntry> docs, int limit) {
        if (query == null || query.isBlank()) return Collections.emptyList();
        String[] terms = tokenize(query);
        Map<String, Integer> df = computeDocumentFrequency(terms, docs);
        int N = docs.size();
        List<ScoredEntry> scored = new ArrayList<>();
        for (DictionaryEntry e : docs) {
            double score = 0.0;
            Map<String, Integer> tf = termFreq(terms, e);
            for (String t : terms) {
                int tfVal = tf.getOrDefault(t, 0);
                int dfVal = Math.max(1, df.getOrDefault(t, 0));
                double idf = Math.log((N + 1.0) / (dfVal + 1.0)) + 1.0;
                score += tfVal * idf;
            }
            scored.add(new ScoredEntry(e, score));
        }
        return scored.stream()
            .filter(s -> s.score > 0.0)
            .sorted(Comparator.comparingDouble((ScoredEntry s) -> s.score).reversed())
            .limit(limit)
            .map(s -> s.entry)
            .collect(Collectors.toList());
    }

    private Map<String, Integer> computeDocumentFrequency(String[] terms, List<DictionaryEntry> docs) {
        Map<String, Integer> df = new HashMap<>();
        for (DictionaryEntry e : docs) {
            Set<String> seen = new HashSet<>();
            for (String t : terms) {
                if (containsToken(e, t) && seen.add(t)) {
                    df.merge(t, 1, Integer::sum);
                }
            }
        }
        return df;
    }

    private Map<String, Integer> termFreq(String[] terms, DictionaryEntry e) {
        Map<String, Integer> tf = new HashMap<>();
        for (String t : terms) {
            int count = countToken(e, t);
            if (count > 0) tf.put(t, count);
        }
        return tf;
    }

    private boolean containsToken(DictionaryEntry e, String t) {
        return countToken(e, t) > 0;
    }

    private int countToken(DictionaryEntry e, String t) {
        int c = 0;
        c += countOccurrences(NormalizerUtil.normalize(e.getWord()), t);
        c += countOccurrences(NormalizerUtil.normalize(e.getMeaning()), t);
        if (e.getTags() != null) {
            for (String tag : e.getTags()) {
                c += countOccurrences(NormalizerUtil.normalize(tag), t);
            }
        }
        return c;
    }

    private int countOccurrences(String text, String token) {
        if (text == null || token == null || token.isEmpty()) return 0;
        String s = NormalizerUtil.normalize(text);
        String tok = NormalizerUtil.normalize(token);
        int idx = 0, c = 0;
        while ((idx = s.indexOf(tok, idx)) != -1) {
            c++; idx += tok.length();
        }
        return c;
    }

    private String[] tokenize(String q) {
        return Arrays.stream(q.toLowerCase().split("[^a-zA-Z0-9]+"))
            .filter(s -> !s.isBlank())
            .toArray(String[]::new);
    }

    private static class ScoredEntry {
        final DictionaryEntry entry; final double score;
        ScoredEntry(DictionaryEntry entry, double score) { this.entry = entry; this.score = score; }
    }
}


