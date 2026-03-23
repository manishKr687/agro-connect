package com.agroconnect.controller;

import com.agroconnect.dto.ApproveAssignmentRequest;
import com.agroconnect.dto.RegisterUserRequest;
import com.agroconnect.dto.CancelTaskRequest;
import com.agroconnect.dto.CreateDeliveryTaskRequest;
import com.agroconnect.dto.MatchSuggestionResponse;
import com.agroconnect.dto.ReassignTaskRequest;
import com.agroconnect.dto.TaskExceptionResponse;
import com.agroconnect.dto.UpdateDeliveryTaskRequest;
import com.agroconnect.dto.UpdateDemandStatusRequest;
import com.agroconnect.dto.UpdateHarvestStatusRequest;
import com.agroconnect.dto.UpdateUserRequest;
import com.agroconnect.model.DeliveryTask;
import com.agroconnect.model.Demand;
import com.agroconnect.model.Harvest;
import com.agroconnect.model.User;
import com.agroconnect.service.DeliveryTaskService;
import com.agroconnect.service.DemandService;
import com.agroconnect.service.HarvestService;
import com.agroconnect.service.MatchingService;
import com.agroconnect.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admins/{adminId}")
@RequiredArgsConstructor
public class AdminDeliveryTaskController {
    private final DeliveryTaskService deliveryTaskService;
    private final HarvestService harvestService;
    private final DemandService demandService;
    private final MatchingService matchingService;
    private final UserService userService;

    @GetMapping("/users")
    public List<User> getAllUsers(@PathVariable Long adminId) {
        return userService.getAllUsersForAdmin(adminId);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(
            @PathVariable Long adminId,
            @Valid @RequestBody RegisterUserRequest request
    ) {
        return userService.createUserForAdmin(adminId, request);
    }

    @GetMapping("/harvests")
    public List<Harvest> getAllHarvests(@PathVariable Long adminId) {
        return harvestService.getAllHarvestsForAdmin(adminId);
    }

    @GetMapping("/demands")
    public List<Demand> getAllDemands(@PathVariable Long adminId) {
        return demandService.getAllDemandsForAdmin(adminId);
    }

    @GetMapping("/tasks")
    public List<DeliveryTask> getAllTasks(@PathVariable Long adminId) {
        return deliveryTaskService.getAllTasksForAdmin(adminId);
    }

    @GetMapping("/exceptions/tasks")
    public List<TaskExceptionResponse> getTaskExceptions(@PathVariable Long adminId) {
        return deliveryTaskService.getTaskExceptionsForAdmin(adminId);
    }

    @GetMapping("/match-suggestions")
    public List<MatchSuggestionResponse> getMatchSuggestions(@PathVariable Long adminId) {
        return matchingService.getTopSuggestionsForAdmin(adminId);
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryTask createTask(
            @PathVariable Long adminId,
            @Valid @RequestBody CreateDeliveryTaskRequest request
    ) {
        request.setAdminId(adminId);
        return deliveryTaskService.createTask(request);
    }

    @PostMapping("/assignments/approve")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryTask approveAssignment(
            @PathVariable Long adminId,
            @Valid @RequestBody ApproveAssignmentRequest request
    ) {
        return deliveryTaskService.approveAssignment(adminId, request);
    }

    @PostMapping("/tasks/{taskId}/reassign")
    public DeliveryTask reassignTask(
            @PathVariable Long adminId,
            @PathVariable Long taskId,
            @RequestBody(required = false) ReassignTaskRequest request
    ) {
        return deliveryTaskService.reassignTaskForAdmin(adminId, taskId, request == null ? new ReassignTaskRequest() : request);
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public DeliveryTask cancelTask(
            @PathVariable Long adminId,
            @PathVariable Long taskId,
            @Valid @RequestBody CancelTaskRequest request
    ) {
        return deliveryTaskService.cancelTaskForAdmin(adminId, taskId, request);
    }

    @PostMapping("/tasks/{taskId}/retry")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryTask retryTask(@PathVariable Long adminId, @PathVariable Long taskId) {
        return deliveryTaskService.retryTaskForAdmin(adminId, taskId);
    }

    @PutMapping("/users/{userId}")
    public User updateUser(
            @PathVariable Long adminId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.updateUserForAdmin(adminId, userId, request);
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long adminId, @PathVariable Long userId) {
        userService.deleteUserForAdmin(adminId, userId);
    }

    @PutMapping("/harvests/{harvestId}")
    public Harvest updateHarvest(
            @PathVariable Long adminId,
            @PathVariable Long harvestId,
            @Valid @RequestBody UpdateHarvestStatusRequest request
    ) {
        return harvestService.updateHarvestForAdmin(adminId, harvestId, request);
    }

    @PutMapping("/demands/{demandId}")
    public Demand updateDemand(
            @PathVariable Long adminId,
            @PathVariable Long demandId,
            @Valid @RequestBody UpdateDemandStatusRequest request
    ) {
        return demandService.updateDemandForAdmin(adminId, demandId, request);
    }

    @PostMapping("/demands/{demandId}/approve-change")
    public Demand approveDemandChange(@PathVariable Long adminId, @PathVariable Long demandId) {
        return demandService.approveDemandChangeForAdmin(adminId, demandId);
    }

    @PostMapping("/demands/{demandId}/reject-change")
    public Demand rejectDemandChange(@PathVariable Long adminId, @PathVariable Long demandId) {
        return demandService.rejectDemandChangeForAdmin(adminId, demandId);
    }

    @PutMapping("/tasks/{taskId}")
    public DeliveryTask updateTask(
            @PathVariable Long adminId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateDeliveryTaskRequest request
    ) {
        return deliveryTaskService.updateTaskForAdmin(adminId, taskId, request);
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long adminId, @PathVariable Long taskId) {
        deliveryTaskService.deleteTaskForAdmin(adminId, taskId);
    }
}
