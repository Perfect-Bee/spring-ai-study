package com.study.bookadvisor.dto;

public record BookResponse(
        Long id,
        String title,
        String author,
        String genre,
        String description,
        Double rating,
        Integer publishedYear,
        String isbn
) {}
