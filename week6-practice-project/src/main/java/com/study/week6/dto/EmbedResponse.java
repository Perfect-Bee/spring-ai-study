package com.study.week6.dto;

import java.util.List;

public record EmbedResponse(
        String model,
        int dimensions,
        int vectorLength,
        List<Float> vectorSample
) {}
