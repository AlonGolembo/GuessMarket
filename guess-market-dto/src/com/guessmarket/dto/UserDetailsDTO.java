package com.guessmarket.dto;

import java.util.List;

/**
 * Everything the UI shows for one user: their summary {@link UserDTO} plus their
 * full cash-balance history, oldest entry first.
 *
 * <p>Mirrors {@link EventDetailsDTO} - a details DTO that composes its own summary
 * DTO rather than duplicating its fields.
 */
public record UserDetailsDTO(
        UserDTO userInfo,
        List<LedgerEntryDTO> balanceHistory
) {}
