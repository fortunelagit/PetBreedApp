package com.project.petbreedapp;

public class Recognition {
    private String label;
    private float confidence;

    public Recognition(String label, float confidence) {
        this.label = label;
        this.confidence = confidence;
    }

    public String getLabel() {
        return label;
    }

    public float getConfidence() {
        return confidence;
    }
}

