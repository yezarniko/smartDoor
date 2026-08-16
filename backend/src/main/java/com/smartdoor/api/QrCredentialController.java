package com.smartdoor.api;

import com.smartdoor.api.dto.ApiDtos.CredentialRequest;
import com.smartdoor.api.dto.ApiDtos.CredentialResponse;
import com.smartdoor.service.QrCredentialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class QrCredentialController {
    private final QrCredentialService service;
    public QrCredentialController(QrCredentialService service) { this.service = service; }

    @GetMapping("/api/users/{userId}/qr-credentials")
    public List<CredentialResponse> list(@PathVariable String userId) { return service.list(userId); }

    @PostMapping("/api/users/{userId}/qr-credentials")
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialResponse create(@PathVariable String userId, @Valid @RequestBody CredentialRequest request) {
        return service.create(userId, request);
    }

    @PostMapping("/api/qr-credentials/{id}/revoke")
    public CredentialResponse revoke(@PathVariable String id) { return service.revoke(id); }

    @PostMapping("/api/qr-credentials/{id}/regenerate")
    public CredentialResponse regenerate(@PathVariable String id, @Valid @RequestBody CredentialRequest request) {
        return service.regenerate(id, request);
    }
}
