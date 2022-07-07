/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.misc.RandomUtils;
import net.ccbluex.liquidbounce.utils.PacketUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.utils.timer.TickTimer;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification;
import net.minecraft.block.BlockAir;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;

import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;

import java.awt.*;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.RoundingMode;

@ModuleInfo(name = "Fly", description = "Allows you to fly in survival mode.", category = ModuleCategory.MOVEMENT, keyBind = Keyboard.KEY_F)
public class Fly extends Module {

    public final ListValue modeValue = new ListValue("Mode", new String[]{
            "Motion",
            "Creative",
            "Damage",
            "Pearl",

            // NCP
            "NCP",
            "OldNCP",
            "Watchdog",
            "WatchdogTest",

            // FunCraft
            "FunCraft",

            // Rewinside
            "Rewinside",

            // Verus
            "Verus",
            "VerusLowHop",

            // Spartan
            "Spartan",
            "Spartan2",
            "BugSpartan",
            
            // AAC
            "AAC5-Vanilla",

            "Jetpack",
            "KeepAlive",
            "Clip",
            "Jump",
            "Derp",
            "Collide"
    }, "Motion");

    private final FloatValue vanillaSpeedValue = new FloatValue("Speed", 2F, 0F, 5F, () -> { 
        return (modeValue.get().equalsIgnoreCase("motion") || modeValue.get().equalsIgnoreCase("damage") || modeValue.get().equalsIgnoreCase("pearl") || modeValue.get().equalsIgnoreCase("aac5-vanilla") || modeValue.get().equalsIgnoreCase("bugspartan") || modeValue.get().equalsIgnoreCase("keepalive") || modeValue.get().equalsIgnoreCase("derp"));
    });
    private final FloatValue vanillaVSpeedValue = new FloatValue("V-Speed", 2F, 0F, 5F, () -> { return modeValue.get().equalsIgnoreCase("motion"); });
    private final FloatValue vanillaMotionYValue = new FloatValue("Y-Motion", 0F, -1F, 1F, () -> { return modeValue.get().equalsIgnoreCase("motion"); });
    private final BoolValue vanillaKickBypassValue = new BoolValue("KickBypass", false, () -> { return modeValue.get().equalsIgnoreCase("motion") || modeValue.get().equalsIgnoreCase("creative"); });

    private final BoolValue groundSpoofValue = new BoolValue("GroundSpoof", false, () -> { return modeValue.get().equalsIgnoreCase("motion") || modeValue.get().equalsIgnoreCase("creative"); });

    private final FloatValue ncpMotionValue = new FloatValue("NCPMotion", 0F, 0F, 1F, () -> { return modeValue.get().equalsIgnoreCase("ncp"); });

    // Verus
    private final ListValue verusDmgModeValue = new ListValue("Verus-DamageMode", new String[]{"None", "Instant", "InstantC06", "Jump"}, "None", () -> { return modeValue.get().equalsIgnoreCase("verus"); });
    private final ListValue verusBoostModeValue = new ListValue("Verus-BoostMode", new String[]{"Static", "Gradual"}, "Gradual", () -> { return modeValue.get().equalsIgnoreCase("verus") && !verusDmgModeValue.get().equalsIgnoreCase("none"); });
    private final BoolValue verusReDamageValue = new BoolValue("Verus-ReDamage", true, () -> { return modeValue.get().equalsIgnoreCase("verus") && !verusDmgModeValue.get().equalsIgnoreCase("none") && !verusDmgModeValue.get().equalsIgnoreCase("jump"); });
    private final IntegerValue verusReDmgTickValue = new IntegerValue("Verus-ReDamage-Ticks", 20, 0, 300, () -> { return modeValue.get().equalsIgnoreCase("verus") && !verusDmgModeValue.get().equalsIgnoreCase("none") && !verusDmgModeValue.get().equalsIgnoreCase("jump") && verusReDamageValue.get(); });
    private final BoolValue verusVisualValue = new BoolValue("Verus-VisualPos", false, () -> { return modeValue.get().equalsIgnoreCase("verus"); });
    private final FloatValue verusVisualHeightValue = new FloatValue("Verus-VisualHeight", 0.42F, 0F, 1F, () -> { return modeValue.get().equalsIgnoreCase("verus") && verusVisualValue.get(); });
    private final FloatValue verusSpeedValue = new FloatValue("Verus-Speed", 5F, 0F, 10F, () -> { return modeValue.get().equalsIgnoreCase("verus") && !verusDmgModeValue.get().equalsIgnoreCase("none"); });
    private final FloatValue verusTimerValue = new FloatValue("Verus-Timer", 1F, 0.1F, 10F, () -> { return modeValue.get().equalsIgnoreCase("verus") && !verusDmgModeValue.get().equalsIgnoreCase("none"); });
    private final IntegerValue verusDmgTickValue = new IntegerValue("Verus-Ticks", 20, 0, 300, () -> { return modeValue.get().equalsIgnoreCase("verus") && !verusDmgModeValue.get().equalsIgnoreCase("none"); });
    private final BoolValue verusSpoofGround = new BoolValue("Verus-SpoofGround", false, () -> { return modeValue.get().equalsIgnoreCase("verus"); });

