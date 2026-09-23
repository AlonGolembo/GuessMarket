package com.guessmarket.client.http;

import java.util.List;

/** Response bodies {@link HttpMarketEngine} reads back that don't already have a DTO. */
final class WireResponses {

    private WireResponses() {}

    record UploadResult(List<String> addedEventNames) {}

    record ErrorResponse(String error) {}
}
