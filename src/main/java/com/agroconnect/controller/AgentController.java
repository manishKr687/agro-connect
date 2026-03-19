package com.agroconnect.controller;

import com.agroconnect.model.Agent;
import com.agroconnect.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {
    private final AgentRepository agentRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> addAgent(@RequestBody Agent agent) {
        agentRepository.save(agent);
        return ResponseEntity.ok("Agent added");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAgent(@PathVariable Long id, @RequestBody Agent updatedAgent) {
        return agentRepository.findById(id)
            .map(agent -> {
                agent.setName(updatedAgent.getName());
                agent.setRegion(updatedAgent.getRegion());
                agentRepository.save(agent);
                return ResponseEntity.ok("Agent updated");
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAgent(@PathVariable Long id) {
        if (agentRepository.existsById(id)) {
            agentRepository.deleteById(id);
            return ResponseEntity.ok("Agent deleted");
        }
        return ResponseEntity.notFound().build();
    }
}
