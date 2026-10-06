package com.example.fraudpoc.benchmark;

import com.example.fraudpoc.config.AppProperties;
import com.example.fraudpoc.dataset.DatasetLoader;
import com.example.fraudpoc.model.*;
import com.example.fraudpoc.service.ArchitectureOneService;
import com.example.fraudpoc.service.ArchitectureThreeService;
import com.example.fraudpoc.service.ArchitectureTwoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BenchmarkRunner {

    private final DatasetLoader loader;
    private final ArchitectureOneService a1;
    private final ArchitectureTwoService a2;
    private final ArchitectureThreeService a3;
    private final MetricsCalculator metrics;
    private final AppProperties props;

    public BenchmarkRunner(
            DatasetLoader loader,
            ArchitectureOneService a1,
            ArchitectureTwoService a2,
            ArchitectureThreeService a3,
            AppProperties props) {

        this.loader = loader;
        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;
        this.metrics = new MetricsCalculator();
        this.props = props;
    }

    public ArchitectureMetrics runArchitectureOne()
            throws JsonProcessingException {

        List<DatasetRecord> records = limit(loader.load());

        List<EvaluationResult> results =
                new ArrayList<>();

        for (DatasetRecord r : records) {
            results.add(a1.evaluate(r));
        }

        return metrics.calculate(
                records,
                results,
                "ARCHITECTURE_1"
        );
    }

    public ArchitectureMetrics runArchitectureTwo()
            throws JsonProcessingException {

        List<DatasetRecord> records = limit(loader.load());

        List<EvaluationResult> results =
                new ArrayList<>();

        for (DatasetRecord r : records) {
            results.add(a2.evaluate(r));
        }

        return metrics.calculate(
                records,
                results,
                "ARCHITECTURE_2"
        );
    }

    public ArchitectureMetrics runArchitectureThree()
            throws JsonProcessingException {

        List<DatasetRecord> records = limit(loader.load());

        List<EvaluationResult> results =
                new ArrayList<>();

        for (DatasetRecord r : records) {
            results.add(a3.evaluate(r));
        }

        return metrics.calculate(
                records,
                results,
                "ARCHITECTURE_3"
        );
    }

    private List<DatasetRecord> limit(
            List<DatasetRecord> records) {

        return records.subList(
                0,
                Math.min(
                        props.getBenchmark().getLimit(),
                        records.size()
                )
        );
    }
}