package com.smartdoor.api;

import com.smartdoor.api.dto.ApiDtos.DoorResponse;
import com.smartdoor.repository.DoorRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doors")
public class DoorController {
    private final DoorRepository doors;
    public DoorController(DoorRepository doors) { this.doors = doors; }

    @GetMapping
    public List<DoorResponse> list() {
        return doors.findAll().stream().map(door -> new DoorResponse(
                door.getId(), door.getPublicId(), door.getName(), door.getStatus())).toList();
    }
}
