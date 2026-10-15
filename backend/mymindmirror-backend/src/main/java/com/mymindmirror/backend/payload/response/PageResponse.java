// src/main/java/com/mymindmirror/backend/payload/response/PageResponse.java
package com.mymindmirror.backend.payload.response;
import lombok.Builder;
import java.util.List;

@Builder
public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {}