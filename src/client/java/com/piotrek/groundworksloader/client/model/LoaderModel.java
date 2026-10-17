package com.piotrek.groundworksloader.client.model;

import com.piotrek.groundworksloader.client.render.LoaderRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Geometric hierarchy model for the 4-wheel industrial wheel loader.
 *
 * <p>Faithfully modeled after heavy industrial loaders (CAT / Volvo / SDLG):
 * <ul>
 *   <li>4 massive treaded construction tires with deep chevron lugs and yellow planetary hubs.</li>
 *   <li>Articulated chassis with steering hydraulic cylinders.</li>
 *   <li>Sloped rear engine hood with ventilation louvers, exhaust stack with rain flapper, air cleaner, and heavy counterweight.</li>
 *   <li>Hollow ROPS/FOPS operator safety cab with panoramic safety glass, seat, steering wheel, levers, console, side ladders, and handrails.</li>
 *   <li>Twin curved heavy lift arms with cross-tube and animated hydraulic lift cylinders.</li>
 *   <li>Central Z-bar bell crank linkage and hydraulic tilt cylinder.</li>
 *   <li>Heavy 3.0m excavation scoop bucket with 6 forged teeth, side cutters, and top spill guard.</li>
 *   <li>Generated loose-material mound rendered separately inside the bucket bowl.</li>
 *   <li>Amber rotary safety beacon on the cab roof.</li>
 * </ul>
 */
public class LoaderModel extends EntityModel<LoaderRenderState> {

    private final ModelPart rearChassis;
    private final ModelPart beaconReflector;
    private final ModelPart rearLeftWheel;
    private final ModelPart rearRightWheel;

    private final ModelPart frontChassis;
    private final ModelPart frontLeftWheel;
    private final ModelPart frontRightWheel;

    private final ModelPart liftArms;
    private final ModelPart leftLiftCylinder;
    private final ModelPart rightLiftCylinder;
    private final ModelPart zbarLinkage;

    private final ModelPart bucket;

