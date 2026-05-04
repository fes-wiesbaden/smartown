package fes.smartown.backend.bridge.model;

import java.time.Instant;

public record BridgeSnapshot(
    BridgeMode mode,
    boolean isPhysicallyOpen,
    boolean brokerConnected,
    boolean espOnline,
    Instant updatedAt
) {}
