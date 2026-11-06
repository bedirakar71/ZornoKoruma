package dev.zorno.guard.core;

/** Dünya sorguları için ince arayüz (testte sahtesi yazılır, sunucuda Bukkit gerçeklemesi kullanılır). */
public interface BlockProbe {
    /** İki nokta arasında ilk katı çarpışma bloğuna mesafe; yol açıksa -1. */
    double colliderHit(double x1, double y1, double z1, double x2, double y2, double z2);

    /** Aynısı, ama yalnızca çarpılan blok tam küp ise (duvar/cam/taş gibi); değilse -1. */
    double fullCubeHit(double x1, double y1, double z1, double x2, double y2, double z2);

    /** Nokta tam küp opak bloğun içinde mi (2), örümcek ağının içinde mi (1), değilse 0. */
    int insideKind(double x, double y, double z);
}
