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
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    @TableField(value = "session_id")
    private Long sessionId;

    @TableField(value = "role")
    private String role;

    @TableField(value = "content")
    private String content;

    @TableField(value = "tokens")
    private Integer tokens;

    @TableField(value = "stock_code")
    private String stockCode;

    @TableField(value = "stock_name")
    private String stockName;

}
