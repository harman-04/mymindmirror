// src/main/java/com/mymindmirror.backend/payload/request/TaskForInsightRequest.java
package com.mymindmirror.backend.payload.request;
import com.mymindmirror.backend.enums.Status;
import java.time.LocalDate;

public record TaskForInsightRequest(String description, LocalDate dueDate, Status status) {}
