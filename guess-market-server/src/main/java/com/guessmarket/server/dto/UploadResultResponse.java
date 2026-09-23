package com.guessmarket.server.dto;

import java.util.List;

/** POST /api/events/upload response: the events the upload actually added. */
public record UploadResultResponse(List<String> addedEventNames) {}
