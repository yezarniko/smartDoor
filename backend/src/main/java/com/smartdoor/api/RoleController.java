package com.smartdoor.api;

import com.smartdoor.api.dto.ApiDtos.RoleRequest;
import com.smartdoor.api.dto.ApiDtos.RoleResponse;
import com.smartdoor.api.dto.ApiDtos.RoleUpdateRequest;
import com.smartdoor.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService service;

    public RoleController(RoleService service) { this.service = service; }

    @GetMapping public List<RoleResponse> list() { return service.list(); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse create(@Valid @RequestBody RoleRequest request) { return service.create(request); }

    @PatchMapping("/{code}")
    public RoleResponse update(@PathVariable String code, @Valid @RequestBody RoleUpdateRequest request) {
        return service.update(code, request);
    }

    @DeleteMapping("/{code}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String code) { service.delete(code); }
}
