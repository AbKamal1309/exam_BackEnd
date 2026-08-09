package com.acoidemy.exambackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint public minimal, sans authentification, utilisé pour les services
 * de ping anti-veille (cron-job.org, UptimeRobot...) sur le plan gratuit Render.
 * Ne renvoie aucune donnée sensible, juste une confirmation que le service est actif.
 */
@RestController
@CrossOrigin
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
