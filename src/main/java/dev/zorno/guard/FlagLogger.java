package dev.zorno.guard;

import dev.zorno.guard.core.Settings;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/** Günlük dosyalara (logs/flags-YYYY-AA-GG.log) ayrı bir thread'de yazar; ana thread hiç diske dokunmaz. */
public final class FlagLogger {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Path dir;
    private final Settings s;
    private final Logger log;
    private final ExecutorService exec;

    public FlagLogger(File dataFolder, Settings s, Logger log) {
        this.dir = new File(dataFolder, "logs").toPath();
        this.s = s;
        this.log = log;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.warning("Log klasörü oluşturulamadı: " + e.getMessage());
        }
        this.exec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ZornoGuard-Log");
            t.setDaemon(true);
            return t;
        });
        exec.execute(this::cleanOld);
    }

    public void write(String line) {
        if (!s.logFile) return;
        final LocalDateTime now = LocalDateTime.now();
        exec.execute(() -> {
            try {
                Path f = dir.resolve("flags-" + DAY.format(now) + ".log");
                Files.writeString(f, "[" + TIME.format(now) + "] " + line + System.lineSeparator(),
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                log.warning("Log yazılamadı: " + e.getMessage());
            }
        });
    }

    private void cleanOld() {
        int days = s.logRetentionDays;
        if (days <= 0) return;
        long limit = System.currentTimeMillis() - days * 86_400_000L;
        File[] files = dir.toFile().listFiles((d, n) -> n.startsWith("flags-") && n.endsWith(".log"));
        if (files == null) return;
        for (File f : files) {
            if (f.lastModified() < limit) {
                // silinemezse sorun değil
                if (!f.delete()) log.fine("Eski log silinemedi: " + f.getName());
            }
        }
    }

    public void shutdown() {
        exec.shutdown();
        try {
            exec.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
