package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bintech.metrix.enums.ChatRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 对话消息实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    /** 会话ID */
    @TableField(value = "session_id")
    private Long sessionId;

    /** 角色：user-用户 assistant-AI助手 */
    @TableField(value = "role")
    private ChatRole role;

    /** 消息内容 */
    @TableField(value = "content")
    private String content;

    /** 消耗Token数 */
    @TableField(value = "tokens")
    private Integer tokens;

    /** 关联股票代码 */
    @TableField(value = "stock_code")
    private String stockCode;

    /** 关联股票名称 */
    @TableField(value = "stock_name")
    private String stockName;

    /** AI分析步骤记录（JSON数组） */
    @TableField(value = "steps")
    private String steps;

    /** 用户ID */
    @TableField(value = "user_id")
    private Long userId;

}
