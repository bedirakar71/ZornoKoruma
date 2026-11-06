package dev.zorno.guard.core;

public interface ViolationSink {
    void flag(PlayerData d, CheckType type, double weight, String detail);
}
