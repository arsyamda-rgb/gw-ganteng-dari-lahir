package com.awakening;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;

public enum Race {
    // Beast: amber/oranye -> cokelat tanah (liar, buas)
    BEAST("Beast", "#FFA63A", "#B5541A", Color.fromRGB(240, 140, 40), Color.fromRGB(130, 65, 20),
            Particle.CRIT, Particle.CRIT,
            Sound.ENTITY_WOLF_HOWL, Sound.ENTITY_RAVAGER_ROAR),
    // Dragon: merah -> emas, berapi-api
    DRAGON("Dragon", "#FF2200", "#FFC21A", Color.fromRGB(255, 45, 0), Color.fromRGB(255, 195, 30),
            Particle.FLAME, Particle.FLAME,
            Sound.ENTITY_ENDER_DRAGON_GROWL, Sound.ENTITY_ENDER_DRAGON_DEATH),
    // Elf: hijau
    ELF("Elf", "#12C94A", "#9CFF6B", Color.fromRGB(30, 205, 75), Color.fromRGB(165, 255, 115),
            Particle.HAPPY_VILLAGER, Particle.HAPPY_VILLAGER,
            Sound.ITEM_TOTEM_USE, Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    // Human: putih
    HUMAN("Human", "#FFFFFF", "#D3DCE8", Color.fromRGB(255, 255, 255), Color.fromRGB(195, 205, 220),
            Particle.ELECTRIC_SPARK, Particle.END_ROD,
            Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.ENTITY_PLAYER_LEVELUP);

    public final String display;
    public final String g1;         // warna gradasi teks (hex)
    public final String g2;
    public final Color primary;
    public final Color secondary;
    public final Particle ambient;  // keluar dari tanah
    public final Particle accent;   // aura di badan
    public final Sound sound1;
    public final Sound sound2;

    Race(String display, String g1, String g2, Color primary, Color secondary,
         Particle ambient, Particle accent, Sound sound1, Sound sound2) {
        this.display = display;
        this.g1 = g1;
        this.g2 = g2;
        this.primary = primary;
        this.secondary = secondary;
        this.ambient = ambient;
        this.accent = accent;
        this.sound1 = sound1;
        this.sound2 = sound2;
    }

    public static Race parse(String s) {
        if (s == null) return null;
        for (Race r : values()) {
            if (r.name().equalsIgnoreCase(s)) return r;
        }
        return null;
    }
}
