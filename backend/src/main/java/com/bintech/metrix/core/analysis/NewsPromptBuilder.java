package com.bintech.metrix.core.analysis;

import com.bintech.metrix.constants.BusinessConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class NewsPromptBuilder {

    public String buildSummarizePrompt(List<Map<String, Object>> newsList) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : newsList) {
            sb.append("- ").append(item.get("title")).append(": ").append(item.get("summary")).append("\n");
        }
        return String.format(BusinessConstants.SUMMARIZE_PROMPT, sb.toString());
    }
}
