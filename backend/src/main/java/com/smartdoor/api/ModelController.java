package com.smartdoor.api;

import com.smartdoor.api.dto.ApiDtos.ModelInfoResponse;
import com.smartdoor.service.DecisionTreeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/model")
public class ModelController {
    private final DecisionTreeService service;
    public ModelController(DecisionTreeService service) { this.service = service; }
    @GetMapping("/info") public ModelInfoResponse info() { return service.info(); }
}
