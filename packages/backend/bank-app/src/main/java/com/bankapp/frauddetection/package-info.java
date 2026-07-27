/**
 * Fraud detection bounded context: consumes payment events (Kafka), flags suspicious
 * activity, emits alerts. Mostly event-driven; few or no REST endpoints.
 */
package com.bankapp.frauddetection;
