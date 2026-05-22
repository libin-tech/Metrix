package com.bintech.metrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageResult<T> {

    private List<T> items;
    private boolean hasMore;
    private Long nextCursor;

}
