package org.example.dictionarysuggestionsystem.trie;

import java.util.HashMap;
import java.util.Map;

class TrieNode {
    final Map<Character, TrieNode> children = new HashMap<>();
    boolean isWord = false;
}


