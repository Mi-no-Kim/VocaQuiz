package com.vocaquiz.catalog.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class TextNormalizer {

    private static final Pattern REMOVE =
        Pattern.compile("[\\s()\\[\\]【】・ー〜~!?,./-]");

    private TextNormalizer() {}

    public static String normalize(String raw) {
        if (raw == null) return "";

        String nfkc = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        String lower = nfkc.toLowerCase(Locale.ROOT);

        return REMOVE.matcher(lower).replaceAll("");
    }
}