    // AAC
    private final BoolValue aac5NoClipValue = new BoolValue("AAC5-NoClip", true, () -> { return modeValue.get().equalsIgnoreCase("aac5-vanilla"); });
    private final BoolValue aac5NofallValue = new BoolValue("AAC5-NoFall", true, () -> { return modeValue.get().equalsIgnoreCase("aac5-vanilla"); });
    private final BoolValue aac5UseC04Packet = new BoolValue("AAC5-UseC04", true, () -> { return modeValue.get().equalsIgnoreCase("aac5-vanilla"); });
    private final ListValue aac5Packet = new ListValue("AAC5-Packet", new String[]{"Original", "Rise", "Other"}, "Original", () -> { return modeValue.get().equalsIgnoreCase("aac5-vanilla"); }); // Original is from UnlegitMC/FDPClient.
    private final IntegerValue aac5PursePacketsValue = new IntegerValue("AAC5-Purse", 7, 3, 20, () -> { return modeValue.get().equalsIgnoreCase("aac5-vanilla"); });

    // Hypixel glide
    private final IntegerValue clipDelay = new IntegerValue("Clip-DelayTick", 25, 1, 50, () -> { return modeValue.get().equalsIgnoreCase("clip"); });
    private final FloatValue clipH = new FloatValue("Clip-Horizontal", 7.9F, 0, 10, () -> { return modeValue.get().equalsIgnoreCase("clip"); });
    private final FloatValue clipV = new FloatValue("Clip-Vertical", 1.75F, -10, 10, () -> { return modeValue.get().equalsIgnoreCase("clip"); });
    private final FloatValue clipMotionY = new FloatValue("Clip-MotionY", 0F, -2, 2, () -> { return modeValue.get().equalsIgnoreCase("clip"); });
    private final FloatValue clipTimer = new FloatValue("Clip-Timer", 1F, -0.08F, 10F, () -> { return modeValue.get().equalsIgnoreCase("clip"); });
    private final BoolValue clipGroundSpoof = new BoolValue("Clip-GroundSpoof", true, () -> { return modeValue.get().equalsIgnoreCase("clip"); });
    private final BoolValue clipCollisionCheck = new BoolValue("Clip-CollisionCheck", true, () -> { return modeValue.get().equalsIgnoreCase("clip"); });
    private final BoolValue clipNoMove = new BoolValue("Clip-NoMove", true, () -> { return modeValue.get().equalsIgnoreCase("clip"); });

    private final BoolValue fakeSprintingValue = new BoolValue("FakeSprinting", true, () -> { return modeValue.get().toLowerCase().contains("watchdog"); });
    private final BoolValue fakeNoMoveValue = new BoolValue("FakeNoMove", true, () -> { return modeValue.get().toLowerCase().contains("watchdog"); });
    private final BoolValue pulsiveTroll = new BoolValue("PulsiveTroll", true, () -> { return modeValue.get().equalsIgnoreCase("watchdogtest"); });

    // Visuals
    private final BoolValue fakeDmgValue = new BoolValue("FakeDamage", true);
    private final BoolValue bobbingValue = new BoolValue("Bobbing", true);
    private final FloatValue bobbingAmountValue = new FloatValue("BobbingAmount", 0.2F, 0F, 1F, () -> { return bobbingValue.get(); });
    private final BoolValue markValue = new BoolValue("Mark", true);

    private BlockPos lastPosition;

    private double startY;

    private final MSTimer groundTimer = new MSTimer();
    private final MSTimer boostTimer = new MSTimer();
    private final MSTimer wdTimer = new MSTimer();
    
    private final TickTimer spartanTimer = new TickTimer();
    private final TickTimer verusTimer = new TickTimer();

    private boolean shouldFakeJump, shouldActive = false;

    private boolean noPacketModify;

    private boolean noFlag;
    private int pearlState = 0;

    private boolean wasDead;

    private int boostTicks, dmgCooldown = 0;
    private int verusJumpTimes = 0;
    private int wdState, wdTick = 0;

    private boolean verusDmged, shouldActiveDmg = false;

    private float lastYaw, lastPitch;

    private double moveSpeed = 0.0;

