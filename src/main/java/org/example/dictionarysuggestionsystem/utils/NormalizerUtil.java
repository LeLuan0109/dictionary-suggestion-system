package org.example.dictionarysuggestionsystem.utils;

import java.text.Normalizer;

public final class NormalizerUtil {
  private NormalizerUtil() {}

  public static String normalize(String input) {
    if (input == null) return "";
    String lower = input.toLowerCase();
    String norm = Normalizer.normalize(lower, Normalizer.Form.NFD);
    return norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
  }
}

