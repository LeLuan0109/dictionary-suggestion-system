package org.example.dictionarysuggestionsystem.trie;

import org.example.dictionarysuggestionsystem.utils.NormalizerUtil;

import java.util.*;

public class Trie {
    private final TrieNode root = new TrieNode();
    private final Map<String, String> normalizedToOriginal = new HashMap<>();

    public void insert(String word) {
        if (word == null || word.isEmpty()) return;
        String norm = NormalizerUtil.normalize(word);
        normalizedToOriginal.putIfAbsent(norm, word);
        TrieNode node = root;
        for (char c : norm.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isWord = true;
    }

    public boolean search(String word) {
        if (word == null || word.isEmpty()) return false;
        String norm = NormalizerUtil.normalize(word);
        TrieNode node = root;
        for (char c : norm.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return false;
        }
        return node.isWord;
    }

    public List<String> prefixSuggest(String prefix, int limit, boolean bfs) {
        List<String> results = new ArrayList<>();
        if (prefix == null) return results;
        String normPrefix = NormalizerUtil.normalize(prefix);
        TrieNode node = root;
        for (char c : normPrefix.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return results;
        }
        List<String> normalized = new ArrayList<>();
        if (bfs) bfsCollect(normPrefix, node, limit, normalized);
        else dfsCollect(normPrefix, node, limit, normalized);
        for (String n : normalized) {
            String orig = normalizedToOriginal.getOrDefault(n, n);
            if (!results.contains(orig)) results.add(orig);
        }
        return results;
    }

    private void bfsCollect(String prefix, TrieNode start, int limit, List<String> out) {
        Deque<Pair> q = new ArrayDeque<>();
        q.add(new Pair(prefix, start));
        while (!q.isEmpty() && out.size() < limit) {
            Pair p = q.poll();
            if (p.node.isWord) out.add(p.word);
            for (Map.Entry<Character, TrieNode> e : p.node.children.entrySet()) {
                q.add(new Pair(p.word + e.getKey(), e.getValue()));
            }
        }
    }

    private void dfsCollect(String prefix, TrieNode start, int limit, List<String> out) {
        Deque<Pair> st = new ArrayDeque<>();
        st.push(new Pair(prefix, start));
        while (!st.isEmpty() && out.size() < limit) {
            Pair p = st.pop();
            if (p.node.isWord) out.add(p.word);
            for (Map.Entry<Character, TrieNode> e : p.node.children.entrySet()) {
                st.push(new Pair(p.word + e.getKey(), e.getValue()));
            }
        }
    }

    private static class Pair {
        final String word;
        final TrieNode node;
        Pair(String word, TrieNode node) { this.word = word; this.node = node; }
    }
}


