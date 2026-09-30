package com.ragworkshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagConfig {
    private int chunkSize = 800;
    private int chunkOverlap = 100;
    private int topK = 3;
    private double similarityThreshold = 0.45;
    private double temperature = 0.2;
    private String embeddingModelPath = "models/all-MiniLM-L6-v2/model.onnx";
    private String tokenizerPath = "models/all-MiniLM-L6-v2/tokenizer.json";
    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int value) { chunkSize = value; }
    public int getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(int value) { chunkOverlap = value; }
    public int getTopK() { return topK; }
    public void setTopK(int value) { topK = value; }
    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double value) { similarityThreshold = value; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double value) { temperature = value; }
    public String getEmbeddingModelPath() { return embeddingModelPath; }
    public void setEmbeddingModelPath(String value) { embeddingModelPath = value; }
    public String getTokenizerPath() { return tokenizerPath; }
    public void setTokenizerPath(String value) { tokenizerPath = value; }
}
