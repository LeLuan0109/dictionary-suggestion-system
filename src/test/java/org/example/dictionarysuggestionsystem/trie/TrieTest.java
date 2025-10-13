package org.example.dictionarysuggestionsystem.trie;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrieTest {
    @Test
    void insertSearchPrefix() {
        Trie trie = new Trie();
        trie.insert("apple");
        trie.insert("app");
        trie.insert("ape");
        assertTrue(trie.search("app"));
        assertFalse(trie.search("ap"));
        List<String> s = trie.prefixSuggest("ap", 10, true);
        assertTrue(s.contains("app"));
        assertTrue(s.contains("apple"));
        assertTrue(s.contains("ape"));
    }
}


