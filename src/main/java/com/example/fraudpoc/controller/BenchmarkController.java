package com.example.fraudpoc.controller;

import com.example.fraudpoc.benchmark.BenchmarkRunner;
import com.example.fraudpoc.model.ArchitectureMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/benchmark")
public class BenchmarkController {

    private final BenchmarkRunner runner;

    public BenchmarkController(
            BenchmarkRunner runner) {

        this.runner = runner;
    }

    @PostMapping("/architecture-1")
    public ArchitectureMetrics a1()
            throws JsonProcessingException {

        return runner.runArchitectureOne();
    }

    @PostMapping("/architecture-2")
    public ArchitectureMetrics a2()
            throws JsonProcessingException {

        return runner.runArchitectureTwo();
    }

    @PostMapping("/architecture-3")
    public ArchitectureMetrics a3()
            throws JsonProcessingException {

        return runner.runArchitectureThree();
    }
}