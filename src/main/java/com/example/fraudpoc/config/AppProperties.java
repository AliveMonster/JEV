package com.example.fraudpoc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fraud")
public class AppProperties {
    private final Ollama ollama = new Ollama();
    private final SystemOne systemOne = new SystemOne();
    private final Benchmark benchmark = new Benchmark();
    private Jev jev = new Jev();

    public Ollama getOllama() { return ollama; }
    public SystemOne getSystemOne() { return systemOne; }
    public Benchmark getBenchmark() { return benchmark; }

    public static class Jev {

        private String baseUrl;
        private String apiKey;
        private String model;
        private double confidenceThreshold;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getConfidenceThreshold() {
            return confidenceThreshold;
        }

        public void setConfidenceThreshold(double confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
        }
    }

    public static class Ollama {
        private String baseUrl = "http://localhost:11434";
        private String model = "qwen3:1.7b";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public Jev getJev() {
        return jev;
    }

    public void setJev(Jev jev) {
        this.jev = jev;
    }
    public static class SystemOne {
        private String baseUrl = "http://localhost:11435";
        private String model = "laya:en";
        private double confidenceThreshold = 0.70;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public double getConfidenceThreshold() { return confidenceThreshold; }
        public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
    }

    public static class Benchmark {
        private String dataset = "/datasets/fraud-dataset.json";
        private int limit = 10000;
        private boolean failFast = true;
        public String getDataset() { return dataset; }
        public void setDataset(String dataset) { this.dataset = dataset; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public boolean isFailFast() { return failFast; }
        public void setFailFast(boolean failFast) { this.failFast = failFast; }
    }
}
