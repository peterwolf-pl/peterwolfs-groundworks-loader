package com.piotrek.groundworksloader.client.render;

import com.piotrek.groundworksloader.GroundworksLoaderMod;
import com.piotrek.groundworksloader.entity.GroundworksLoaderEntity;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * High-contrast in-cab instrument HUD displayed while operating the wheel loader.
 *
 * <p>Shows real-time drive gear and speed, articulation angle, true cutting-edge
 * clearance above/below ground, boom/bucket angles, carried payload volume and controls.</p>
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
        if (client.player == null) return;

        if (!(client.player.getVehicle() instanceof GroundworksLoaderEntity loader)) {
            return;
        }

        Font font = client.font;
        int x = 12;
        int y = 12;
        int width = 350;
        int height = 98;

        // Opaque high-contrast background to prevent terrain texture bleed-through
        extractor.fill(x - 6, y - 6, x + width, y + height, PANEL_BG);
        extractor.outline(x - 6, y - 6, width + 6, height + 6, PANEL_BORDER);

        // Header
        extractor.text(font, I18n.get("hud.pw_groundworks_loader.header"), x, y, TEXT_AMBER, true);

        // Speed & Direction
        float speed = loader.getForwardSpeed();
        float kmh = Math.abs(speed) * 72.0F;
        String gear = speed > 0.01F
                ? I18n.get("hud.pw_groundworks_loader.gear_forward")
                : (speed < -0.01F ? I18n.get("hud.pw_groundworks_loader.gear_reverse") : I18n.get("hud.pw_groundworks_loader.gear_neutral"));

        extractor.text(font, I18n.get("hud.pw_groundworks_loader.drive", gear, kmh), x, y + 13, TEXT_PRIMARY, true);

        // Articulation Steering Angle & Status
        float steer = loader.getSteerAngle();
        String steerLabel;
        int steerColor;
        if (Math.abs(steer) < 1.0F) {
            steerLabel = I18n.get("hud.pw_groundworks_loader.steer_straight");
            steerColor = TEXT_GREEN;
        } else if (steer > 0.0F) {
            steerLabel = I18n.get("hud.pw_groundworks_loader.steer_right", steer);
            steerColor = TEXT_AMBER;
        } else {
            steerLabel = I18n.get("hud.pw_groundworks_loader.steer_left", steer);
            steerColor = TEXT_AMBER;
        }
        extractor.text(font, I18n.get("hud.pw_groundworks_loader.steer", steerLabel), x + 200, y + 13, steerColor, true);

        // Bucket Cutting Edge Clearance Above Ground
        List<Vec3> edgePoints = loader.getCuttingEdgePoints();
        double lowestLipY = edgePoints.stream()
                .mapToDouble(p -> p.y)
                .min()
                .orElse(loader.getY());
        double clearance = lowestLipY - loader.getY();

        String clearanceText;
        int clearanceColor;
        if (clearance >= 0.0D) {
            clearanceText = I18n.get("hud.pw_groundworks_loader.clearance_above", clearance);
            clearanceColor = clearance > 0.05D ? TEXT_GREEN : TEXT_AMBER;
        } else {
            clearanceText = I18n.get("hud.pw_groundworks_loader.clearance_below", clearance);
            clearanceColor = TEXT_RED;
        }
        extractor.text(font, clearanceText, x, y + 26, clearanceColor, true);

        // Live Action Status (Scooping / Dumping / Ready)
        String liveAction;
        int actionColor;
        if (loader.isScooping()) {
            liveAction = I18n.get("hud.pw_groundworks_loader.status_scooping");
            actionColor = TEXT_AMBER;
        } else if (loader.isDumping()) {
            liveAction = I18n.get("hud.pw_groundworks_loader.status_dumping");
            actionColor = TEXT_AMBER;
        } else {
            liveAction = I18n.get("hud.pw_groundworks_loader.status_ready");
            actionColor = TEXT_GREEN;
        }
        extractor.text(font, I18n.get("hud.pw_groundworks_loader.status", liveAction), x + 250, y + 26, actionColor, true);

        // Boom Elevation & Bucket Tilt angles
        float boom = loader.getBoomAngle();
        float bucket = loader.getBucketAngle();
        extractor.text(
                font,
                I18n.get("hud.pw_groundworks_loader.angles", boom, bucket),
                x,
                y + 39,
                TEXT_SECONDARY,
                true
        );

        // Carried Material & Volume
        int units = loader.getCarriedUnits();
        int cap = loader.capacity();
        double m3 = (double) units / 512.0D;
        String matName;
        if (units <= 0) {
            matName = I18n.get("hud.pw_groundworks_loader.payload_empty");
        } else if (loader.isCarriedPure()) {
            matName = loader.getCarriedMaterial() != null && loader.getCarriedMaterial().id() > 0
                    ? loader.getCarriedMaterial().name().toUpperCase()
                    : "DIRT";
        } else {
            StringBuilder sb = new StringBuilder(I18n.get("hud.pw_groundworks_loader.payload_mixed")).append(" [");
            boolean first = true;
            if (loader.getDirtUnits() > 0) {
                sb.append(I18n.get("hud.pw_groundworks_loader.material_dirt")).append(": ").append(loader.getDirtUnits()).append("u");
                first = false;
            }
            if (loader.getSandUnits() > 0) {
                if (!first) sb.append(", ");
                sb.append(I18n.get("hud.pw_groundworks_loader.material_sand")).append(": ").append(loader.getSandUnits()).append("u");
                first = false;
            }
            if (loader.getGravelUnits() > 0) {
                if (!first) sb.append(", ");
                sb.append(I18n.get("hud.pw_groundworks_loader.material_gravel")).append(": ").append(loader.getGravelUnits()).append("u");
                first = false;
            }
            if (loader.getCobblestoneUnits() > 0) {
                if (!first) sb.append(", ");
                sb.append(I18n.get("hud.pw_groundworks_loader.material_cobblestone")).append(": ").append(loader.getCobblestoneUnits()).append("u");
            }
            sb.append("]");
            matName = sb.toString();
        }
        extractor.text(
                font,
                I18n.get("hud.pw_groundworks_loader.payload", matName, units, cap, m3),
                x,
                y + 52,
                TEXT_SECONDARY,
                true
        );

        // Volume Progress Bar
        int barW = 200;
        int barH = 6;
        int barY = y + 64;
        extractor.fill(x, barY, x + barW, barY + barH, BAR_BG);
        float ratio = Math.min(1.0F, (float) units / (float) cap);
        int fillW = Math.round(barW * ratio);
        if (fillW > 0) {
            extractor.fill(x, barY, x + fillW, barY + barH, ratio > 0.85F ? BAR_FULL : BAR_FILL);
        }
        extractor.text(
                font,
                String.format("%.0f%%", ratio * 100.0F),
                x + barW + 8,
                barY - 2,
                TEXT_PRIMARY,
                true
        );

        // Control hints
        extractor.text(
                font,
                I18n.get("hud.pw_groundworks_loader.hint_controls"),
                x,
                y + 76,
                TEXT_PRIMARY,
                true
        );
        extractor.text(
                font,
                I18n.get("hud.pw_groundworks_loader.hint_steer"),
                x,
                y + 87,
                TEXT_MUTED,
                true
        );
    }
}
