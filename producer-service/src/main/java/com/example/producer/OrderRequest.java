package com.example.producer;

/**
 * Lightweight DTO for accepting optional order overrides from the REST API.
 */
public record OrderRequest(String orderId, String product, Float price) {
}
