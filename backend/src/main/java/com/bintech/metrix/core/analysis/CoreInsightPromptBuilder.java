package com.bintech.metrix.core.analysis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CoreInsightPromptBuilder {

    public String buildCoreInsightPrompt(String analysisResult) {
        return "请将以下股票分析报告总结为400字以内的核心洞察，包括基本面，技术面，情绪面，新闻舆情还有操作建议。帮助投资者快速了解该股票的核心要点和操作。"
                + "另外，请在核心洞察之后单独列出该股票最相关的3个关联板块（不受字数限制），并标记是否核心热门板块。以表格形式呈现。"
                + "最后以Markdown格式回复，适当增加一点符号，看起来更美观和直观一些。直接回复内容即可不需要介绍和标题。"
                + "\n参考完整分析报告：\n" + analysisResult;
    }
}
