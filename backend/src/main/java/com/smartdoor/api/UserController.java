package com.smartdoor.api;

import com.smartdoor.api.dto.ApiDtos.*;
import com.smartdoor.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) { this.service = service; }

    @GetMapping public List<UserResponse> list() { return service.list(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest request) { return service.create(request); }
    @GetMapping("/{id}") public UserResponse get(@PathVariable String id) { return service.get(id); }
    @PatchMapping("/{id}") public UserResponse update(@PathVariable String id, @Valid @RequestBody UserRequest request) {
        return service.update(id, request);
    }
    @PatchMapping("/{id}/status") public UserResponse status(@PathVariable String id, @Valid @RequestBody StatusRequest request) {
        return service.setStatus(id, request);
    }
    @GetMapping("/{id}/permissions") public List<PermissionResponse> permissions(@PathVariable String id) {
        return service.getPermissions(id);
    }
    @PutMapping("/{id}/permissions") public List<PermissionResponse> permissions(@PathVariable String id,
                                                                                  @Valid @RequestBody PermissionRequest request) {
        return service.replacePermissions(id, request);
    }
    @GetMapping("/{id}/schedules") public List<ScheduleResponse> schedules(@PathVariable String id) {
        return service.getSchedules(id);
    }
    @PutMapping("/{id}/schedules") public List<ScheduleResponse> schedules(@PathVariable String id,
                                                                            @Valid @RequestBody ScheduleRequest request) {
        return service.replaceSchedules(id, request);
    }
}

