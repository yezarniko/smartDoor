package com.smartdoor.api;

import com.smartdoor.api.dto.ApiDtos.*;
import com.smartdoor.service.AccessService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AccessController {
    private final AccessService service;
    public AccessController(AccessService service) { this.service = service; }

    @PostMapping("/api/access/verify")
    public AccessDecisionResponse verify(@Valid @RequestBody AccessVerifyRequest request) {
        return service.verify(request);
    }

    @GetMapping("/api/access-events")
    public List<AccessEventResponse> events() { return service.listEvents(); }
}

