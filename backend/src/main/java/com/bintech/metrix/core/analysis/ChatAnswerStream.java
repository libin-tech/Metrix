package com.bintech.metrix.core.analysis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 只允许模板的最终报告进入流；跨 token 的 think 标签也不会泄露。 */
public final class ChatAnswerStream {
    private static final Pattern REPORT_START = Pattern.compile(
            "#{1,6}\\s*(?:\\*\\*)?(?:[一二三四五六七八九十\\d]+[、.．）)]\\s*)?结论摘要");
    private static final String THINK_START = "<think>";
    private static final String THINK_END = "</think>";
    private final StringBuilder pending = new StringBuilder();
    private final StringBuilder answer = new StringBuilder();
    private boolean started;
    private boolean hidden;

    public synchronized String append(String chunk) {
        pending.append(chunk);
        StringBuilder delta = new StringBuilder();
        while (!pending.isEmpty()) {
            String text = pending.toString();
            String marker = hidden ? THINK_END : THINK_START;
            if (text.startsWith(marker)) {
                pending.delete(0, marker.length());
                hidden = !hidden;
                continue;
            }
            if (marker.startsWith(text)) break;
            if (!started && !hidden && waitingForHeading(text)) break;
            char current = pending.charAt(0);
            pending.deleteCharAt(0);
            if (started && !hidden) delta.append(current);
        }
        answer.append(delta);
        return delta.toString();
    }

    private boolean waitingForHeading(String text) {
        Matcher heading = REPORT_START.matcher(text);
        started = heading.lookingAt();
        return !started && heading.hitEnd();
    }

    public synchronized String content() { return answer.toString(); }
}
