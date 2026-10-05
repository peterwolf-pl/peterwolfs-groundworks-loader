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

/**
 * Modern in-cab instrument HUD displayed while operating the wheel loader.
 */
public class LoaderHudOverlay implements HudElement {

    public static final Identifier ID = GroundworksLoaderMod.id("hud_overlay");

    public static void register() {
        HudElementRegistry.addLast(ID, new LoaderHudOverlay());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (!(client.player.getVehicle() instanceof GroundworksLoaderEntity loader)) {
            return;
        }

        Font font = client.font;
        int x = 10;
        int y = 10;
        int width = 280;
        int height = 76;

        // Semi-transparent HUD backing plate
        extractor.fill(x - 4, y - 4, x + width, y + height, 0x88000000);
        extractor.outline(x - 4, y - 4, width + 4, height + 4, 0xFFFFAA00);

        // Header
        extractor.text(font, "§6§lPeterwolf's Groundworks Wheel Loader", x, y, 0xFFFFFF, true);

        // Speed & Direction
        float speed = loader.getForwardSpeed();
        float kmh = Math.abs(speed) * 72.0F; // Approx km/h in MC units
        String gear = speed > 0.01F ? "§aD (Przód)" : (speed < -0.01F ? "§cR (Wsteczny)" : "§eN (Neutral)");
        extractor.text(font, String.format("Napęd [WSAD]: %s §7| §f%.1f km/h", gear, kmh), x, y + 11, 0xFFFFFF, true);

        // Boom Elevation
        float boom = loader.getBoomAngle();
        String boomStatus;
        if (boom > 25.0F) {
            boomStatus = "§6§lW GÓRZE (Załadunek)";
        } else if (boom >= -5.0F) {
            boomStatus = "§a§lPOZIOM GRUNTU";
        } else {
            boomStatus = "§c§lGŁĘBOKIE KOPANIE / RÓW";
        }
        extractor.text(font, String.format("Wysięgnik [↑/↓]: §f%.1f° §7(%s§7)", boom, boomStatus), x, y + 22, 0xFFFFFF, true);

        // Bucket Tilt
        float bucket = loader.getBucketAngle();
        String bucketStatus = bucket > 15.0F ? "§c§lOTWARTA (Wysyp)" : (bucket < -10.0F ? "§a§lZAMKNIĘTA" : "§ePOZIOMO");
        extractor.text(font, String.format("Łyżka [←/→]: §f%.1f° §7(%s§7)", bucket, bucketStatus), x, y + 33, 0xFFFFFF, true);

        // Carried Material & Fill Bar
        int units = loader.getCarriedUnits();
        int cap = LoaderBucketController.BUCKET_CAPACITY;
        String matName = loader.getCarriedMaterialId() > 0 && loader.getCarriedMaterial() != null
                ? loader.getCarriedMaterial().name().toUpperCase()
                : "PUSTA";
        double m3 = (double) units / 512.0D;
        extractor.text(font, String.format("Łyżka: §f%s §7(%d / %d u | %.2f m³)", matName, units, cap, m3), x, y + 46, 0xCCCCCC, true);

        // Progress bar background & fill
        int barW = 270;
        int barH = 5;
        int barY = y + 58;
        extractor.fill(x, barY, x + barW, barY + barH, 0xFF333333);
        float ratio = Math.min(1.0F, (float) units / (float) cap);
        int fillW = (int) (ratio * barW);
        if (fillW > 0) {
            int barColor = ratio >= 0.95F ? 0xFFFF4444 : (ratio >= 0.75F ? 0xFFFFBB00 : 0xFF44FF44);
            extractor.fill(x, barY, x + fillW, barY + barH, barColor);
        }

        // Live Operating State (Scooping / Dumping)
        String liveAction;
        if (loader.isScooping()) {
            liveAction = "§e§lŁADOWANIE MATERIAŁU";
        } else if (loader.isDumping()) {
            liveAction = "§6§lWYSYP Z ŁYŻKI";
        } else {
            liveAction = "§7GOTOWA";
        }
        extractor.text(font, "Stan: " + liveAction, x + 160, y + 11, 0xFFFFFF, true);
    }
}