    private void doMove(double h, double v) {
        if (mc.thePlayer == null) return;

        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;

        final double yaw = Math.toRadians(mc.thePlayer.rotationYaw);

        double expectedX = x + (-Math.sin(yaw) * h);
        double expectedY = y + v;
        double expectedZ = z + (Math.cos(yaw) * h);

        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(expectedX, expectedY, expectedZ, mc.thePlayer.onGround));
        mc.thePlayer.setPosition(expectedX, expectedY, expectedZ);
    }

    private void hClip(double x, double y, double z) {
        if (mc.thePlayer == null) return;

        double expectedX = mc.thePlayer.posX + x;
        double expectedY = mc.thePlayer.posY + y;
        double expectedZ = mc.thePlayer.posZ + z;

        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(expectedX, expectedY, expectedZ, mc.thePlayer.onGround));
        mc.thePlayer.setPosition(expectedX, expectedY, expectedZ);
    }

    private double[] getMoves(double h, double v) {
        if (mc.thePlayer == null) return new double[]{ 0.0, 0.0, 0.0 };

        final double yaw = Math.toRadians(mc.thePlayer.rotationYaw);

        double expectedX = (-Math.sin(yaw) * h);
        double expectedY = v;
        double expectedZ = (Math.cos(yaw) * h);

        return new double[] { expectedX, expectedY, expectedZ };
    }
    
    @Override
    public void onEnable() {
        if(mc.thePlayer == null)
            return;

        noPacketModify = true;

        verusTimer.reset();
        shouldFakeJump = false;
        shouldActive = true;

        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;

        lastYaw = mc.thePlayer.rotationYaw;
        lastPitch = mc.thePlayer.rotationPitch;

        final String mode = modeValue.get();

        boostTicks = 0;
        dmgCooldown = 0;
        pearlState = 0;

        verusJumpTimes = 0;
        verusDmged = false;

        moveSpeed = 0;
        wdState = 0;
        wdTick = 0;

        switch (mode.toLowerCase()) {
            case "ncp":
                mc.thePlayer.motionY = -ncpMotionValue.get();

                if(mc.gameSettings.keyBindSneak.isKeyDown())
                    mc.thePlayer.motionY = -0.5D;
                MovementUtils.strafe();
                break;
            case "oldncp":
                if(startY > mc.thePlayer.posY)
                    mc.thePlayer.motionY = -0.000000000000000000000000000000001D;

                if(mc.gameSettings.keyBindSneak.isKeyDown())
                    mc.thePlayer.motionY = -0.2D;

                if(mc.gameSettings.keyBindJump.isKeyDown() && mc.thePlayer.posY < (startY - 0.1D))
                    mc.thePlayer.motionY = 0.2D;
                MovementUtils.strafe();
                break;
            case "verus":
                if (verusDmgModeValue.get().equalsIgnoreCase("Instant")) {
                    if (mc.thePlayer.onGround && mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0, 4, 0).expand(0, 0, 0)).isEmpty()) {
                        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, y + 4, mc.thePlayer.posZ, false));
                        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, y, mc.thePlayer.posZ, false));
                        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, y, mc.thePlayer.posZ, true));
                        mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
                        if (verusReDamageValue.get()) dmgCooldown = verusReDmgTickValue.get();
                    }
                } else if (verusDmgModeValue.get().equalsIgnoreCase("InstantC06")) {
                    if (mc.thePlayer.onGround && mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0, 4, 0).expand(0, 0, 0)).isEmpty()) {
                        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, y + 4, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, false));
                        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, y, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, false));
                        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, y, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, true));
                        mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
                        if (verusReDamageValue.get()) dmgCooldown = verusReDmgTickValue.get();
                    }
                } else if (verusDmgModeValue.get().equalsIgnoreCase("Jump")) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                        verusJumpTimes = 1;
                    }
                } else {
                    // set dmged = true since there's no damage method
                    verusDmged = true;
                }
                if (verusVisualValue.get()) mc.thePlayer.setPosition(mc.thePlayer.posX, y + verusVisualHeightValue.get(), mc.thePlayer.posZ);
                shouldActiveDmg = dmgCooldown > 0;
                break;
            case "bugspartan":
                for(int i = 0; i < 65; ++i) {
                    PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.049D, z, false));
                    PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                }
                PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.1D, z, true));
                mc.thePlayer.motionX *= 0.1D;
                mc.thePlayer.motionZ *= 0.1D;
                mc.thePlayer.swingItem();
                break;
            case "funcraft":
                if (mc.thePlayer.onGround)
                    mc.thePlayer.jump();
                moveSpeed = 1;
                break;
            case "watchdogtest":
                if (mc.thePlayer.onGround)
                    mc.thePlayer.setPosition(x, y + 0.1, z);
                break;
        }

        startY = mc.thePlayer.posY;
        noPacketModify = false;

        if (!mode.equalsIgnoreCase("watchdog") && !mode.equalsIgnoreCase("watchdogtest") 
            && !mode.equalsIgnoreCase("bugspartan") && !mode.equalsIgnoreCase("verus") && !mode.equalsIgnoreCase("damage") 
            && fakeDmgValue.get()) {
            mc.thePlayer.handleStatusUpdate((byte) 2);
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        wasDead = false;

        if (mc.thePlayer == null)
            return;

        noFlag = false;

        final String mode = modeValue.get();

        if ((!mode.equalsIgnoreCase("Collide") && !mode.equalsIgnoreCase("Verus") && !mode.equalsIgnoreCase("Jump") && !mode.equalsIgnoreCase("creative")) || (mode.equalsIgnoreCase("pearl") && pearlState != -1)) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionY = 0;
            mc.thePlayer.motionZ = 0;
        }

        if (boostTicks > 0 && mode.equalsIgnoreCase("Verus")) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
        }

        if (mode.equalsIgnoreCase("AAC5-Vanilla") && !mc.isIntegratedServerRunning()) {
            sendAAC5Packets();
        }

        mc.thePlayer.capabilities.isFlying = false;

        mc.timer.timerSpeed = 1F;
        mc.thePlayer.speedInAir = 0.02F;
    }

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        final float vanillaSpeed = vanillaSpeedValue.get();
        final float vanillaVSpeed = vanillaVSpeedValue.get();

        mc.thePlayer.noClip = false;
        if (modeValue.get().equalsIgnoreCase("aac5-vanilla") && aac5NoClipValue.get())
            mc.thePlayer.noClip = true;

        switch (modeValue.get().toLowerCase()) {
            case "motion":
                mc.thePlayer.capabilities.isFlying = false;
                mc.thePlayer.motionY = vanillaMotionYValue.get();
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionZ = 0;
                if (mc.gameSettings.keyBindJump.isKeyDown())
                    mc.thePlayer.motionY += vanillaVSpeed;
                if (mc.gameSettings.keyBindSneak.isKeyDown())
                    mc.thePlayer.motionY -= vanillaVSpeed;
                MovementUtils.strafe(vanillaSpeed);
                handleVanillaKickBypass();
                break;
            case "ncp":
                mc.thePlayer.motionY = -ncpMotionValue.get();

                if(mc.gameSettings.keyBindSneak.isKeyDown())
                    mc.thePlayer.motionY = -0.5D;
                MovementUtils.strafe();
                break;
            case "oldncp":
                if(startY > mc.thePlayer.posY)
                    mc.thePlayer.motionY = -0.000000000000000000000000000000001D;

                if(mc.gameSettings.keyBindSneak.isKeyDown())
                    mc.thePlayer.motionY = -0.2D;

                if(mc.gameSettings.keyBindJump.isKeyDown() && mc.thePlayer.posY < (startY - 0.1D))
                    mc.thePlayer.motionY = 0.2D;
                MovementUtils.strafe();
                break;
            case "clip":
                mc.thePlayer.motionY = clipMotionY.get();
                mc.timer.timerSpeed = clipTimer.get();
                if (mc.thePlayer.ticksExisted % clipDelay.get() == 0) {
                    double[] expectMoves = getMoves((double)clipH.get(), (double)clipV.get());
                    if (!clipCollisionCheck.get() || mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(expectMoves[0], expectMoves[1], expectMoves[2]).expand(0, 0, 0)).isEmpty())
                        hClip(expectMoves[0], expectMoves[1], expectMoves[2]);
                }
                break;
            case "damage":
                mc.thePlayer.capabilities.isFlying = false;
                if (mc.thePlayer.hurtTime <= 0) break;
            case "derp":
            case "aac5-vanilla":
            case "bugspartan":
                mc.thePlayer.capabilities.isFlying = false;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionZ = 0;
                if (mc.gameSettings.keyBindJump.isKeyDown())
                    mc.thePlayer.motionY += vanillaSpeed;
                if (mc.gameSettings.keyBindSneak.isKeyDown())
                    mc.thePlayer.motionY -= vanillaSpeed;
                MovementUtils.strafe(vanillaSpeed);
                break;
            case "verus":
                mc.thePlayer.capabilities.isFlying = false;
                mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
                if (!verusDmgModeValue.get().equalsIgnoreCase("Jump") || shouldActiveDmg || verusDmged)
                    mc.thePlayer.motionY = 0;

                if (verusDmgModeValue.get().equalsIgnoreCase("Jump") && verusJumpTimes < 5) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                        verusJumpTimes += 1;
                    }
                    return;
                }

                if (shouldActiveDmg) {
                    if (dmgCooldown > 0) 
                        dmgCooldown--;
                    else if (verusDmged) {
                        verusDmged = false;
                        double y = mc.thePlayer.posY;
                        if (verusDmgModeValue.get().equalsIgnoreCase("Instant")) {
                            if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0, 4, 0).expand(0, 0, 0)).isEmpty()) {
                                PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, y + 4, mc.thePlayer.posZ, false));
                                PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, y, mc.thePlayer.posZ, false));
                                PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, y, mc.thePlayer.posZ, true));
                                mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
                            }
                        } else if (verusDmgModeValue.get().equalsIgnoreCase("InstantC06")) {
                            if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0, 4, 0).expand(0, 0, 0)).isEmpty()) {
                                PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, y + 4, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, false));
                                PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, y, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, false));
                                PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, y, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, true));
                                mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
                            }
                        }
                        dmgCooldown = verusReDmgTickValue.get();
                    }
                }

                if (!verusDmged && mc.thePlayer.hurtTime > 0) {
                    verusDmged = true;
                    boostTicks = verusDmgTickValue.get();
                }

                if (boostTicks > 0) {
                    mc.timer.timerSpeed = verusTimerValue.get();
                    float motion = 0F;
                    
                    if (verusBoostModeValue.get().equalsIgnoreCase("static")) motion = verusSpeedValue.get(); else motion = ((float)boostTicks / (float)verusDmgTickValue.get()) * verusSpeedValue.get();
                    boostTicks--;

                    MovementUtils.strafe(motion);
                } else if (verusDmged) {
                    mc.timer.timerSpeed = 1F;
                    MovementUtils.strafe((float)MovementUtils.getBaseMoveSpeed() * 0.6F);
                } else {
                    mc.thePlayer.movementInput.moveForward = 0F;
                    mc.thePlayer.movementInput.moveStrafe = 0F;
                }
                break;
            case "creative":
                mc.thePlayer.capabilities.isFlying = true;

                handleVanillaKickBypass();
                break;
            case "keepalive":
                PacketUtils.sendPacketNoEvent(new C00PacketKeepAlive());

                mc.thePlayer.capabilities.isFlying = false;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionZ = 0;
                if(mc.gameSettings.keyBindJump.isKeyDown())
                    mc.thePlayer.motionY += vanillaSpeed;
                if(mc.gameSettings.keyBindSneak.isKeyDown())
                    mc.thePlayer.motionY -= vanillaSpeed;
                MovementUtils.strafe(vanillaSpeed);
                break;
            case "jetpack":
                if(mc.gameSettings.keyBindJump.isKeyDown()) {
                    mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.FLAME.getParticleID(), mc.thePlayer.posX, mc.thePlayer.posY + 0.2D, mc.thePlayer.posZ, -mc.thePlayer.motionX, -0.5D, -mc.thePlayer.motionZ);
                    mc.thePlayer.motionY += 0.15D;
                    mc.thePlayer.motionX *= 1.1D;
                    mc.thePlayer.motionZ *= 1.1D;
                }
                break;
            case "spartan":
                mc.thePlayer.motionY = 0;
                spartanTimer.update();
                if(spartanTimer.hasTimePassed(12)) {
                    PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 8, mc.thePlayer.posZ, true));
                    PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY - 8, mc.thePlayer.posZ, true));
                    spartanTimer.reset();
                }
                break;
            case "spartan2":
                MovementUtils.strafe(0.264F);

                if(mc.thePlayer.ticksExisted % 8 == 0)
                    mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 10, mc.thePlayer.posZ, true));
                break;
            case "pearl":
                mc.thePlayer.capabilities.isFlying = false;
                mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0;

                int enderPearlSlot = getPearlSlot();
                if (pearlState == 0) {
                    if (enderPearlSlot == -1) {
                        LiquidBounce.hud.addNotification(new Notification("You don't have any ender pearl!", Notification.Type.ERROR));
                        pearlState = -1;
                        this.setState(false);
                        return;
                    }

                    if (mc.thePlayer.inventory.currentItem != enderPearlSlot) {
                        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(enderPearlSlot));
                    }

                    mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(mc.thePlayer.rotationYaw, 90, mc.thePlayer.onGround));
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 255, mc.thePlayer.inventoryContainer.getSlot(enderPearlSlot + 36).getStack(), 0, 0, 0));
                    if (enderPearlSlot != mc.thePlayer.inventory.currentItem) {
                        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                    }
                    pearlState = 1;    
                }

                if (pearlState == 1 && mc.thePlayer.hurtTime > 0) 
                    pearlState = 2;

                if (pearlState == 2) {
                    if (mc.gameSettings.keyBindJump.isKeyDown())
                        mc.thePlayer.motionY += vanillaSpeed;
                    if (mc.gameSettings.keyBindSneak.isKeyDown())
                        mc.thePlayer.motionY -= vanillaSpeed;
                    MovementUtils.strafe(vanillaSpeed);
                }
                break;
            case "jump":
                if (mc.thePlayer.onGround)
                    mc.thePlayer.jump();
                break;
            case "watchdog":
                if (wdState == 0) {
                    mc.thePlayer.motionY = 0.1D;
                    wdState++;
                }

                if (wdState == 1 && wdTick == 3)
                    wdState++;

                if (wdState == 4) {
                    if (!boostTimer.hasTimePassed(500L))
                        mc.timer.timerSpeed = 1.6F;
                    else if (!boostTimer.hasTimePassed(800L))
                        mc.timer.timerSpeed = 1.4F;
                    else if (!boostTimer.hasTimePassed(1000L))
                        mc.timer.timerSpeed = 1.2F;
                    else
                        mc.timer.timerSpeed = 1F;

                    mc.thePlayer.motionY = 0.0001D;
                    MovementUtils.strafe((float) (MovementUtils.getBaseMoveSpeed() * (mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 0.81D : 0.77D)));
                }
                break;
            case "watchdogtest":
                if (wdState == 3) {
                    mc.timer.timerSpeed = 1F;
                    mc.thePlayer.motionY = 0.0001D;
                    MovementUtils.strafe((float) (MovementUtils.getBaseMoveSpeed() * (mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 0.8D : 0.75D)));
                }
                else if (wdState == 2) {
                    mc.timer.timerSpeed = 1.5F;
                    mc.thePlayer.motionY = 0.0001D;
                    MovementUtils.strafe((float) (MovementUtils.getBaseMoveSpeed() * (mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 0.81D : 0.77D)));
                } 
                break;
        }
    }

    @EventTarget // drew
    public void onMotion(final MotionEvent event) {
        if (mc.thePlayer == null) return;

        if (bobbingValue.get()) {
            mc.thePlayer.cameraYaw = bobbingAmountValue.get();
            mc.thePlayer.prevCameraYaw = bobbingAmountValue.get();
        }

        switch (modeValue.get().toLowerCase()) {
            case "funcraft":
                event.setOnGround(true);
                if (!MovementUtils.isMoving())
                    moveSpeed = 0.25;
                if (moveSpeed > 0.25) {
                    moveSpeed -= moveSpeed / 159.0;
                }
                if (event.getEventState() == EventState.PRE) {
                    mc.thePlayer.capabilities.isFlying = false;
                    mc.thePlayer.motionY = 0;
                    mc.thePlayer.motionX = 0;
                    mc.thePlayer.motionZ = 0;

                    MovementUtils.strafe((float)moveSpeed);
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - 8e-6, mc.thePlayer.posZ);
                }
                break;
            case "watchdog":
                if (event.getEventState() == EventState.PRE)
                    wdTick++;
                break;
            case "watchdogtest":
                if (event.getEventState() == EventState.POST && pulsiveTroll.get() && wdState >= 2 && mc.thePlayer.ticksExisted % 2 == 0) 
                    mc.getNetHandler().addToSendQueue(new C0CPacketInput(coerceAtMost(mc.thePlayer.moveStrafing, 0.98F), coerceAtMost(mc.thePlayer.moveForward, 0.98F), mc.thePlayer.movementInput.jump, mc.thePlayer.movementInput.sneak));
                break;
        }  
    }

    public float coerceAtMost(double value, double max) {
        return (float) Math.min(value, max);
    }

    @EventTarget
    public void onAction(final ActionEvent event) {
        if (modeValue.get().toLowerCase().contains("watchdog") && fakeSprintingValue.get())
            event.setSprinting(false);
    }

    @EventTarget
    public void onRender3D(final Render3DEvent event) {
        final String mode = modeValue.get();

        if (!markValue.get() || mode.equalsIgnoreCase("Motion") || mode.equalsIgnoreCase("Creative") || mode.equalsIgnoreCase("Damage") || mode.equalsIgnoreCase("AAC5-Vanilla") || mode.equalsIgnoreCase("Derp") || mode.equalsIgnoreCase("KeepAlive"))
            return;

        double y = startY + 2D;

        RenderUtils.drawPlatform(y, mc.thePlayer.getEntityBoundingBox().maxY < y ? new Color(0, 255, 0, 90) : new Color(255, 0, 0, 90), 1);
    }

    @EventTarget
    public void onRender2D(final Render2DEvent event) {
        final String mode = modeValue.get();
        ScaledResolution scaledRes = new ScaledResolution(mc);
        if (mode.equalsIgnoreCase("Verus") && boostTicks > 0) {
            float width = (float)(verusDmgTickValue.get() - boostTicks) / (float)verusDmgTickValue.get() * 60F;
            RenderUtils.drawRect(scaledRes.getScaledWidth() / 2F - 31F, scaledRes.getScaledHeight() / 2F + 14F, scaledRes.getScaledWidth() / 2F + 31F, scaledRes.getScaledHeight() / 2F + 18F, 0xA0000000);
            RenderUtils.drawRect(scaledRes.getScaledWidth() / 2F - 30F, scaledRes.getScaledHeight() / 2F + 15F, scaledRes.getScaledWidth() / 2F - 30F + width, scaledRes.getScaledHeight() / 2F + 17F, 0xFFFFFFFF);
        }
        if (mode.equalsIgnoreCase("Verus") && shouldActiveDmg) {
            float width = (float)(verusReDmgTickValue.get() - dmgCooldown) / (float)verusReDmgTickValue.get() * 60F;
            RenderUtils.drawRect(scaledRes.getScaledWidth() / 2F - 31F, scaledRes.getScaledHeight() / 2F + 14F + 10F, scaledRes.getScaledWidth() / 2F + 31F, scaledRes.getScaledHeight() / 2F + 18F + 10F, 0xA0000000);
            RenderUtils.drawRect(scaledRes.getScaledWidth() / 2F - 30F, scaledRes.getScaledHeight() / 2F + 15F + 10F, scaledRes.getScaledWidth() / 2F - 30F + width, scaledRes.getScaledHeight() / 2F + 17F + 10F, 0xFFFF1F1F);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        final Packet<?> packet = event.getPacket();
        final String mode = modeValue.get();

        if(noPacketModify)
            return;

        if (packet instanceof S08PacketPlayerPosLook && mode.equalsIgnoreCase("watchdog") && wdState == 3) {
            wdState = 4;
            if (boostTimer.hasTimePassed(8000L)) {
                LiquidBounce.hud.addNotification(new Notification("Enabled boost.", Notification.Type.SUCCESS));
                boostTimer.reset();
            } else {
                LiquidBounce.hud.addNotification(new Notification("Disabled boost to prevent flagging.", Notification.Type.WARNING));
            }

            if (fakeDmgValue.get() && mc.thePlayer != null)
                mc.thePlayer.handleStatusUpdate((byte) 2);
        }

        if (mode.equalsIgnoreCase("WatchdogTest") && packet instanceof S08PacketPlayerPosLook) {
            if (wdState == 1) {
                wdState = 2;
                LiquidBounce.hud.addNotification(new Notification("Activated.", Notification.Type.SUCCESS));

                if (fakeDmgValue.get() && mc.thePlayer != null)
                    mc.thePlayer.handleStatusUpdate((byte) 2);
            } else if (wdState == 2) {
                wdState = 3;
                LiquidBounce.hud.addNotification(new Notification("Flagged.", Notification.Type.INFO));
            }
        }

        if (packet instanceof C03PacketPlayer) {
            final C03PacketPlayer packetPlayer = (C03PacketPlayer) packet;

            boolean lastOnGround = packetPlayer.onGround;

            if (mode.equalsIgnoreCase("NCP") || mode.equalsIgnoreCase("Rewinside") || (mode.equalsIgnoreCase("Verus") && verusSpoofGround.get() && verusDmged))
                packetPlayer.onGround = true;

            if (mode.equalsIgnoreCase("Derp")) {
                packetPlayer.yaw = RandomUtils.nextFloat(0F, 360F);
                packetPlayer.pitch = RandomUtils.nextFloat(-90F, 90F);
            }

            if (mode.equalsIgnoreCase("AAC5-Vanilla") && !mc.isIntegratedServerRunning()) {
                if (aac5NofallValue.get()) packetPlayer.onGround = true;
                aac5C03List.add(packetPlayer);
                event.cancelEvent();
                if(aac5C03List.size()>aac5PursePacketsValue.get())
                    sendAAC5Packets();
            }

            if (mode.equalsIgnoreCase("clip") && clipGroundSpoof.get())
                packetPlayer.onGround = true;

            if ((mode.equalsIgnoreCase("motion") || mode.equalsIgnoreCase("creative")) && groundSpoofValue.get())
                packetPlayer.onGround = true;

            if (verusDmgModeValue.get().equalsIgnoreCase("Jump") && verusJumpTimes < 5 && mode.equalsIgnoreCase("Verus")) {
                packetPlayer.onGround = false;
            }

            if (mode.equalsIgnoreCase("watchdog")) {
                if (wdState == 2) {
                    packetPlayer.y -= 0.187;
                    wdState++;
                }
                if (wdState > 3) {
                    if (fakeNoMoveValue.get())
                        packetPlayer.setMoving(false);
                }
            }

            if (mode.equalsIgnoreCase("watchdogtest")) {
                if (wdState == 0 && packetPlayer.onGround) {
                    packetPlayer.y -= 0.187;
                    wdState = 1;
                } else {
                    packetPlayer.y = startY;
                    if (fakeNoMoveValue.get())
                        packetPlayer.setMoving(false);
                }
            }
        }
    }

    private final ArrayList<C03PacketPlayer> aac5C03List=new ArrayList<>();

    private void sendAAC5Packets(){
        float yaw = mc.thePlayer.rotationYaw;
        float pitch = mc.thePlayer.rotationPitch;
        for (C03PacketPlayer packet : aac5C03List) {
            PacketUtils.sendPacketNoEvent(packet);
            if (packet.isMoving()) {
                if (packet.getRotating()) {
                    yaw = packet.yaw;
                    pitch = packet.pitch;
                }
                switch (aac5Packet.get()) {
                    case "Original":
                        if (aac5UseC04Packet.get()) {
                            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(packet.x, 1e+159, packet.z, true));
                            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(packet.x,packet.y,packet.z, true));
                        } else {
                            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(packet.x, 1e+159, packet.z, yaw, pitch, true));
                            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(packet.x,packet.y,packet.z, yaw, pitch, true));
                        }
                        break;
                    case "Rise":
                        if (aac5UseC04Packet.get()) {
                            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(packet.x, -1e+159, packet.z+10, true));
                            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(packet.x,packet.y,packet.z, true));
                        } else {
                            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(packet.x, -1e+159, packet.z+10, yaw, pitch, true));
                            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(packet.x,packet.y,packet.z, yaw, pitch, true));
                        }
                        break;
                    case "Other":
                        if (aac5UseC04Packet.get()) {
                            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(packet.x, 1.7976931348623157E+308, packet.z, true));
                            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(packet.x,packet.y,packet.z, true));
                        } else {
                            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(packet.x, 1.7976931348623157E+308, packet.z, yaw, pitch, true));
                            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(packet.x,packet.y,packet.z, yaw, pitch, true));
                        }
                        break;
                }
                
            }
        }
        aac5C03List.clear();
    }

    @EventTarget
    public void onMove(final MoveEvent event) {
        /*final float vanillaSpeed = vanillaSpeedValue.get();
        final TargetStrafe targetStrafe = (TargetStrafe) LiquidBounce.moduleManager.getModule(TargetStrafe.class);
        if (targetStrafe == null) 
            return;*/
        
        switch(modeValue.get().toLowerCase()) {
            case "pearl":
                if (pearlState != 2 && pearlState != -1) {
                    event.cancelEvent();
                }
                break;
            case "verus": 
                if (!verusDmged)
                    if (verusDmgModeValue.get().equalsIgnoreCase("Jump"))
                        event.zeroXZ();
                    else
                        event.cancelEvent();
                break;
            case "clip":
                if (clipNoMove.get()) event.zeroXZ();
                break;
            case "veruslowhop":
                if (!mc.thePlayer.isInWeb && !mc.thePlayer.isInLava() && !mc.thePlayer.isInWater() && !mc.thePlayer.isOnLadder() && !mc.gameSettings.keyBindJump.isKeyDown() && mc.thePlayer.ridingEntity == null) {
                    if (MovementUtils.isMoving()) {
                        mc.gameSettings.keyBindJump.pressed = false;
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.jump();
                            mc.thePlayer.motionY = 0;
                            MovementUtils.strafe(0.61F);
                            event.setY(0.41999998688698);
                        }
                        MovementUtils.strafe();
                    }
                }
                break;
            case "watchdog":
                if (wdState < 4)
                    event.zeroXZ();
                break;
            case "watchdogtest":
                if (wdState < 2)
                    event.zeroXZ();
                break;
        }
    }

    @EventTarget
    public void onBB(final BlockBBEvent event) {
        if (mc.thePlayer == null) return;

        final String mode = modeValue.get();

        if (event.getBlock() instanceof BlockAir && mode.equalsIgnoreCase("Jump") && event.getY() < startY) {
            event.setBoundingBox(AxisAlignedBB.fromBounds(event.getX(), event.getY(), event.getZ(), event.getX() + 1, startY, event.getZ() + 1));
        }

        if (event.getBlock() instanceof BlockAir && ((mode.equalsIgnoreCase("collide") && !mc.thePlayer.isSneaking()) || mode.equalsIgnoreCase("veruslowhop")))
            event.setBoundingBox(new AxisAlignedBB(-2, -1, -2, 2, 1, 2).offset(event.getX(), event.getY(), event.getZ()));

        if (event.getBlock() instanceof BlockAir && (mode.equalsIgnoreCase("Rewinside") || (mode.equalsIgnoreCase("Verus") && 
            (verusDmgModeValue.get().equalsIgnoreCase("none") || verusDmged)))
            && event.getY() < mc.thePlayer.posY)
            event.setBoundingBox(AxisAlignedBB.fromBounds(event.getX(), event.getY(), event.getZ(), event.getX() + 1, mc.thePlayer.posY, event.getZ() + 1));
    }

    @EventTarget
    public void onJump(final JumpEvent e) {
        final String mode = modeValue.get();

        if (mode.equalsIgnoreCase("Rewinside") || (mode.equalsIgnoreCase("FunCraft") && moveSpeed > 0) || (mode.equalsIgnoreCase("watchdog") && wdState >= 1) || mode.equalsIgnoreCase("watchdogtest"))
            e.cancelEvent();
    }

    @EventTarget
    public void onStep(final StepEvent e) {
        final String mode = modeValue.get();

        if (mode.equalsIgnoreCase("Rewinside") || mode.equalsIgnoreCase("FunCraft") || (mode.equalsIgnoreCase("watchdog") && wdState > 2) || mode.equalsIgnoreCase("watchdogtest"))
            e.setStepHeight(0F);
    }

    private void handleVanillaKickBypass() {
        if(!vanillaKickBypassValue.get() || !groundTimer.hasTimePassed(1000)) return;

        final double ground = calculateGround();

        for(double posY = mc.thePlayer.posY; posY > ground; posY -= 8D) {
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, posY, mc.thePlayer.posZ, true));

            if(posY - 8D < ground) break; // Prevent next step
        }

        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, ground, mc.thePlayer.posZ, true));


        for(double posY = ground; posY < mc.thePlayer.posY; posY += 8D) {
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, posY, mc.thePlayer.posZ, true));

            if(posY + 8D > mc.thePlayer.posY) break; // Prevent next step
        }

        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));

        groundTimer.reset();
    }

    // TODO: Make better and faster calculation lol
    private double calculateGround() {
        final AxisAlignedBB playerBoundingBox = mc.thePlayer.getEntityBoundingBox();
        double blockHeight = 1D;

        for(double ground = mc.thePlayer.posY; ground > 0D; ground -= blockHeight) {
            final AxisAlignedBB customBox = new AxisAlignedBB(playerBoundingBox.maxX, ground + blockHeight, playerBoundingBox.maxZ, playerBoundingBox.minX, ground, playerBoundingBox.minZ);

            if(mc.theWorld.checkBlockCollision(customBox)) {
                if(blockHeight <= 0.05D)
                    return ground + blockHeight;

                ground += blockHeight;
                blockHeight = 0.05D;
            }
        }

        return 0F;
    }

    // Hypixel things
    private int getPearlSlot() {
        for(int i = 36; i < 45; ++i) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemEnderPearl) {
                return i - 36;
            }
        }
        return -1;
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }
}
