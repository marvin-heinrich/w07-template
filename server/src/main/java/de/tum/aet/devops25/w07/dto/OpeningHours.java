package de.tum.aet.devops25.w07.dto;

public record OpeningHours(
    String monday,
    String tuesday,
    String wednesday,
    String thursday,
    String friday,
    String saturday,
    String sunday
) {}