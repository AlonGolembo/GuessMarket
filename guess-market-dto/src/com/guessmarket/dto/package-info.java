/**
 * Immutable record DTOs exchanged across the {@code guess-market-engine} API
 * boundary. The graph is flat: an {@link com.guessmarket.dto.EventDTO} never
 * references a {@link com.guessmarket.dto.UserDTO} and vice versa — they
 * cross-reference by id and name.
 */
package com.guessmarket.dto;
