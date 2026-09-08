package com.smartdoor.service;

import com.smartdoor.api.ResourceNotFoundException;
import com.smartdoor.api.dto.ApiDtos.RoleRequest;
import com.smartdoor.api.dto.ApiDtos.RoleResponse;
import com.smartdoor.api.dto.ApiDtos.RoleUpdateRequest;
import com.smartdoor.domain.Role;
import com.smartdoor.repository.RoleRepository;
import com.smartdoor.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {
    private final RoleRepository roles;
    private final UserRepository users;

    public RoleService(RoleRepository roles, UserRepository users) {
        this.roles = roles;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roles.findAllByOrderByNameAsc().stream().map(RoleService::toResponse).toList();
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        String code = Role.normalizeCode(request.code());
        if (roles.existsById(code)) throw new IllegalArgumentException("Role code already exists");
        if (roles.existsByNameIgnoreCase(request.name().trim())) throw new IllegalArgumentException("Role name already exists");
        return toResponse(roles.save(new Role(code, request.name(), request.modelRole())));
    }

    @Transactional
    public RoleResponse update(String code, RoleUpdateRequest request) {
        String normalized = Role.normalizeCode(code);
        Role role = roles.findById(normalized).orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        if (roles.existsByNameIgnoreCaseAndCodeNot(request.name().trim(), normalized)) {
            throw new IllegalArgumentException("Role name already exists");
        }
        role.update(request.name(), request.modelRole());
        return toResponse(role);
    }

    @Transactional
    public void delete(String code) {
        String normalized = Role.normalizeCode(code);
        if (!roles.existsById(normalized)) throw new ResourceNotFoundException("Role not found");
        if (users.existsByRole(normalized)) throw new IllegalArgumentException("Role is assigned to one or more users");
        roles.deleteById(normalized);
    }

    private static RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getCode(), role.getName(), role.getModelRole(),
                role.getCreatedAt(), role.getUpdatedAt());
    }
}
