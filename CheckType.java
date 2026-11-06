package dev.zorno.guard.core;

/** Tespit türleri: kimlik, Türkçe ad ve uyarı rengi. */
public enum CheckType {
    REACH("reach", "Reach", "#ff6b6b"),
    WALL("wall", "Duvar Arkası", "#ffa94d"),
    INSIDE("inside", "Ağ/Blok İçi", "#ffd43b"),
    AIM("aim", "Nişan", "#69db7c"),
    SILENT("silent", "Sessiz Rotasyon", "#38d9a9"),
    MULTI("multi", "Çoklu Hedef", "#4dabf7"),
    ACTION("action", "İmkânsız Eylem", "#9775fa"),
    RATE("rate", "Vuruş Hızı", "#f783ac"),
    SWING("swing", "Kol Sallama", "#adb5bd");

    public static final int COUNT = values().length;

    public final String id;
    public final String display;
    public final String color;

    CheckType(String id, String display, String color) {
        this.id = id;
        this.display = display;
        this.color = color;
    }

    public static CheckType byId(String s) {
        if (s == null) return null;
        for (CheckType t : values()) {
            if (t.id.equalsIgnoreCase(s)) return t;
        }
        return null;
    }
}
