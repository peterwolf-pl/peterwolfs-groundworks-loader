package com.piotrek.groundworksloader.client.render;

import com.piotrek.groundworksloader.GroundworksLoaderMod;
import com.piotrek.groundworksloader.bucket.LoaderBucketController;
import com.piotrek.groundworksloader.entity.GroundworksLoaderEntity;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * High-contrast in-cab HUD for the Groundworks wheel loader.
 */
public class LoaderHudOverlay implements HudElement {

    public static final Identifier ID = GroundworksLoaderMod.id("hud_overlay");

    private static final int PANEL_BG = 0xE6000000;
    private static final int PANEL_BORDER = 0xFFFFB000;
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFFE6E6E6;
    private static final int TEXT_MUTED = 0xFFB8B8B8;
    private static final int TEXT_AMBER = 0xFFFFC247;
    private static final int TEXT_GREEN = 0xFF72FF72;
    private static final int TEXT_RED = 0xFFFF6868;
    private static final int BAR_BG = 0xFF242424;
    private static final int BAR_FILL = 0xFF25B7FF;
    private static final int BAR_FULL = 0xFFFF6A3D;

    public static void register() {
        HudElementRegistry.addLast(ID, new LoaderHudOverlay());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        if (!(client.player.getVehicle() instanceof GroundworksLoaderEntity loader)) {
            return;
        }

        Font font = client.font;
        int x = 12;
        int y = 12;
        int width = 360;
        int height = 112;

        extractor.fill(x - 6, y - 6, x + width, y + height, PANEL_BG);
        extractor.outline(x - 6, y - 6, width + 6, height + 6, PANEL_BORDER);

        extractor.text(font, "PETERWOLF'S GROUNDWORKS WHEEL LOADER", x, y, TEXT_AMBER, true);

        float speed = loader.getForwardSpeed();
        float kmh = Math.abs(speed) * 72.0F;
        String gear = speed > 0.01F ? "D / PRZÓD" : (speed < -0.01F ? "R / WSTECZNY" : "N / NEUTRAL");
        int gearColor = speed > 0.01F ? TEXT_GREEN : (speed < -0.01F ? TEXT_RED : TEXT_AMBER);
        extractor.text(font, String.format("Napęd: %s   %.1f km/h", gear, kmh), x, y + 13, gearColor, true);

        String action = loader.isScooping()
                ? "ŁADOWANIE"
                : (loader.isDumping() ? "WYSYP" : "GOTOWA");
        int actionColor = loader.isScooping() || loader.isDumping() ? TEXT_AMBER : TEXT_GREEN;
        extractor.text(font, "Stan: " + action, x + 230, y + 13, actionColor, true);

        float boom = loader.getBoomAngle();
        String boomStatus = boom > 20.0F
                ? "ZAŁADUNEK"
                : (boom >= 0.0F ? "TRANSPORT" : (boom >= -10.0F ? "SKRAWANIE" : "GŁĘBOKIE KOPANIE"));
        int boomColor = boom < -10.0F ? TEXT_RED : (boom < 0.0F ? TEXT_AMBER : TEXT_PRIMARY);
        extractor.text(
                font,
                String.format("Wysięgnik [↑/↓]: %.1f°   %s", boom, boomStatus),
                x,
                y + 26,
                boomColor,
                true
        );

        float bucket = loader.getBucketAngle();
        String bucketStatus = bucket > 45.0F
                ? "PEŁNY WYSYP"
                : (bucket > 15.0F ? "OTWARTA" : (bucket < -15.0F ? "ZAMKNIĘTA" : "POZIOMO"));
        int bucketColor = bucket > 15.0F ? TEXT_AMBER : TEXT_PRIMARY;
        extractor.text(
                font,
                String.format("Łyżka [←/→]: %.1f°   %s", bucket, bucketStatus),
                x,
                y + 39,
                bucketColor,
                true
        );

        double lowestEdgeY = LoaderBucketController.getCuttingEdgePoints(
                        loader.position(),
                        loader.getYRot(),
                        loader.getVehiclePitch(),
                        loader.getBoomAngle(),
                        loader.getBucketAngle()
                ).stream()
                .mapToDouble(Vec3::y)
                .min()
                .orElse(loader.getY());

        double clearance = lowestEdgeY - loader.getY();
        String clearanceText;
        int clearanceColor;
        if (clearance >= 0.0D) {
            clearanceText = String.format("Dolna krawędź łyżki nad gruntem: +%.2f m", clearance);
            clearanceColor = clearance > 0.05D ? TEXT_GREEN : TEXT_AMBER;
        } else {
            clearanceText = String.format("Dolna krawędź łyżki poniżej gruntu: %.2f m", clearance);
            clearanceColor = TEXT_RED;
        }
        extractor.text(font, clearanceText, x, y + 52, clearanceColor, true);

        int units = loader.getCarriedUnits();
        int cap = LoaderBucketController.BUCKET_CAPACITY;
        String matName = loader.getCarriedMaterialId() > 0 && loader.getCarriedMaterial() != null
                ? loader.getCarriedMaterial().name().toUpperCase()
                : "PUSTA";
        double m3 = (double) units / 512.0D;
        extractor.text(
                font,
                String.format("Urobek: %s   %d/%d u   %.2f m³", matName, units, cap, m3),
                x,
                y + 65,
                TEXT_SECONDARY,
                true
        );

        int barW = 250;
        int barH = 6;
        int barY = y + 78;
        float ratio = Math.min(1.0F, (float) units / (float) cap);
        extractor.fill(x, barY, x + barW, barY + barH, BAR_BG);
        int fillW = Math.round(ratio * barW);
        if (fillW > 0) {
            extractor.fill(x, barY, x + fillW, barY + barH, ratio >= 0.90F ? BAR_FULL : BAR_FILL);
        }
        extractor.text(font, String.format("%.0f%%", ratio * 100.0F), x + barW + 8, barY - 2, TEXT_PRIMARY, true);

        extractor.text(
                font,
                String.format(
                        "Przegub: %.1f°   Pochylenie: %.1f°   Boczne: %.1f°",
                        loader.getSteerAngle(),
                        loader.getVehiclePitch(),
                        loader.getVehicleRoll()
                ),
                x,
                y + 91,
                TEXT_SECONDARY,
                true
        );
        extractor.text(
                font,
                "[W/S] Jazda   [A/D] Przegub   [↑/↓] Wysięgnik   [←/→] Łyżka",
                x,
                y + 102,
                TEXT_MUTED,
                true
        );
    }
}
