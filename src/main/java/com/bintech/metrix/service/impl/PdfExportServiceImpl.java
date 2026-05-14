package com.bintech.metrix.service.impl;

import com.bintech.metrix.service.PdfExportService;
import com.bintech.metrix.repository.entity.StockAnalysisRecord;
import com.bintech.metrix.repository.entity.StockBasic;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PdfExportServiceImpl implements PdfExportService {

    @Override
    public byte[] generatePdf(StockAnalysisRecord record, StockBasic stockBasic) {
        String markdown = buildMarkdown(record);
        String html = markdownToHtml(markdown);
        String fullHtml = wrapHtml(html);

        Path tempHtml = null;
        Path tempPdf = null;
        try {
            tempHtml = Files.createTempFile("stock-report-", ".html");
            tempPdf = Files.createTempFile("stock-report-", ".pdf");

            Files.writeString(tempHtml, fullHtml, StandardCharsets.UTF_8);

            String chromePath = findChrome();
            if (chromePath == null) {
                throw new RuntimeException("未找到 Chrome 浏览器，请安装 Chrome 后重试");
            }

            ProcessBuilder pb = new ProcessBuilder(
                    chromePath,
                    "--headless=new",
                    "--disable-gpu",
                    "--no-pdf-header-footer",
                    "--print-to-pdf=" + tempPdf.toAbsolutePath(),
                    tempHtml.toAbsolutePath().toUri().toString()
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Chrome 渲染超时");
            }
            if (process.exitValue() != 0) {
                String error = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new RuntimeException("Chrome 渲染失败: " + error);
            }

            byte[] pdfBytes = Files.readAllBytes(tempPdf);
            log.info("PDF生成成功，大小: {} bytes", pdfBytes.length);
            return pdfBytes;
        } catch (Exception e) {
            log.error("PDF生成失败", e);
            throw new RuntimeException("PDF生成失败: " + e.getMessage());
        } finally {
            try { if (tempHtml != null) Files.deleteIfExists(tempHtml); } catch (Exception ignored) {}
            try { if (tempPdf != null) Files.deleteIfExists(tempPdf); } catch (Exception ignored) {}
        }
    }

    private String markdownToHtml(String markdown) {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create(),
                AnchorLinkExtension.create(),
                TaskListExtension.create(),
                TocExtension.create()
        ));
        options.set(HtmlRenderer.SOFT_BREAK, "\n");
        options.set(HtmlRenderer.HARD_BREAK, "<br/>\n");
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        return renderer.render(parser.parse(markdown));
    }

    private String wrapHtml(String bodyHtml) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                <meta charset="UTF-8"/>
                <style>
                    @page {
                        size: A4;
                        margin: 2cm;
                    }
                    * {
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans SC', 'Microsoft YaHei', 'PingFang SC', Helvetica, Arial, sans-serif;
                        font-size: 11pt;
                        line-height: 1.7;
                        color: #24292e;
                        max-width: 210mm;
                        padding: 0;
                        margin: 0 auto;
                        word-wrap: break-word;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        margin-top: 24px;
                        margin-bottom: 12px;
                        font-weight: 600;
                        line-height: 1.25;
                        color: #1a1a1a;
                    }
                    h1 { font-size: 20pt; border-bottom: 1px solid #eaecef; padding-bottom: 8px; }
                    h2 { font-size: 16pt; border-bottom: 1px solid #eaecef; padding-bottom: 6px; }
                    h3 { font-size: 14pt; }
                    h4 { font-size: 12pt; }
                    h5 { font-size: 11pt; }
                    h6 { font-size: 10pt; color: #6a737d; }
                    p {
                        margin-top: 0;
                        margin-bottom: 10px;
                    }
                    blockquote {
                        padding: 0 1em;
                        margin: 0 0 16px;
                        border-left: 4px solid #d0d7de;
                        color: #57606a;
                    }
                    blockquote p {
                        margin-bottom: 0;
                    }
                    table {
                        display: block;
                        width: 100%;
                        max-width: 100%;
                        overflow: auto;
                        border-collapse: collapse;
                        margin: 12px 0;
                        font-size: 10pt;
                    }
                    th, td {
                        border: 1px solid #d0d7de;
                        padding: 6px 13px;
                        text-align: left;
                    }
                    th {
                        background-color: #f6f8fa;
                        font-weight: 600;
                    }
                    tr:nth-child(even) {
                        background-color: #fafbfc;
                    }
                    code {
                        padding: 2px 6px;
                        font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
                        font-size: 85%;
                        background-color: rgba(27,31,35,0.07);
                        border-radius: 3px;
                        color: #24292e;
                    }
                    pre {
                        background-color: #f6f8fa;
                        border: 1px solid #e1e4e8;
                        border-radius: 4px;
                        padding: 12px 16px;
                        overflow: auto;
                        margin: 0 0 16px;
                        font-size: 10pt;
                        line-height: 1.45;
                        page-break-inside: avoid;
                    }
                    pre code {
                        padding: 0;
                        background: none;
                        font-size: inherit;
                        color: inherit;
                        word-wrap: normal;
                    }
                    ul, ol {
                        padding-left: 24px;
                        margin: 0 0 12px;
                    }
                    li {
                        margin: 4px 0;
                    }
                    li > p {
                        margin-bottom: 0;
                    }
                    hr {
                        height: 1px;
                        padding: 0;
                        margin: 24px 0;
                        background-color: #e1e4e8;
                        border: 0;
                    }
                    a {
                        color: #0969da;
                        text-decoration: none;
                    }
                    a:hover {
                        text-decoration: underline;
                    }
                    img {
                        max-width: 100%;
                        height: auto;
                    }
                    input[type="checkbox"] {
                        margin-right: 6px;
                    }
                </style>
                </head>
                <body>
                """ +
                bodyHtml +
                """
                </body>
                </html>
                """;
    }

    private String buildMarkdown(StockAnalysisRecord record) {
        String analysisResult = record.getAnalysisResult();
        if (analysisResult != null && !analysisResult.isEmpty()) {
            return analysisResult;
        }
        return "暂无分析内容";
    }

    private static String findChrome() {
        String[] candidates = {
            System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe",
            System.getenv("ProgramFiles") + "\\Google\\Chrome\\Application\\chrome.exe",
            System.getenv("ProgramW6432") + "\\Google\\Chrome\\Application\\chrome.exe",
            System.getenv("PROGRAMFILES(X86)") + "\\Google\\Chrome\\Application\\chrome.exe",
            "/usr/bin/google-chrome",
            "/usr/bin/chromium-browser",
            "/usr/bin/chromium",
            "/snap/bin/chromium",
        };
        for (String path : candidates) {
            if (path == null) continue;
            File f = new File(path);
            if (f.exists()) {
                log.info("使用 Chrome: {}", path);
                return f.getAbsolutePath();
            }
        }
        return null;
    }
}
