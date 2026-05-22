package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("chat_session")
public class ChatSession extends BaseEntity {

    @TableField(value = "session_name")
    private String sessionName;

    @TableField(value = "user_id")
    private Long userId;

    @TableField(value = "total_tokens")
    private Integer totalTokens;

    @TableField(value = "message_count")
    private Integer messageCount;

}
