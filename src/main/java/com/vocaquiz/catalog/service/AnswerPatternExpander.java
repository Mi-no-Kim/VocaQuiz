package com.vocaquiz.catalog.service;

import com.vocaquiz.common.error.ApiException;
import com.vocaquiz.common.error.ErrorCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 정답 패턴 원문을 후보 문자열로 전개한다 (D-052, D-056).
 *
 * <p>문법은 괄호와 파이프뿐이고 괄호는 중첩할 수 없다. 유한한 문자열 집합을 적는 언어라
 * 중첩은 표현력을 늘리지 못한다 — {@code (F|G)H}는 {@code FH|GH}로 쓴다.
 *
 * <p>중복 제거와 정규화는 하지 않는다. 호출하는 서비스가 한다.
 */
public class AnswerPatternExpander {

    /**
     * 정답 패턴 하나가 만들 수 있는 결과의 최대 개수. {@link #expand}가 이 개수를 넘기면
     * 던진다 — 20개(관리자 화면의 {@code WARNING_THRESHOLD})는 경고일 뿐이지만 이건 상한이다.
     */
    private static final int MAX_RESULTS = 400;

    private AnswerPatternExpander() {}

    /**
     * 여러 줄 원문을 전개한다. 한 줄이라도 문법이 틀리면 전체가 실패한다.
     *
     * <p>결과가 {@value #MAX_RESULTS}개를 넘으면 던진다. 정답 패턴 검사(check)도 실제
     * 저장({@link SongCatalogService#replaceAnswerPattern})도 이 메서드를 거치므로
     * 상한을 여기 한 곳에만 둔다.
     */
    public static List<String> expand(String pattern) {
        List<String> out = new ArrayList<>();
        String[] lines = split(pattern);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty()) continue;
            List<List<String>> slots = parse(line, i + 1);
            if (out.size() + count(slots) > MAX_RESULTS) {
                throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "정답 패턴 결과가 %d개를 넘을 수 없다 (지금까지 %d개)"
                        .formatted(MAX_RESULTS, out.size() + count(slots)));
            }
            out.addAll(combine(slots));
        }
        return out;
    }

    /** 전개하지 않고 개수만 센다. 관리자 화면의 경고 판정용이다. */
    public static long countOf(String pattern) {
        long total = 0;
        String[] lines = split(pattern);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty()) continue;
            total += count(parse(line, i + 1));
        }
        return total;
    }

    private static String[] split(String pattern) {
        return pattern == null ? new String[0] : pattern.split("\\R", -1);
    }

    /**
     * 한 줄을 '자리'의 목록으로 쪼갠다.
     * 리터럴 구간은 후보가 하나인 자리이고, 괄호는 후보가 여럿인 자리다.
     */
    private static List<List<String>> parse(String line, int lineNo) {
        List<List<String>> slots = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        List<String> alternatives = null;   // null이면 괄호 밖
        int groupStart = -1;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\\') {
                if (i + 1 == line.length()) {
                    throw error(lineNo, i + 1, "줄이 '\\'로 끝났다");
                }
                buf.append(line.charAt(++i));
                continue;
            }

            switch (c) {
                case '(' -> {
                    if (alternatives != null) {
                        throw error(lineNo, i + 1, "괄호를 중첩할 수 없다");
                    }
                    flush(slots, buf);
                    alternatives = new ArrayList<>();
                    groupStart = i + 1;
                }
                case '|' -> {
                    if (alternatives == null) {
                        throw error(lineNo, i + 1, "괄호 밖의 '|'");
                    }
                    alternatives.add(buf.toString());
                    buf.setLength(0);
                }
                case ')' -> {
                    if (alternatives == null) {
                        throw error(lineNo, i + 1, "여는 괄호가 없는 ')'");
                    }
                    alternatives.add(buf.toString());
                    buf.setLength(0);
                    slots.add(List.copyOf(alternatives));
                    alternatives = null;
                }
                default -> buf.append(c);
            }
        }

        if (alternatives != null) {
            throw error(lineNo, groupStart, "닫히지 않은 '('");
        }
        flush(slots, buf);
        return slots;
    }

    private static void flush(List<List<String>> slots, StringBuilder buf) {
        if (buf.isEmpty()) return;
        slots.add(List.of(buf.toString()));
        buf.setLength(0);
    }

    private static long count(List<List<String>> slots) {
        long n = 1;
        for (List<String> slot : slots) {
            n *= slot.size();
        }
        return n;
    }

    private static List<String> combine(List<List<String>> slots) {
        List<String> acc = new ArrayList<>();
        acc.add("");

        for (List<String> slot : slots) {
            List<String> next = new ArrayList<>(acc.size() * slot.size());
            for (String prefix : acc) {
                for (String alternative : slot) {
                    next.add(prefix + alternative);
                }
            }
            acc = next;
        }
        return acc;
    }

    private static ApiException error(int lineNo, int column, String what) {
        return new ApiException(
            ErrorCode.INVALID_REQUEST,
            "정답 패턴 %d번째 줄 %d번째 글자: %s".formatted(lineNo, column, what)
        );
    }
}
