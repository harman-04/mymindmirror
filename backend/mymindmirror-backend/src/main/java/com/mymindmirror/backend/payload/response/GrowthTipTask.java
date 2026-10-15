package com.mymindmirror.backend.payload.response;

import java.util.List;

public record GrowthTipTask(String title, String description, List<String> subtasks) {}
