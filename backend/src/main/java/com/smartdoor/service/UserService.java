package com.smartdoor.service;

import com.smartdoor.api.ResourceNotFoundException;
import com.smartdoor.api.dto.ApiDtos.*;
import com.smartdoor.domain.*;
import com.smartdoor.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UserService {
    private final UserRepository users;
    private final DoorRepository doors;
    private final PermissionRepository permissions;
    private final ScheduleRepository schedules;
    private final QrCredentialRepository credentials;
    private final AccessEventRepository events;
    private final RoleRepository roles;

    public UserService(UserRepository users, DoorRepository doors, PermissionRepository permissions,
                       ScheduleRepository schedules, QrCredentialRepository credentials,
                       AccessEventRepository events, RoleRepository roles) {
        this.users = users;
        this.doors = doors;
        this.permissions = permissions;
        this.schedules = schedules;
        this.credentials = credentials;
        this.events = events;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(UserService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(String id) { return toResponse(requireUser(id)); }

    @Transactional(readOnly = true)
    public NextUserCodeResponse nextCode() {
        return new NextUserCodeResponse(nextAvailableCode());
    }

    @Transactional
    public synchronized UserResponse create(UserCreateRequest request) {
        String role = requireRole(request.role());
        int code = nextAvailableCode();
        String publicId = "USR-" + code;
        return toResponse(users.saveAndFlush(new UserAccount(publicId, code, request.fullName(), request.email(),
                request.phone(), role)));
    }

    @Transactional
    public UserResponse update(String id, UserUpdateRequest request) {
        UserAccount user = requireUser(id);
        user.update(request.fullName(), request.email(), request.phone(), requireRole(request.role()));
        return toResponse(user);
    }

    @Transactional
    public void delete(String id) {
        requireUser(id);
        events.deleteAllByUserId(id);
        credentials.deleteAllByUserId(id);
        schedules.deleteAllByUserId(id);
        permissions.deleteAllByUserId(id);
        users.deleteById(id);
    }

    @Transactional
    public UserResponse setStatus(String id, StatusRequest request) {
        UserAccount user = requireUser(id);
        user.setStatus(request.status());
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissions(String userId) {
        requireUser(userId);
        return permissions.findAllByUserId(userId).stream().map(permission -> new PermissionResponse(
                permission.getDoor().getId(), permission.getDoor().getPublicId(), permission.getDoor().getName())).toList();
    }

    @Transactional
    public List<PermissionResponse> replacePermissions(String userId, PermissionRequest request) {
        UserAccount user = requireUser(userId);
        permissions.deleteAllByUserId(userId);
        request.doorIds().stream().distinct().forEach(doorId -> {
            Door door = doors.findById(doorId).orElseThrow(() -> new ResourceNotFoundException("Door not found"));
            permissions.save(new UserDoorPermission(user, door));
        });
        return getPermissions(userId);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedules(String userId) {
        requireUser(userId);
        return schedules.findAllByUserIdOrderByDayOfWeekAscStartTimeAsc(userId).stream()
                .map(schedule -> new ScheduleResponse(schedule.getId(), schedule.getDayOfWeek(),
                        schedule.getStartTime(), schedule.getEndTime())).toList();
    }

    @Transactional
    public List<ScheduleResponse> replaceSchedules(String userId, ScheduleRequest request) {
        UserAccount user = requireUser(userId);
        schedules.deleteAllByUserId(userId);
        request.schedules().forEach(item -> schedules.save(
                new AccessSchedule(user, item.dayOfWeek(), item.startTime(), item.endTime())));
        return getSchedules(userId);
    }

    public UserAccount requireUser(String id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private int nextAvailableCode() {
        Set<Integer> used = new HashSet<>();
        users.findAll().forEach(user -> used.add(user.getUserCode()));
        int candidate = 1;
        while (used.contains(candidate)) candidate++;
        return candidate;
    }

    private String requireRole(String value) {
        String code = value.trim().toUpperCase(Locale.ROOT);
        if (!roles.existsById(code)) throw new IllegalArgumentException("Role does not exist: " + code);
        return code;
    }

    public static UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getPublicId(), user.getUserCode(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getRole(), user.getStatus(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