    public LoaderModel(ModelPart root) {
        super(root);
        this.rearChassis = root.getChild("rear_chassis");
        ModelPart cab = this.rearChassis.getChild("cab");
        this.beaconReflector = cab.getChild("beacon_base").getChild("beacon_reflector");

        this.rearLeftWheel = this.rearChassis.getChild("rear_left_wheel");
        this.rearRightWheel = this.rearChassis.getChild("rear_right_wheel");

        this.frontChassis = root.getChild("front_chassis");
        this.frontLeftWheel = this.frontChassis.getChild("front_left_wheel");
        this.frontRightWheel = this.frontChassis.getChild("front_right_wheel");

        this.liftArms = this.frontChassis.getChild("lift_arms");
        this.leftLiftCylinder = this.frontChassis.getChild("left_lift_cylinder");
        this.rightLiftCylinder = this.frontChassis.getChild("right_lift_cylinder");
        this.zbarLinkage = this.liftArms.getChild("zbar_linkage");

        this.bucket = this.liftArms.getChild("bucket");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ══════════════════════════════════════════════════════════════════════
        // 1. REAR CHASSIS (Rear frame, engine hood, counterweight, cab, wheels)
        // ══════════════════════════════════════════════════════════════════════
        PartDefinition rearChassis = root.addOrReplaceChild(
                "rear_chassis",
                CubeListBuilder.create()
                        // Lower chassis belly frame: UV [264, 0] (extended back to Z = -42.0F)
                        .texOffs(264, 0).addBox(-10.0F, 6.0F, -42.0F, 20.0F, 8.0F, 42.0F)
                        // Rear massive cast counterweight with chamfer: UV [264, 0]
                        .texOffs(264, 0).addBox(-14.0F, 2.0F, -47.0F, 28.0F, 13.0F, 6.0F)
                        // Towing hitch / lower step: UV [264, 0]
                        .texOffs(264, 0).addBox(-6.0F, 13.0F, -48.5F, 12.0F, 3.0F, 3.0F)
                        // Recessed tail lights: UV [264, 0]
                        .texOffs(264, 0).addBox(-12.0F, 4.0F, -47.5F, 5.0F, 3.0F, 1.0F)
                        .texOffs(264, 0).addBox(7.0F, 4.0F, -47.5F, 5.0F, 3.0F, 1.0F)

                        // ── Main Extended Engine Hood & Compartment (Industrial Yellow): UV [0, 84] ──
                        .texOffs(0, 84).addBox(-11.0F, -6.0F, -41.0F, 22.0F, 12.0F, 37.0F)
                        // Sloped upper engine cover: UV [0, 84]
                        .texOffs(0, 84).addBox(-9.5F, -11.0F, -39.0F, 19.0F, 5.0F, 34.0F)
                        // Rear radiator cooling grille: UV [184, 84]
                        .texOffs(184, 84).addBox(-8.0F, -4.0F, -41.5F, 16.0F, 9.0F, 1.0F)
                        // Side ventilation louvers: UV [184, 84]
                        .texOffs(184, 84).addBox(-11.5F, -4.0F, -36.0F, 1.0F, 8.0F, 30.0F)
                        .texOffs(184, 84).addBox(10.5F, -4.0F, -36.0F, 1.0F, 8.0F, 30.0F)

                        // ── Exhaust Stack & Cyclone Air Pre-Cleaner: UV [364, 164] ──
                        // Vertical exhaust stack pipe with rain flapper cap
                        .texOffs(364, 164).addBox(6.0F, -23.0F, -16.0F, 2.5F, 13.0F, 2.5F)
                        .texOffs(364, 164).addBox(5.5F, -24.5F, -16.5F, 3.5F, 2.0F, 3.5F)
                        // Cyclone air cleaner canister
                        .texOffs(364, 164).addBox(-8.5F, -17.0F, -18.0F, 4.0F, 7.0F, 4.0F)

                        // ── Rear Wheel Mudguards / Fenders (Centered over rear axle at Z = -32.0F): UV [0, 84] ──
                        // Left rear mudguard & flares
                        .texOffs(0, 84).addBox(-22.0F, -3.0F, -42.0F, 11.0F, 2.0F, 20.0F)
                        .texOffs(0, 84).addBox(-22.0F, -1.0F, -42.0F, 11.0F, 6.0F, 2.0F)
                        .texOffs(0, 84).addBox(-22.0F, -1.0F, -24.0F, 11.0F, 6.0F, 2.0F)
                        // Right rear mudguard & flares
                        .texOffs(0, 84).addBox(11.0F, -3.0F, -42.0F, 11.0F, 2.0F, 20.0F)
                        .texOffs(0, 84).addBox(11.0F, -1.0F, -42.0F, 11.0F, 6.0F, 2.0F)
                        .texOffs(0, 84).addBox(11.0F, -1.0F, -24.0F, 11.0F, 6.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // ── Rear Left Wheel (High-poly 16-faceted circular tire with rounded planetary hub) ──
        rearChassis.addOrReplaceChild(
                "rear_left_wheel",
                CubeListBuilder.create()
                        // 16-faceted rounded rubber tire: UV [0, 0]
                        .texOffs(0, 0).addBox(-5.0F, -11.0F, -4.0F, 10.0F, 22.0F, 8.0F)
                        .texOffs(0, 0).addBox(-5.0F, -10.5F, -6.5F, 10.0F, 21.0F, 13.0F)
                        .texOffs(0, 0).addBox(-5.0F, -9.5F, -8.5F, 10.0F, 19.0F, 17.0F)
                        .texOffs(0, 0).addBox(-5.0F, -8.5F, -9.5F, 10.0F, 17.0F, 19.0F)
                        .texOffs(0, 0).addBox(-5.0F, -6.5F, -10.5F, 10.0F, 13.0F, 21.0F)
                        .texOffs(0, 0).addBox(-5.0F, -4.0F, -11.0F, 10.0F, 8.0F, 22.0F)
                        // Rounded yellow rim ring: UV [164, 0]
                        .texOffs(164, 0).addBox(-5.5F, -6.5F, -6.5F, 2.0F, 13.0F, 13.0F)
                        .texOffs(164, 0).addBox(-5.5F, -7.5F, -3.5F, 2.0F, 15.0F, 7.0F)
                        .texOffs(164, 0).addBox(-5.5F, -3.5F, -7.5F, 2.0F, 7.0F, 15.0F)
                        // Octagonal planetary gear hub & axle boss: UV [164, 0]
                        .texOffs(164, 0).addBox(-6.5F, -4.0F, -4.0F, 2.0F, 8.0F, 8.0F)
                        .texOffs(164, 0).addBox(-6.5F, -5.0F, -2.0F, 2.0F, 10.0F, 4.0F)
                        .texOffs(164, 0).addBox(-6.5F, -2.0F, -5.0F, 2.0F, 4.0F, 10.0F)
                        .texOffs(164, 0).addBox(-7.0F, -2.0F, -2.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offset(-17.0F, 13.0F, -32.0F)
        );

        // ── Rear Right Wheel ──
        rearChassis.addOrReplaceChild(
                "rear_right_wheel",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -11.0F, -4.0F, 10.0F, 22.0F, 8.0F)
                        .texOffs(0, 0).addBox(-5.0F, -10.5F, -6.5F, 10.0F, 21.0F, 13.0F)
                        .texOffs(0, 0).addBox(-5.0F, -9.5F, -8.5F, 10.0F, 19.0F, 17.0F)
                        .texOffs(0, 0).addBox(-5.0F, -8.5F, -9.5F, 10.0F, 17.0F, 19.0F)
                        .texOffs(0, 0).addBox(-5.0F, -6.5F, -10.5F, 10.0F, 13.0F, 21.0F)
                        .texOffs(0, 0).addBox(-5.0F, -4.0F, -11.0F, 10.0F, 8.0F, 22.0F)
                        .texOffs(164, 0).addBox(3.5F, -6.5F, -6.5F, 2.0F, 13.0F, 13.0F)
                        .texOffs(164, 0).addBox(3.5F, -7.5F, -3.5F, 2.0F, 15.0F, 7.0F)
                        .texOffs(164, 0).addBox(3.5F, -3.5F, -7.5F, 2.0F, 7.0F, 15.0F)
                        .texOffs(164, 0).addBox(4.5F, -4.0F, -4.0F, 2.0F, 8.0F, 8.0F)
                        .texOffs(164, 0).addBox(4.5F, -5.0F, -2.0F, 2.0F, 10.0F, 4.0F)
                        .texOffs(164, 0).addBox(4.5F, -2.0F, -5.0F, 2.0F, 4.0F, 10.0F)
                        .texOffs(164, 0).addBox(6.0F, -2.0F, -2.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offset(17.0F, 13.0F, -32.0F)
        );

        // ── ROPS/FOPS Operator Safety Cabin ──
        PartDefinition cab = rearChassis.addOrReplaceChild(
                "cab",
                CubeListBuilder.create()
                        // Cab floor deck: UV [264, 0]
                        .texOffs(264, 0).addBox(-12.0F, -1.0F, -6.0F, 24.0F, 2.0F, 14.0F)
                        // Roof plate with sun visor overhang: UV [0, 84]
                        .texOffs(0, 84).addBox(-13.0F, -24.0F, -7.0F, 26.0F, 3.0F, 16.0F)
                        // 4 Roll cage pillars (A-pillars and B-pillars): UV [0, 84]
                        .texOffs(0, 84).addBox(-12.5F, -21.0F, 7.0F, 2.0F, 20.0F, 2.0F)
                        .texOffs(0, 84).addBox(10.5F, -21.0F, 7.0F, 2.0F, 20.0F, 2.0F)
                        .texOffs(0, 84).addBox(-12.5F, -21.0F, -6.5F, 2.0F, 20.0F, 2.0F)
                        .texOffs(0, 84).addBox(10.5F, -21.0F, -6.5F, 2.0F, 20.0F, 2.0F)
                        // Lower cowl & rear bulkhead: UV [0, 84]
                        .texOffs(0, 84).addBox(-10.5F, -5.0F, 7.0F, 21.0F, 5.0F, 1.5F)
                        .texOffs(0, 84).addBox(-10.5F, -6.0F, -6.5F, 21.0F, 6.0F, 1.5F)

                        // ── Boarding Ladders & Yellow Tubular Safety Handrails ──
                        // Left ladder & handrails
                        .texOffs(264, 0).addBox(-15.5F, 1.0F, -3.0F, 3.0F, 12.0F, 6.0F)
                        .texOffs(0, 84).addBox(-14.0F, -12.0F, -5.0F, 1.0F, 12.0F, 1.0F)
                        .texOffs(0, 84).addBox(-14.0F, -12.0F, 3.0F, 1.0F, 12.0F, 1.0F)
                        .texOffs(0, 84).addBox(-14.0F, -12.0F, -5.0F, 1.0F, 1.0F, 8.0F)
                        // Right ladder & handrails
                        .texOffs(264, 0).addBox(12.5F, 1.0F, -3.0F, 3.0F, 12.0F, 6.0F)
                        .texOffs(0, 84).addBox(13.0F, -12.0F, -5.0F, 1.0F, 12.0F, 1.0F)
                        .texOffs(0, 84).addBox(13.0F, -12.0F, 3.0F, 1.0F, 12.0F, 1.0F)
                        .texOffs(0, 84).addBox(13.0F, -12.0F, -5.0F, 1.0F, 1.0F, 8.0F)

                        // ── Dual Exterior Rearview Mirrors ──
                        .texOffs(364, 164).addBox(-15.0F, -18.0F, 8.0F, 1.0F, 5.0F, 3.0F)
                        .texOffs(364, 164).addBox(14.0F, -18.0F, 8.0F, 1.0F, 5.0F, 3.0F)

                        // ── Interior Cab Elements: UV [364, 164] ──
                        // Operator suspension seat with armrests
                        .texOffs(364, 164).addBox(-4.0F, 1.0F, -4.0F, 8.0F, 3.0F, 8.0F)
                        .texOffs(364, 164).addBox(-4.0F, -6.0F, -4.0F, 8.0F, 7.0F, 2.0F)
                        // Steering column & wheel
                        .texOffs(364, 164).addBox(-1.0F, 0.0F, 2.0F, 2.0F, 5.0F, 2.0F)
                        .texOffs(364, 164).addBox(-3.5F, -1.0F, 1.0F, 7.0F, 1.0F, 7.0F)
                        // Hydraulic dual control joysticks
                        .texOffs(364, 164).addBox(5.0F, 0.0F, -1.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(364, 164).addBox(7.0F, 0.0F, -1.0F, 1.0F, 4.0F, 1.0F)
                        // Dashboard console
                        .texOffs(364, 164).addBox(-5.0F, 1.0F, 4.0F, 10.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // Safety warning beacon base & spinning reflector: UV [454, 0]
        PartDefinition beaconBase = cab.addOrReplaceChild(
                "beacon_base",
                CubeListBuilder.create()
                        .texOffs(454, 0).addBox(-2.5F, -27.0F, -2.0F, 5.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        beaconBase.addOrReplaceChild(
                "beacon_reflector",
                CubeListBuilder.create()
                        .texOffs(454, 0).addBox(-1.5F, -26.5F, -1.5F, 3.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // ══════════════════════════════════════════════════════════════════════
        // 2. FRONT ARTICULATED CHASSIS (Front frame, towers, lights, front wheels)
        // ══════════════════════════════════════════════════════════════════════
        PartDefinition frontChassis = root.addOrReplaceChild(
                "front_chassis",
                CubeListBuilder.create()
                        // Front chassis belly: UV [264, 0]
                        .texOffs(264, 0).addBox(-10.0F, 6.0F, 0.0F, 20.0F, 8.0F, 22.0F)
                        // Steering hydraulic cylinders: UV [364, 84]
                        .texOffs(364, 84).addBox(-12.0F, 8.0F, -1.0F, 3.0F, 3.0F, 6.0F)
                        .texOffs(364, 84).addBox(9.0F, 8.0F, -1.0F, 3.0F, 3.0F, 6.0F)

                        // ── Front Wheel Mudguards / Fenders (Raised by 1/4 block): UV [0, 84] ──
                        // Left front mudguard
                        .texOffs(0, 84).addBox(-22.0F, -3.0F, 6.0F, 11.0F, 2.0F, 20.0F)
                        .texOffs(0, 84).addBox(-22.0F, -1.0F, 6.0F, 11.0F, 6.0F, 2.0F)
                        .texOffs(0, 84).addBox(-22.0F, -1.0F, 24.0F, 11.0F, 6.0F, 2.0F)
                        // Right front mudguard
                        .texOffs(0, 84).addBox(11.0F, -3.0F, 6.0F, 11.0F, 2.0F, 20.0F)
                        .texOffs(0, 84).addBox(11.0F, -1.0F, 6.0F, 11.0F, 6.0F, 2.0F)
                        .texOffs(0, 84).addBox(11.0F, -1.0F, 24.0F, 11.0F, 6.0F, 2.0F)

                        // ── Boom Pivot Towers & LED Work Headlights: UV [0, 84] & [364, 164] ──
                        .texOffs(0, 84).addBox(-10.0F, -7.0F, 4.0F, 4.0F, 15.0F, 8.0F)
                        .texOffs(0, 84).addBox(6.0F, -7.0F, 4.0F, 4.0F, 15.0F, 8.0F)
                        .texOffs(0, 84).addBox(-6.0F, 0.0F, 6.0F, 12.0F, 3.0F, 4.0F)
                        // Work headlights
                        .texOffs(364, 164).addBox(-10.5F, -9.0F, 8.0F, 3.0F, 3.0F, 2.0F)
                        .texOffs(364, 164).addBox(7.5F, -9.0F, 8.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // ── Front Left Wheel (High-poly 16-faceted circular tire with rounded planetary hub) ──
        frontChassis.addOrReplaceChild(
                "front_left_wheel",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -11.0F, -4.0F, 10.0F, 22.0F, 8.0F)
                        .texOffs(0, 0).addBox(-5.0F, -10.5F, -6.5F, 10.0F, 21.0F, 13.0F)
                        .texOffs(0, 0).addBox(-5.0F, -9.5F, -8.5F, 10.0F, 19.0F, 17.0F)
                        .texOffs(0, 0).addBox(-5.0F, -8.5F, -9.5F, 10.0F, 17.0F, 19.0F)
                        .texOffs(0, 0).addBox(-5.0F, -6.5F, -10.5F, 10.0F, 13.0F, 21.0F)
                        .texOffs(0, 0).addBox(-5.0F, -4.0F, -11.0F, 10.0F, 8.0F, 22.0F)
                        .texOffs(164, 0).addBox(-5.5F, -6.5F, -6.5F, 2.0F, 13.0F, 13.0F)
                        .texOffs(164, 0).addBox(-5.5F, -7.5F, -3.5F, 2.0F, 15.0F, 7.0F)
                        .texOffs(164, 0).addBox(-5.5F, -3.5F, -7.5F, 2.0F, 7.0F, 15.0F)
                        .texOffs(164, 0).addBox(-6.5F, -4.0F, -4.0F, 2.0F, 8.0F, 8.0F)
                        .texOffs(164, 0).addBox(-6.5F, -5.0F, -2.0F, 2.0F, 10.0F, 4.0F)
                        .texOffs(164, 0).addBox(-6.5F, -2.0F, -5.0F, 2.0F, 4.0F, 10.0F)
                        .texOffs(164, 0).addBox(-7.0F, -2.0F, -2.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offset(-17.0F, 13.0F, 16.0F)
        );

        // ── Front Right Wheel ──
        frontChassis.addOrReplaceChild(
                "front_right_wheel",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -11.0F, -4.0F, 10.0F, 22.0F, 8.0F)
                        .texOffs(0, 0).addBox(-5.0F, -10.5F, -6.5F, 10.0F, 21.0F, 13.0F)
                        .texOffs(0, 0).addBox(-5.0F, -9.5F, -8.5F, 10.0F, 19.0F, 17.0F)
                        .texOffs(0, 0).addBox(-5.0F, -8.5F, -9.5F, 10.0F, 17.0F, 19.0F)
                        .texOffs(0, 0).addBox(-5.0F, -6.5F, -10.5F, 10.0F, 13.0F, 21.0F)
                        .texOffs(0, 0).addBox(-5.0F, -4.0F, -11.0F, 10.0F, 8.0F, 22.0F)
                        .texOffs(164, 0).addBox(3.5F, -6.5F, -6.5F, 2.0F, 13.0F, 13.0F)
                        .texOffs(164, 0).addBox(3.5F, -7.5F, -3.5F, 2.0F, 15.0F, 7.0F)
                        .texOffs(164, 0).addBox(3.5F, -3.5F, -7.5F, 2.0F, 7.0F, 15.0F)
                        .texOffs(164, 0).addBox(4.5F, -4.0F, -4.0F, 2.0F, 8.0F, 8.0F)
                        .texOffs(164, 0).addBox(4.5F, -5.0F, -2.0F, 2.0F, 10.0F, 4.0F)
                        .texOffs(164, 0).addBox(4.5F, -2.0F, -5.0F, 2.0F, 4.0F, 10.0F)
                        .texOffs(164, 0).addBox(6.0F, -2.0F, -2.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offset(17.0F, 13.0F, 16.0F)
        );

        // ── Hydraulic Lift Cylinders (Angled from lower frame to boom) ──
        frontChassis.addOrReplaceChild(
                "left_lift_cylinder",
                CubeListBuilder.create()
                        .texOffs(364, 84).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 18.0F)
                        .texOffs(364, 84).addBox(-1.0F, -1.0F, 16.0F, 2.0F, 2.0F, 12.0F),
                PartPose.offset(-11.0F, 7.0F, 6.0F)
        );
        frontChassis.addOrReplaceChild(
                "right_lift_cylinder",
                CubeListBuilder.create()
                        .texOffs(364, 84).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 18.0F)
                        .texOffs(364, 84).addBox(-1.0F, -1.0F, 16.0F, 2.0F, 2.0F, 12.0F),
                PartPose.offset(11.0F, 7.0F, 6.0F)
        );

        // ══════════════════════════════════════════════════════════════════════
        // 3. LOADER LIFT ARMS (Wysięgnik łamany w dół / Boomerang Lift Arms)
        // ══════════════════════════════════════════════════════════════════════
        PartDefinition liftArms = frontChassis.addOrReplaceChild(
                "lift_arms",
                CubeListBuilder.create()
                        // Left bent lift arm (ramię łamane w dół): UV [0, 174]
                        // Upper root beam (od wieży do kolana)
                        .texOffs(0, 174).addBox(-13.0F, -3.0F, 0.0F, 4.0F, 6.0F, 16.0F)
                        // Knee elbow joint with torque-tube boss
                        .texOffs(0, 174).addBox(-13.0F, 0.0F, 14.0F, 4.0F, 8.0F, 8.0F)
                        // Lower angled beam sloping downwards towards bucket
                        .texOffs(0, 174).addBox(-13.0F, 6.0F, 20.0F, 4.0F, 8.0F, 10.0F)
                        // Lower tip beam reaching the bucket hinge pins
                        .texOffs(0, 174).addBox(-13.0F, 14.0F, 28.0F, 4.0F, 7.0F, 8.0F)

                        // Right bent lift arm:
                        .texOffs(0, 174).addBox(9.0F, -3.0F, 0.0F, 4.0F, 6.0F, 16.0F)
                        .texOffs(0, 174).addBox(9.0F, 0.0F, 14.0F, 4.0F, 8.0F, 8.0F)
                        .texOffs(0, 174).addBox(9.0F, 6.0F, 20.0F, 4.0F, 8.0F, 10.0F)
                        .texOffs(0, 174).addBox(9.0F, 14.0F, 28.0F, 4.0F, 7.0F, 8.0F)

                        // Sturdy tubular cross-member at the knee elbow: UV [0, 174]
                        .texOffs(0, 174).addBox(-9.0F, 0.0F, 15.0F, 18.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, -5.0F, 8.0F)
        );

        // ── Z-Bar Linkage & Hydraulic Tilt Cylinder ──
        liftArms.addOrReplaceChild(
                "zbar_linkage",
                CubeListBuilder.create()
                        // Central bell crank pivot & arms: UV [364, 84]
                        .texOffs(364, 84).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 5.0F)
                        .texOffs(364, 84).addBox(-1.5F, -8.0F, -2.0F, 3.0F, 8.0F, 3.0F)
                        .texOffs(364, 84).addBox(-1.5F, 5.0F, 0.0F, 3.0F, 8.0F, 3.0F)
                        // Hydraulic tilt cylinder from frame
                        .texOffs(364, 84).addBox(-2.0F, 0.0F, -14.0F, 4.0F, 4.0F, 14.0F)
                        // Dog-bone push rod link to bucket
                        .texOffs(0, 174).addBox(-1.5F, 12.0F, 2.0F, 3.0F, 3.0F, 18.0F),
                PartPose.offset(0.0F, 0.0F, 16.0F)
        );

        // ══════════════════════════════════════════════════════════════════════
        // 4. HEAVY EXCAVATION SCOOP BUCKET (Łyżka ładowarki — otwarta pusta misa)
        // ══════════════════════════════════════════════════════════════════════
        PartDefinition bucket = liftArms.addOrReplaceChild(
                "bucket",
                CubeListBuilder.create()
                        // ── 1. Tylna ściana misy (Back curved wall): grubość 2.5px, wysokość 15px ──
                        .texOffs(184, 174).addBox(-22.0F, -8.0F, 0.0F, 44.0F, 12.0F, 2.5F)
                        // Skośne przejście dna do ściany tylnej (Curved heel transition)
                        .texOffs(184, 174).addBox(-22.0F, 4.0F, 1.0F, 44.0F, 2.0F, 4.0F)

                        // ── 2. Dno misy łyżki (Flat floor wear plate): grubość 2px, długość 13px (Z=5..18) ──
                        .texOffs(184, 174).addBox(-22.0F, 4.5F, 5.0F, 44.0F, 2.0F, 13.0F)

                        // ── 3. Hartowana krawędź tnąca / lemiesz czołowy (Cutting lip): grubość 2.5px (Z=18..22) ──
                        .texOffs(184, 174).addBox(-23.0F, 4.5F, 18.0F, 46.0F, 2.0F, 4.0F)

                        // ── 4. 6 kutych zębów skalnych (Forged rock teeth): UV [184, 174] ──
                        .texOffs(184, 174).addBox(-20.0F, 4.0F, 22.0F, 3.0F, 2.5F, 5.0F)
                        .texOffs(184, 174).addBox(-12.0F, 4.0F, 22.0F, 3.0F, 2.5F, 5.0F)
                        .texOffs(184, 174).addBox(-4.0F, 4.0F, 22.0F, 3.0F, 2.5F, 5.0F)
                        .texOffs(184, 174).addBox(4.0F, 4.0F, 22.0F, 3.0F, 2.5F, 5.0F)
                        .texOffs(184, 174).addBox(12.0F, 4.0F, 22.0F, 3.0F, 2.5F, 5.0F)
                        .texOffs(184, 174).addBox(20.0F, 4.0F, 22.0F, 3.0F, 2.5F, 5.0F)

                        // ── 5. Lewa ściana boczna (Left side cheek & cutter): cienka płyta 1.5px ──
                        .texOffs(184, 174).addBox(-23.5F, -9.0F, 0.0F, 1.5F, 15.5F, 20.0F)
                        // Boczny nóż ścinający (side cutter)
                        .texOffs(184, 174).addBox(-24.0F, 1.0F, 16.0F, 1.0F, 5.5F, 5.0F)

                        // ── 6. Prawa ściana boczna (Right side cheek & cutter): cienka płyta 1.5px ──
                        .texOffs(184, 174).addBox(22.0F, -9.0F, 0.0F, 1.5F, 15.5F, 20.0F)
                        .texOffs(184, 174).addBox(23.0F, 1.0F, 16.0F, 1.0F, 5.5F, 5.0F)

                        // ── 7. Daszek ochronny przeciw wysypywaniu urobku w tył (Top spill rock guard) ──
                        .texOffs(184, 174).addBox(-22.0F, -11.0F, -2.0F, 44.0F, 3.0F, 4.0F)
                        // Centralne ucho montażu siłownika Z-Bar
                        .texOffs(184, 174).addBox(-2.0F, -12.0F, 1.0F, 4.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, 19.5F, 34.0F)
        );

        // Bucket contents are rendered separately as a generated granular mound.
        // Keeping loose material out of the static entity model removes the old
        // cuboid surcharge and allows any Groundworks material texture to be used.

        return LayerDefinition.create(mesh, 512, 512);
    }

    /**
     * Original low-poly mining-loader body used by the giant variant.
     *
     * <p>The downloaded GLB was used only as a broad silhouette reference. No
     * source mesh, topology, UV layout, or texture is included here. Keeping
     * the same articulated part names lets both loader sizes share animation
     * code while giving the giant a visibly different high-cab, long-hood,
     * deep-bucket profile.</p>
     */
    public static LayerDefinition createGiantBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition rearChassis = root.addOrReplaceChild(
                "rear_chassis",
                CubeListBuilder.create()
                        // Deep mining frame and oversized rounded counterweight.
                        .texOffs(264, 0).addBox(-11.0F, 4.0F, -45.0F, 22.0F, 11.0F, 45.0F)
                        .texOffs(264, 0).addBox(-15.0F, -2.0F, -51.0F, 30.0F, 17.0F, 7.0F)
                        .texOffs(264, 0).addBox(-13.5F, -7.0F, -49.0F, 27.0F, 6.0F, 5.0F)
                        .texOffs(264, 0).addBox(-9.0F, 13.0F, -53.0F, 18.0F, 3.0F, 4.0F)
                        // Long stepped power module and radiator banks.
                        .texOffs(0, 84).addBox(-12.0F, -10.0F, -43.0F, 24.0F, 15.0F, 33.0F)
                        .texOffs(0, 84).addBox(-10.5F, -15.0F, -40.0F, 21.0F, 5.0F, 25.0F)
                        .texOffs(184, 84).addBox(-9.0F, -8.0F, -44.0F, 18.0F, 11.0F, 1.0F)
                        .texOffs(184, 84).addBox(-12.5F, -7.0F, -38.0F, 1.0F, 10.0F, 24.0F)
                        .texOffs(184, 84).addBox(11.5F, -7.0F, -38.0F, 1.0F, 10.0F, 24.0F)
                        // Twin stacks and air cleaner make the giant recognizable at range.
                        .texOffs(364, 164).addBox(6.0F, -28.0F, -23.0F, 3.0F, 14.0F, 3.0F)
                        .texOffs(364, 164).addBox(5.5F, -29.5F, -23.5F, 4.0F, 2.0F, 4.0F)
                        .texOffs(364, 164).addBox(-9.5F, -23.0F, -25.0F, 5.0F, 9.0F, 5.0F)
                        .texOffs(364, 164).addBox(-10.5F, -25.0F, -26.0F, 7.0F, 2.0F, 7.0F)
                        // Wide service decks over the rear tires.
                        .texOffs(0, 84).addBox(-22.0F, -5.0F, -43.0F, 10.0F, 3.0F, 31.0F)
                        .texOffs(0, 84).addBox(12.0F, -5.0F, -43.0F, 10.0F, 3.0F, 31.0F),
                PartPose.ZERO
        );

        // A separate sloped hood breaks the boxy standard-loader silhouette.
        rearChassis.addOrReplaceChild(
                "sloped_engine_hood",
                CubeListBuilder.create()
                        .texOffs(0, 84).addBox(-11.0F, -4.0F, -16.0F, 22.0F, 8.0F, 31.0F)
                        .texOffs(184, 84).addBox(-11.5F, -1.5F, -13.0F, 1.0F, 7.0F, 24.0F)
                        .texOffs(184, 84).addBox(10.5F, -1.5F, -13.0F, 1.0F, 7.0F, 24.0F),
                PartPose.offsetAndRotation(0.0F, -15.0F, -28.0F, -0.14F, 0.0F, 0.0F)
        );

        rearChassis.addOrReplaceChild(
                "rear_left_wheel",
                giantWheel(true),
                PartPose.offset(-17.0F, 9.0F, -33.0F)
        );
        rearChassis.addOrReplaceChild(
                "rear_right_wheel",
                giantWheel(false),
                PartPose.offset(17.0F, 9.0F, -33.0F)
        );

        PartDefinition cab = rearChassis.addOrReplaceChild(
                "cab",
                CubeListBuilder.create()
                        // Elevated panoramic mining cab.
                        .texOffs(264, 0).addBox(-12.0F, -4.0F, -11.0F, 24.0F, 4.0F, 21.0F)
                        .texOffs(0, 84).addBox(-14.0F, -32.0F, -12.0F, 28.0F, 4.0F, 23.0F)
                        .texOffs(0, 84).addBox(-13.0F, -28.0F, 8.0F, 3.0F, 24.0F, 3.0F)
                        .texOffs(0, 84).addBox(10.0F, -28.0F, 8.0F, 3.0F, 24.0F, 3.0F)
                        .texOffs(0, 84).addBox(-13.0F, -28.0F, -11.0F, 3.0F, 24.0F, 3.0F)
                        .texOffs(0, 84).addBox(10.0F, -28.0F, -11.0F, 3.0F, 24.0F, 3.0F)
                        .texOffs(364, 164).addBox(-9.5F, -8.0F, 8.0F, 19.0F, 5.0F, 2.0F)
                        // Operator seat, console, controls, mirrors and access stairs.
                        .texOffs(364, 164).addBox(-4.5F, -8.0F, -5.0F, 9.0F, 8.0F, 3.0F)
                        .texOffs(364, 164).addBox(-4.5F, -1.0F, -5.0F, 9.0F, 3.0F, 9.0F)
                        .texOffs(364, 164).addBox(-6.0F, -4.0F, 5.0F, 12.0F, 4.0F, 3.0F)
                        .texOffs(364, 164).addBox(-16.0F, -24.0F, 7.0F, 3.0F, 7.0F, 5.0F)
                        .texOffs(364, 164).addBox(13.0F, -24.0F, 7.0F, 3.0F, 7.0F, 5.0F)
                        .texOffs(264, 0).addBox(-18.0F, -2.0F, -7.0F, 5.0F, 17.0F, 12.0F)
                        .texOffs(0, 84).addBox(-17.0F, -18.0F, -8.0F, 1.5F, 17.0F, 1.5F)
                        .texOffs(0, 84).addBox(-17.0F, -18.0F, 5.0F, 1.5F, 17.0F, 1.5F)
                        .texOffs(0, 84).addBox(-17.0F, -18.0F, -8.0F, 1.5F, 1.5F, 14.5F),
                PartPose.offset(0.0F, -3.0F, 2.0F)
        );
        PartDefinition beaconBase = cab.addOrReplaceChild(
                "beacon_base",
                CubeListBuilder.create().texOffs(454, 0)
                        .addBox(-3.0F, -36.0F, -2.5F, 6.0F, 4.0F, 6.0F),
                PartPose.ZERO
        );
        beaconBase.addOrReplaceChild(
                "beacon_reflector",
                CubeListBuilder.create().texOffs(454, 0)
                        .addBox(-2.0F, -35.5F, -2.0F, 4.0F, 3.0F, 5.0F),
                PartPose.ZERO
        );

        PartDefinition frontChassis = root.addOrReplaceChild(
                "front_chassis",
                CubeListBuilder.create()
                        .texOffs(264, 0).addBox(-12.0F, 3.0F, -1.0F, 24.0F, 12.0F, 28.0F)
                        .texOffs(264, 0).addBox(-14.0F, 0.0F, 2.0F, 28.0F, 5.0F, 20.0F)
                        // Heavy pivot tower and articulated steering knuckle.
                        .texOffs(0, 84).addBox(-12.0F, -11.0F, 3.0F, 5.0F, 16.0F, 11.0F)
                        .texOffs(0, 84).addBox(7.0F, -11.0F, 3.0F, 5.0F, 16.0F, 11.0F)
                        .texOffs(0, 84).addBox(-8.0F, -5.0F, 6.0F, 16.0F, 5.0F, 7.0F)
                        .texOffs(364, 84).addBox(-14.0F, 9.0F, -3.0F, 5.0F, 4.0F, 10.0F)
                        .texOffs(364, 84).addBox(9.0F, 9.0F, -3.0F, 5.0F, 4.0F, 10.0F)
                        // Front service decks and work lamps.
                        .texOffs(0, 84).addBox(-22.0F, -5.0F, 6.0F, 10.0F, 3.0F, 25.0F)
                        .texOffs(0, 84).addBox(12.0F, -5.0F, 6.0F, 10.0F, 3.0F, 25.0F)
                        .texOffs(364, 164).addBox(-12.0F, -13.0F, 8.0F, 4.0F, 4.0F, 3.0F)
                        .texOffs(364, 164).addBox(8.0F, -13.0F, 8.0F, 4.0F, 4.0F, 3.0F),
                PartPose.ZERO
        );

        frontChassis.addOrReplaceChild(
                "front_left_wheel",
                giantWheel(true),
                PartPose.offset(-17.0F, 9.0F, 19.0F)
        );
        frontChassis.addOrReplaceChild(
                "front_right_wheel",
                giantWheel(false),
                PartPose.offset(17.0F, 9.0F, 19.0F)
        );

        frontChassis.addOrReplaceChild(
                "left_lift_cylinder",
                CubeListBuilder.create()
                        .texOffs(364, 84).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 22.0F)
                        .texOffs(364, 84).addBox(-1.25F, -1.25F, 20.0F, 2.5F, 2.5F, 16.0F),
                PartPose.offset(-12.0F, 4.0F, 7.0F)
        );
        frontChassis.addOrReplaceChild(
                "right_lift_cylinder",
                CubeListBuilder.create()
                        .texOffs(364, 84).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 22.0F)
                        .texOffs(364, 84).addBox(-1.25F, -1.25F, 20.0F, 2.5F, 2.5F, 16.0F),
                PartPose.offset(12.0F, 4.0F, 7.0F)
        );

        PartDefinition liftArms = frontChassis.addOrReplaceChild(
                "lift_arms",
                CubeListBuilder.create()
                        // Huge torque tube at the boom pivot.
                        .texOffs(0, 174).addBox(-14.0F, -4.0F, -2.0F, 28.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -5.0F, 8.0F)
        );

        // Long diagonal mining-loader arms instead of the standard stepped boom.
        liftArms.addOrReplaceChild(
                "left_diagonal_arm",
                CubeListBuilder.create()
                        .texOffs(0, 174).addBox(-3.0F, -3.5F, 0.0F, 6.0F, 7.0F, 40.0F)
                        .texOffs(0, 174).addBox(-3.5F, -4.5F, 28.0F, 7.0F, 9.0F, 12.0F),
                PartPose.offsetAndRotation(-11.0F, 0.0F, 2.0F, -0.43F, 0.0F, 0.0F)
        );
        liftArms.addOrReplaceChild(
                "right_diagonal_arm",
                CubeListBuilder.create()
                        .texOffs(0, 174).addBox(-3.0F, -3.5F, 0.0F, 6.0F, 7.0F, 40.0F)
                        .texOffs(0, 174).addBox(-3.5F, -4.5F, 28.0F, 7.0F, 9.0F, 12.0F),
                PartPose.offsetAndRotation(11.0F, 0.0F, 2.0F, -0.43F, 0.0F, 0.0F)
        );
        liftArms.addOrReplaceChild(
                "arm_crossmember",
                CubeListBuilder.create().texOffs(0, 174)
                        .addBox(-11.0F, -3.0F, -3.0F, 22.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 14.0F, 34.0F, -0.43F, 0.0F, 0.0F)
        );

        liftArms.addOrReplaceChild(
                "zbar_linkage",
                CubeListBuilder.create()
                        .texOffs(364, 84).addBox(-2.5F, -10.0F, -3.0F, 5.0F, 17.0F, 5.0F)
                        .texOffs(364, 84).addBox(-2.5F, 0.0F, -18.0F, 5.0F, 5.0F, 18.0F)
                        .texOffs(0, 174).addBox(-2.0F, 6.0F, 1.0F, 4.0F, 4.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 18.0F)
        );

        liftArms.addOrReplaceChild(
                "bucket",
                CubeListBuilder.create()
                        // Deep high-back rock bucket using the gameplay lip anchor.
                        .texOffs(184, 174).addBox(-22.0F, -13.0F, -2.0F, 44.0F, 17.0F, 3.0F)
                        .texOffs(184, 174).addBox(-22.0F, 3.0F, 0.0F, 44.0F, 3.5F, 6.0F)
                        .texOffs(184, 174).addBox(-22.0F, 4.5F, 5.0F, 44.0F, 2.0F, 13.0F)
                        .texOffs(184, 174).addBox(-23.0F, 4.5F, 18.0F, 46.0F, 2.5F, 4.0F)
                        // Side cheeks and spill guard.
                        .texOffs(184, 174).addBox(-24.0F, -14.0F, -2.0F, 2.0F, 20.5F, 23.0F)
                        .texOffs(184, 174).addBox(22.0F, -14.0F, -2.0F, 2.0F, 20.5F, 23.0F)
                        .texOffs(184, 174).addBox(-22.0F, -17.0F, -5.0F, 44.0F, 4.0F, 6.0F)
                        .texOffs(184, 174).addBox(-3.0F, -18.0F, -1.0F, 6.0F, 6.0F, 5.0F)
                        // Eight replaceable rock teeth.
                        .texOffs(184, 174).addBox(-22.0F, 4.0F, 22.0F, 3.0F, 3.0F, 6.0F)
                        .texOffs(184, 174).addBox(-16.0F, 4.0F, 22.0F, 3.0F, 3.0F, 6.0F)
                        .texOffs(184, 174).addBox(-10.0F, 4.0F, 22.0F, 3.0F, 3.0F, 6.0F)
                        .texOffs(184, 174).addBox(-4.0F, 4.0F, 22.0F, 3.0F, 3.0F, 6.0F)
                        .texOffs(184, 174).addBox(3.0F, 4.0F, 22.0F, 3.0F, 3.0F, 6.0F)
                        .texOffs(184, 174).addBox(9.0F, 4.0F, 22.0F, 3.0F, 3.0F, 6.0F)
                        .texOffs(184, 174).addBox(15.0F, 4.0F, 22.0F, 3.0F, 3.0F, 6.0F)
                        .texOffs(184, 174).addBox(21.0F, 4.0F, 22.0F, 3.0F, 3.0F, 6.0F),
                PartPose.offset(0.0F, 19.5F, 34.0F)
        );

        return LayerDefinition.create(mesh, 512, 512);
    }

    private static CubeListBuilder giantWheel(boolean left) {
        float rimX = left ? -7.5F : 4.5F;
        float hubX = left ? -8.5F : 6.0F;
        return CubeListBuilder.create()
                // Oversized 30 px mining tire: visibly larger than the standard wheel.
                .texOffs(0, 0).addBox(-7.0F, -15.0F, -6.0F, 14.0F, 30.0F, 12.0F)
                .texOffs(0, 0).addBox(-7.0F, -14.0F, -10.0F, 14.0F, 28.0F, 20.0F)
                .texOffs(0, 0).addBox(-7.0F, -11.5F, -13.0F, 14.0F, 23.0F, 26.0F)
                .texOffs(0, 0).addBox(-7.0F, -8.0F, -15.0F, 14.0F, 16.0F, 30.0F)
                .texOffs(164, 0).addBox(rimX, -9.0F, -9.0F, 3.0F, 18.0F, 18.0F)
                .texOffs(164, 0).addBox(rimX, -10.0F, -4.5F, 3.0F, 20.0F, 9.0F)
                .texOffs(164, 0).addBox(rimX, -4.5F, -10.0F, 3.0F, 9.0F, 20.0F)
                .texOffs(164, 0).addBox(hubX, -5.0F, -5.0F, 2.5F, 10.0F, 10.0F)
                .texOffs(164, 0).addBox(hubX, -3.0F, -7.0F, 2.5F, 6.0F, 14.0F);
    }

    @Override
    public void setupAnim(LoaderRenderState state) {
        // 1. Articulated Steering: Front chassis turns with vehicle steering
        float steerRad = (float) Math.toRadians(state.steerAngle);
        this.frontChassis.yRot = steerRad;

        // 2. Rolling Wheel Animation (degrees, inverted to roll forward)
        float wheelRad = (float) Math.toRadians(-state.wheelRotation);
        this.rearLeftWheel.xRot = wheelRad;
        this.rearRightWheel.xRot = wheelRad;
        this.frontLeftWheel.xRot = wheelRad;
        this.frontRightWheel.xRot = wheelRad;

        // 3. Boom Elevation (Arrow Up = raise, Arrow Down = lower)
        float boomRad = (float) Math.toRadians(state.boomAngle);
        this.liftArms.xRot = boomRad;

        // Hydraulic lift cylinders angle with the boom
        this.leftLiftCylinder.xRot = boomRad * 0.65F;
        this.rightLiftCylinder.xRot = boomRad * 0.65F;

        // 4. Bucket Tilt: positive angle = open (tilts down to dump), negative angle = close (curls up)
        float bucketRad = (float) Math.toRadians(-state.bucketAngle);
        this.bucket.xRot = bucketRad;

        // Z-Bar bell crank pivots proportionally as bucket tilts
        this.zbarLinkage.xRot = bucketRad * 0.50F;

        // 5. Loose material is rendered by GranularBucketContentsRenderer.
        // No material-ID-specific ModelParts are kept in the loader model.

        // 6. Amber Rotary Safety Beacon
        this.beaconReflector.yRot = state.beaconSpin;
        this.beaconReflector.visible = state.beaconFlash;
    }
}
