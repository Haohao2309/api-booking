package com.example.api_booking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "rooms")
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 40) private String number;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 60) private String type;
    @Column(length = 1000) private String description;
    @Column(nullable = false) private Integer capacity;
    @Column(name = "price_per_night", nullable = false, precision = 12, scale = 2) private BigDecimal pricePerNight;
    @Column(nullable = false) private boolean active = true;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "location_id") private Location location;
    protected Room() {}
    public Room(String number, String name, String type, String description, Integer capacity, BigDecimal pricePerNight, boolean active, Location location) { this.number=number; this.name=name; this.type=type; this.description=description; this.capacity=capacity; this.pricePerNight=pricePerNight; this.active=active; this.location=location; }
    public Long getId(){return id;} public String getNumber(){return number;} public String getName(){return name;} public String getType(){return type;} public String getDescription(){return description;} public Integer getCapacity(){return capacity;} public BigDecimal getPricePerNight(){return pricePerNight;} public boolean isActive(){return active;} public Location getLocation(){return location;}
    public void update(String number,String name,String type,String description,Integer capacity,BigDecimal pricePerNight,boolean active,Location location){this.number=number;this.name=name;this.type=type;this.description=description;this.capacity=capacity;this.pricePerNight=pricePerNight;this.active=active;this.location=location;}
}
