package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 对话会话实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("chat_session")
public class ChatSession extends BaseEntity {

    /** 会话名称 */
    @TableField(value = "session_name")
    private String sessionName;

    /** 用户ID */
    @TableField(value = "user_id")
    private Long userId;

    /** 总消耗Token数 */
    @TableField(value = "total_tokens")
    private Integer totalTokens;

    /** 消息数量 */
    @TableField(value = "message_count")
    private Integer messageCount;

}
