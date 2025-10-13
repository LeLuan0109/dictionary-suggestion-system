package org.example.dictionarysuggestionsystem.algorithms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LevenshteinTest {
    @Test
    void distanceBasic() {
        assertEquals(0, Levenshtein.distance("kitten", "kitten"));
        assertEquals(3, Levenshtein.distance("kitten", "sitting"));
        assertEquals(1, Levenshtein.distance("book", "books"));
        assertEquals(2, Levenshtein.distance("intention", "execution"));
    }
}


