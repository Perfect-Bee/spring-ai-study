package com.study.bookadvisor.dto;

public record BookCreateRequest(
        String title,
        String author,
        String genre,
        String description,
        Double rating,
        Integer publishedYear,
        String isbn
) {}
