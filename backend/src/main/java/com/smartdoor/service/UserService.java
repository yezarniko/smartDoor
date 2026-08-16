package com.smartdoor.service;

import com.smartdoor.api.ResourceNotFoundException;
import com.smartdoor.api.dto.ApiDtos.*;
import com.smartdoor.domain.*;
import com.smartdoor.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class UserService {
    private final UserRepository users;
    private final DoorRepository doors;
    private final PermissionRepository permissions;
    private final ScheduleRepository schedules;

    public UserService(UserRepository users, DoorRepository doors, PermissionRepository permissions,
                       ScheduleRepository schedules) {
        this.users = users;
        this.doors = doors;
        this.permissions = permissions;
        this.schedules = schedules;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(UserService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(String id) { return toResponse(requireUser(id)); }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (users.existsByPublicId(request.publicId().trim())) {
            throw new IllegalArgumentException("User ID already exists");
        }
        return toResponse(users.save(new UserAccount(request.publicId(), request.fullName(), request.email(),
                request.phone(), request.role())));
    }

    @Transactional
    public UserResponse update(String id, UserRequest request) {
        UserAccount user = requireUser(id);
        users.findByPublicId(request.publicId().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new IllegalArgumentException("User ID already exists"); });
        user.update(request.publicId(), request.fullName(), request.email(), request.phone(), request.role());
        return toResponse(user);
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

    public static UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getPublicId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getRole(), user.getStatus(), user.getCreatedAt(), user.getUpdatedAt());
    }
}

