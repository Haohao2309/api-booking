package com.example.api_booking.controller;

import com.example.api_booking.dto.RoomRequest;
import com.example.api_booking.entity.*;
import com.example.api_booking.repository.*;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class RoomController {
    private final RoomRepository rooms; private final LocationRepository locations;
    public RoomController(RoomRepository rooms, LocationRepository locations){this.rooms=rooms;this.locations=locations;}
    @GetMapping("/locations") public Map<String,Object> locations(){return Map.of("items", locations.findAll());}
    @GetMapping("/rooms") public Map<String,Object> rooms(@RequestParam(defaultValue="false") boolean includeInactive,@RequestParam(required=false) Long locationId){var list = includeInactive ? rooms.findAll() : (locationId == null ? rooms.findByActiveTrueOrderByIdDesc() : rooms.findByActiveTrueAndLocationIdOrderByIdDesc(locationId)); return Map.of("items", list.stream().map(this::room).toList());}
    @PostMapping("/rooms") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(@Valid @RequestBody RoomRequest r){return Map.of("data", room(rooms.save(new Room(r.number().trim(),r.name().trim(),r.type().trim(),r.description(),r.capacity(),r.pricePerNight(),r.active(),location(r.locationId())))));}
    @PatchMapping("/rooms/{id}") public Map<String,Object> update(@PathVariable Long id,@Valid @RequestBody RoomRequest r){Room room=rooms.findById(id).orElseThrow();room.update(r.number().trim(),r.name().trim(),r.type().trim(),r.description(),r.capacity(),r.pricePerNight(),r.active(),location(r.locationId()));return Map.of("data",this.room(rooms.save(room)));}
    private Location location(Long id){return id == null ? locations.findAll().stream().findFirst().orElseThrow() : locations.findById(id).orElseThrow();}
    private Map<String,Object> room(Room r){return Map.of("id",r.getId(),"number",r.getNumber(),"name",r.getName(),"type",r.getType(),"description",r.getDescription()==null?"":r.getDescription(),"capacity",r.getCapacity(),"pricePerNight",r.getPricePerNight(),"active",r.isActive(),"location",r.getLocation().getName(),"available",r.isActive());}
}
