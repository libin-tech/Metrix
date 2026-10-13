package com.bintech.metrix.core.analysis;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatAnswerStreamTest {
    @Test
    void hidesReasoningAcrossEveryPossibleTokenBoundary() {
        String response = "<think>私密内容## 结论摘要不能展示</think>【思考过程】其他私密内容\n## 结论摘要\n公开报告<think>隐藏推理</think>\n## 风险提示\n风险";
        String expected = "## 结论摘要\n公开报告\n## 风险提示\n风险";
        for (int size = 1; size <= response.length(); size++) {
            ChatAnswerStream stream = new ChatAnswerStream();
            StringBuilder emitted = new StringBuilder();
            for (int i = 0; i < response.length(); i += size) emitted.append(stream.append(response.substring(i, Math.min(i + size, response.length()))));
            assertEquals(expected, emitted.toString(), "chunk size " + size);
            assertEquals(expected, stream.content());
        }
    }
    @Test
    void neverPublishesAnUnstructuredModelReply() {
        ChatAnswerStream stream = new ChatAnswerStream();
        assertEquals("", stream.append("【思考过程】私密推理"));
        assertEquals("", stream.content());
    }
}
