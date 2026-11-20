package com.example.api_booking.entity;

import jakarta.persistence.*;

@Entity @Table(name = "locations")
public class Location {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 120) private String name;
    @Column(length = 500) private String description;
    protected Location() {}
    public Location(String name, String description) { this.name = name; this.description = description; }
    public Long getId() { return id; } public String getName() { return name; } public String getDescription() { return description; }
}
