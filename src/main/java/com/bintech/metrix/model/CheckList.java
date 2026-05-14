package com.bintech.metrix.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 检查清单模块
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckList {

    /**
     * 检查项列表
     */
    private List<CheckItem> items;

    /**
     * 总体评估结果
     */
    private String overallResult;

    /**
     * 检查项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckItem {

        /**
         * 检查项序号
         */
        private Integer index;

        /**
         * 检查项标题
         */
        private String title;

        /**
         * 检查内容描述
         */
        private String description;

        /**
         * 检查结果：PASS-通过，FAIL-失败，WARNING-警告
         */
        private ResultType result;

        /**
         * 结果标识（✅/❌/⚠️）
         */
        private String resultIcon;

        /**
         * 结果类型枚举
         */
        public enum ResultType {
            PASS,    // ✅ 检查通过
            FAIL,    // ❌ 检查失败
            WARNING  // ⚠️ 警告
        }
    }
}