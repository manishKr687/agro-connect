package com.agroconnect.controller;

import com.agroconnect.dto.CreateDeliveryTaskRequest;
import com.agroconnect.dto.DemandRequest;
import com.agroconnect.dto.HarvestRequest;
import com.agroconnect.dto.UpdateDeliveryTaskRequest;
import com.agroconnect.dto.UpdateUserRequest;
import com.agroconnect.model.DeliveryTask;
import com.agroconnect.model.Demand;
import com.agroconnect.model.Harvest;
import com.agroconnect.model.User;
import com.agroconnect.service.DeliveryTaskService;
import com.agroconnect.service.DemandService;
import com.agroconnect.service.HarvestService;
import com.agroconnect.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;

@RestController
@RequestMapping("/api/admins/{adminId}")
@RequiredArgsConstructor
public class AdminDeliveryTaskController {
    private final DeliveryTaskService deliveryTaskService;
    private final HarvestService harvestService;
    private final DemandService demandService;
    private final UserService userService;

    @GetMapping("/users")
    public List<User> getAllUsers(@PathVariable Long adminId) {
        return userService.getAllUsersForAdmin(adminId);
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

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryTask createTask(
            @PathVariable Long adminId,
            @Valid @RequestBody CreateDeliveryTaskRequest request
    ) {
        request.setAdminId(adminId);
        return deliveryTaskService.createTask(request);
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
            @Valid @RequestBody HarvestRequest request
    ) {
        return harvestService.updateHarvestForAdmin(adminId, harvestId, request);
    }

    @DeleteMapping("/harvests/{harvestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHarvest(@PathVariable Long adminId, @PathVariable Long harvestId) {
        harvestService.deleteHarvestForAdmin(adminId, harvestId);
    }

    @PutMapping("/demands/{demandId}")
    public Demand updateDemand(
            @PathVariable Long adminId,
            @PathVariable Long demandId,
            @Valid @RequestBody DemandRequest request
    ) {
        return demandService.updateDemandForAdmin(adminId, demandId, request);
    }

    @DeleteMapping("/demands/{demandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDemand(@PathVariable Long adminId, @PathVariable Long demandId) {
        demandService.deleteDemandForAdmin(adminId, demandId);
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
