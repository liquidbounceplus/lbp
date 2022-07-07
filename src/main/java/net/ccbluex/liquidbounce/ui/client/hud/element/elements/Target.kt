/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.TeleportAura
import net.ccbluex.liquidbounce.features.module.modules.color.ColorMixer
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.ccbluex.liquidbounce.utils.AnimationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.BlendUtils
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.ShadowUtils
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.UiUtils
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.FontValue
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.MathHelper
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

/**
 * A target hud
 */
@ElementInfo(name = "Target", disableScale = true, retrieveDamage = true)
class Target : Element() {

    private val decimalFormat = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))
    private val decimalFormat2 = DecimalFormat("##0.0", DecimalFormatSymbols(Locale.ENGLISH))
    private val decimalFormat3 = DecimalFormat("0.#", DecimalFormatSymbols(Locale.ENGLISH))
    private val styleValue = ListValue("Style", arrayOf(Flux", "Novoline", "Exhibition", "Chill"), "Chill")
    private val fadeSpeed = FloatValue("FadeSpeed", 2F, 1F, 9F)
    private val chillFontSpeed = FloatValue("Chill-FontSpeed", 0.5F, 0.01F, 1F, { styleValue.get().equals("chill", true) })
    private val chillHealthBarValue = BoolValue("Chill-Healthbar", true, { styleValue.get().equals("chill", true) })
    private val chillFadingValue = BoolValue("Chill-FadingAnim", true, { styleValue.get().equals("chill", true) })
    private val showUrselfWhenChatOpen = BoolValue("DisplayWhenChat", true)
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = maxParticleSize.get()
            if (v < newValue) set(v)
        }
    }
    private val exhiFontValue = FontValue("Exhi-Font", Fonts.fontSFUI35, { styleValue.get().equals("exhibition", true) })
    private val colorModeValue = ListValue("Color", arrayOf("Custom", "Rainbow", "Sky", "LiquidSlowly", "Fade", "Mixer", "Health"), "Custom")
    private val redValue = IntegerValue("Red", 252, 0, 255)
    private val greenValue = IntegerValue("Green", 96, 0, 255)
    private val blueValue = IntegerValue("Blue", 66, 0, 255)
    private val saturationValue = FloatValue("Saturation", 1F, 0F, 1F)
    private val brightnessValue = FloatValue("Brightness", 1F, 0F, 1F)
    private val mixerSecondsValue = IntegerValue("Seconds", 2, 1, 10)
    private val backgroundColorRedValue = IntegerValue("Background-Red", 0, 0, 255)
    private val backgroundColorGreenValue = IntegerValue("Background-Green", 0, 0, 255)
    private val backgroundColorBlueValue = IntegerValue("Background-Blue", 0, 0, 255)
    private val backgroundColorAlphaValue = IntegerValue("Background-Alpha", 160, 0, 255)

    private val shieldIcon = ResourceLocation("liquidbounce+/shield.png")

    private var easingHealth: Float = 0F
    private var lastTarget: Entity? = null

    private val particleList = mutableListOf<Particle>()

    private var gotDamaged: Boolean = false

    private var progress: Float = 0F
    private var progressChill = 0F

    private var target: EntityPlayer? = null

    private val numberRenderer = CharRenderer(false)

    override fun drawElement(): Border {
        val kaTarget = (LiquidBounce.moduleManager[KillAura::class.java] as KillAura).target
        val taTarget = (LiquidBounce.moduleManager[TeleportAura::class.java] as TeleportAura).lastTarget

        val actualTarget = if (kaTarget != null && kaTarget is EntityPlayer) kaTarget 
                            else if (taTarget != null &&  taTarget is EntityPlayer) taTarget
                            else if ((mc.currentScreen is GuiChat && showUrselfWhenChatOpen.get()) || mc.currentScreen is GuiHudDesigner) mc.thePlayer 
                            else null

        val barColor = when (colorModeValue.get()) {
            "Rainbow" -> Color(RenderUtils.getRainbowOpaque(mixerSecondsValue.get(), saturationValue.get(), brightnessValue.get(), 0))
            "Custom" -> Color(redValue.get(), greenValue.get(), blueValue.get())
            "Sky" -> RenderUtils.skyRainbow(0, saturationValue.get(), brightnessValue.get())
            "Fade" -> ColorUtils.fade(Color(redValue.get(), greenValue.get(), blueValue.get()), 0, 100)
            "Health" -> if (actualTarget != null) BlendUtils.getHealthColor(actualTarget.health, actualTarget.maxHealth) else Color.green
            "Mixer" -> ColorMixer.getMixedColor(0, mixerSecondsValue.get())
            else -> ColorUtils.LiquidSlowly(System.nanoTime(), 0, saturationValue.get(), brightnessValue.get())!!
        }
        val bgColor = Color(backgroundColorRedValue.get(), backgroundColorGreenValue.get(), backgroundColorBlueValue.get(), backgroundColorAlphaValue.get())
        val borderColor = Color(borderColorRedValue.get(), borderColorGreenValue.get(), borderColorBlueValue.get(), borderColorAlphaValue.get())

        progress += 0.0025F * RenderUtils.deltaTime * if (actualTarget != null) -1F else 1F
        progressChill += 0.0075F * RenderUtils.deltaTime * if (actualTarget != null) -1F else 1F

        if (progress < 0F)
            progress = 0F
        else if (progress > 1F)
            progress = 1F

        if (progressChill < 0F || !chillFadingValue.get())
            progressChill = 0F
        else if (progressChill > 1F)
            progressChill = 1F

        if (styleValue.get().equals("chill", true)) {
            if (actualTarget == null && chillFadingValue.get()) {
                if (progressChill >= 1F && target != null) 
                    target = null
            } else 
                target = actualTarget
        } else {
            if (actualTarget == null && tSlideAnim.get()) {
                if (progress >= 1F && target != null) 
                    target = null
            } else 
                target = actualTarget
        }

        val animProgress = EaseUtils.easeInQuart(progress.toDouble())
        val tHeight = getTBorder().y2 - getTBorder().y

        if (tSlideAnim.get() && !styleValue.get().equals("chill", true)) {
            GL11.glPushMatrix()
            GL11.glTranslated(0.0, (-renderY - tHeight.toDouble()) * animProgress, 0.0)
        }

        if (target != null) {
            val convertedTarget = target!! as EntityPlayer
            when (styleValue.get()) {
                "Flux" -> {
                    val width = (26F + Fonts.fontSFUI40.getStringWidth(convertedTarget.name)).coerceAtLeast(26F + Fonts.fontSFUI35.getStringWidth("Health: ${decimalFormat2.format(convertedTarget.health)}")).toFloat() + 10F
                    RenderUtils.drawRoundedRect(-1F, -1F, 1F + width, 47F, 1F, Color(35, 35, 40, 230).rgb)
                    //RenderUtils.drawBorder(1F, 1F, 26F, 26F, 1F, Color(115, 255, 115).rgb)
                    if (mc.netHandler.getPlayerInfo(convertedTarget.uniqueID) != null) drawHead(mc.netHandler.getPlayerInfo(convertedTarget.uniqueID).locationSkin, 1, 1, 26, 26)
                    Fonts.fontSFUI40.drawString(convertedTarget.name, 30F, 5F, 0xFFFFFF) // Draw convertedTarget name
                    Fonts.fontSFUI35.drawString("Health: ${decimalFormat2.format(convertedTarget.health)}", 30F, 17.5F, 0xFFFFFF) // Draw convertedTarget health   

                    // bar icon
                    Fonts.fontSFUI35.drawString("❤", 2F, 29F, -1)
                    drawArmorIcon(2, 38, 7, 7)

                    easingHealth += ((convertedTarget.health - easingHealth) / Math.pow(2.0, 10.0 - 3.0)).toFloat() * RenderUtils.deltaTime.toFloat()

                    // bar bg
                    RenderUtils.drawRect(12F, 30F, 12F + width - 15F, 33F, Color(20, 20, 20, 255).rgb)
                    RenderUtils.drawRect(12F, 40F, 12F + width - 15F, 43F, Color(20, 20, 20, 255).rgb)

                    // Health bar
                    if (easingHealth < 0 || easingHealth > convertedTarget.maxHealth) {
                        easingHealth = convertedTarget.health.toFloat()
                    }
                    if (easingHealth > convertedTarget.health) {
                        RenderUtils.drawRect(12F, 30F, 12F + (easingHealth / convertedTarget.maxHealth) * (width - 15F), 33F, Color(231, 182, 0, 255).rgb)
                    } // Damage animation

                    RenderUtils.drawRect(12F, 30F, 12F + (convertedTarget.health / convertedTarget.maxHealth) * (width - 15F), 33F, Color(0, 224, 84, 255).rgb)

                    if (convertedTarget.getTotalArmorValue() != 0) {
                        RenderUtils.drawRect(12F, 40F, 12F + (convertedTarget.getTotalArmorValue() / 20F) * (width - 15F), 43F, Color(77, 128, 255, 255).rgb) // Draw armor bar
                    }
                }

                "Novoline" -> {
                    val font = Fonts.minecraftFont
                    val fontHeight = font.FONT_HEIGHT
                    val mainColor = barColor
                    val percent = convertedTarget.health.toFloat()/convertedTarget.maxHealth.toFloat() * 100F
                    val nameLength = (font.getStringWidth(convertedTarget.name)).coerceAtLeast(font.getStringWidth("${decimalFormat2.format(percent)}%")).toFloat() + 10F
                    val barWidth = (convertedTarget.health / convertedTarget.maxHealth).coerceIn(0F, convertedTarget.maxHealth.toFloat()) * (nameLength - 2F)

                    RenderUtils.drawRect(-2F, -2F, 3F + nameLength + 36F, 2F + 36F, Color(24, 24, 24, 255).rgb)
                    RenderUtils.drawRect(-1F, -1F, 2F + nameLength + 36F, 1F + 36F, Color(31, 31, 31, 255).rgb)
                    if (mc.netHandler.getPlayerInfo(convertedTarget.uniqueID) != null) drawHead(mc.netHandler.getPlayerInfo(convertedTarget.uniqueID).locationSkin, 0, 0, 36, 36)
                    font.drawStringWithShadow(convertedTarget.name, 2F + 36F + 1F, 2F, -1)
                    RenderUtils.drawRect(2F + 36F, 15F, 36F + nameLength, 25F, Color(24, 24, 24, 255).rgb)

                    easingHealth += ((convertedTarget.health - easingHealth) / 2.0F.pow(10.0F - fadeSpeed.get())) * RenderUtils.deltaTime

                    val animateThingy = (easingHealth.coerceIn(convertedTarget.health, convertedTarget.maxHealth) / convertedTarget.maxHealth) * (nameLength - 2F)

                    if (easingHealth > convertedTarget.health)
                        RenderUtils.drawRect(2F + 36F, 15F, 2F + 36F + animateThingy, 25F, mainColor.darker().rgb)
                    
                    RenderUtils.drawRect(2F + 36F, 15F, 2F + 36F + barWidth, 25F, mainColor.rgb)
                    
                    font.drawStringWithShadow("${decimalFormat2.format(percent)}%", 2F + 36F + (nameLength - 2F) / 2F - font.getStringWidth("${decimalFormat2.format(percent)}%").toFloat() / 2F, 16F, -1)
                }

                "Exhibition" -> {
                    val font = exhiFontValue.get()
                    val minWidth = 140F.coerceAtLeast(45F + font.getStringWidth(convertedTarget.name))

                    RenderUtils.drawExhiRect(0F, 0F, minWidth, 45F)

                    RenderUtils.drawRect(2.5F, 2.5F, 42.5F, 42.5F, Color(59, 59, 59).rgb)
                    RenderUtils.drawRect(3F, 3F, 42F, 42F, Color(19, 19, 19).rgb)

                    GL11.glColor4f(1f, 1f, 1f, 1f)
                    RenderUtils.drawEntityOnScreen(22, 40, 15, convertedTarget)

                    font.drawString(convertedTarget.name, 46, 4, -1)

                    val barLength = 60F * (convertedTarget.health / convertedTarget.maxHealth).coerceIn(0F, 1F)
                    RenderUtils.drawRect(45F, 14F, 45F + 60F, 17F, BlendUtils.getHealthColor(convertedTarget.health, convertedTarget.maxHealth).darker().darker().darker().rgb)
                    RenderUtils.drawRect(45F, 14F, 45F + barLength, 17F, BlendUtils.getHealthColor(convertedTarget.health, convertedTarget.maxHealth).rgb)

                    for (i in 0..9) {
                       RenderUtils.drawBorder(45F + i * 6F, 14F, 45F + (i + 1F) * 6F, 17F, 0.25F, Color.black.rgb)
                    }

                    GL11.glPushMatrix()
                    GL11.glTranslatef(46F, 20F, 0F)
                    GL11.glScalef(0.5f, 0.5f, 0.5f)
                    Fonts.minecraftFont.drawString("HP: ${convertedTarget.health.toInt()} | Dist: ${mc.thePlayer.getDistanceToEntityBox(convertedTarget).toInt()}", 0, 0, -1)
                    GL11.glPopMatrix()

                    GlStateManager.resetColor()

                    GL11.glPushMatrix()
                    GL11.glColor4f(1f, 1f, 1f, 1f)
                    RenderHelper.enableGUIStandardItemLighting()

                    val renderItem = mc.renderItem

                    var x = 45
                    var y = 26

                    for (index in 3 downTo 0) {
                        val stack = convertedTarget.inventory.armorInventory[index] ?: continue

                        if (stack.getItem() == null)
                            continue

                        renderItem.renderItemIntoGUI(stack, x, y)
                        renderItem.renderItemOverlays(mc.fontRendererObj, stack, x, y)

                        x += 18
                    }

                    val mainStack = convertedTarget.heldItem
                    if (mainStack != null && mainStack.getItem() != null) {
                        renderItem.renderItemIntoGUI(mainStack, x, y)
                        renderItem.renderItemOverlays(mc.fontRendererObj, mainStack, x, y)
                    }

                    RenderHelper.disableStandardItemLighting()
                    GlStateManager.enableAlpha()
                    GlStateManager.disableBlend()
                    GlStateManager.disableLighting()
                    GlStateManager.disableCull()
                    GL11.glPopMatrix()
                }

                "Chill" -> {
                    easingHealth += ((convertedTarget.health - easingHealth) / 2.0F.pow(10.0F - fadeSpeed.get())) * RenderUtils.deltaTime

                    val name = convertedTarget.name
                    val health = convertedTarget.health
                    val tWidth = (45F + Fonts.font40.getStringWidth(name).coerceAtLeast(Fonts.font72.getStringWidth(decimalFormat.format(health)))).coerceAtLeast(if (chillHealthBarValue.get()) 150F else 90F)
                    val playerInfo = mc.netHandler.getPlayerInfo(convertedTarget.uniqueID)

                    val reColorBg = Color(bgColor.red / 255.0F, bgColor.green / 255.0F, bgColor.blue / 255.0F, bgColor.alpha / 255.0F * (1F - progressChill))
                    val reColorBar = Color(barColor.red / 255.0F, barColor.green / 255.0F, barColor.blue / 255.0F, barColor.alpha / 255.0F * (1F - progressChill))
                    val reColorText = Color(1F, 1F, 1F, 1F - progressChill)

                    val floatX = renderX.toFloat()
                    val floatY = renderY.toFloat()

                    val calcScaleX = (progressChill * (4F / (tWidth / 2F)))
                    val calcScaleY = if (chillHealthBarValue.get()) (progressChill * (4F / 24F))
                                    else (progressChill * (4F / 19F))
                    val calcTranslateX = floatX + tWidth / 2F * calcScaleX
                    val calcTranslateY = floatY + if (chillHealthBarValue.get()) (24F * (progressChill * (4F / 24F))) 
                                                        else (19F * (progressChill * (4F / 19F)))

                    // translation/scaling
                    GL11.glScalef(1f, 1f, 1f)
                    GL11.glPopMatrix()

                    GL11.glPushMatrix()

                    if (chillFadingValue.get()) {
                        GL11.glTranslatef(
                            calcTranslateX, calcTranslateY, 0F)
                        GL11.glScalef(
                            1F - calcScaleX, 1F - calcScaleY, 1F - calcScaleX)
                    } else {
                        GL11.glTranslated(renderX, renderY, 0.0)
                        GL11.glScalef(1F, 1F, 1F)
                    }
                    
                    /*
                    some calculation
                    0.2 of 15 = 3 // 3/15 = 0.2
                    0.2 of 20 = 4 // 4/20 = 0.2
                    0.066 of 60 = 4 // 4/60 = 0.0(6)
                     */

                    // background
                    RenderUtils.drawRoundedRect(0F, 0F, tWidth, if (chillHealthBarValue.get()) 48F else 38F, 7F, reColorBg.rgb)
                    GlStateManager.resetColor()
                    GL11.glColor4f(1F, 1F, 1F, 1F)
                    
                    // head
                    if (playerInfo != null) {
                        Stencil.write(false)
                        GL11.glDisable(GL11.GL_TEXTURE_2D)
                        GL11.glEnable(GL11.GL_BLEND)
                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                        RenderUtils.fastRoundedRect(4F, 4F, 34F, 34F, 8F)
                        GL11.glDisable(GL11.GL_BLEND)
                        GL11.glEnable(GL11.GL_TEXTURE_2D)
                        Stencil.erase(true)
                        //GL11.glTranslated(renderX, renderY, 0.0)
                        drawHead(playerInfo.locationSkin, 4, 4, 30, 30, 1F - progressChill)
                        //GL11.glTranslated(-renderX, -renderY, 0.0)
                        Stencil.dispose()
                    }

                    GlStateManager.resetColor()
                    GL11.glColor4f(1F, 1F, 1F, 1F)

                    // name + health
                    Fonts.font40.drawString(name, 38F, 6F, reColorText.rgb, false)
                    numberRenderer.renderChar(health, calcTranslateX, calcTranslateY, 38F, 17F, calcScaleX, calcScaleY, false, chillFontSpeed.get(), reColorText.rgb)

                    GlStateManager.resetColor()
                    GL11.glColor4f(1F, 1F, 1F, 1F)
                    
                    // health bar
                    if (chillHealthBarValue.get()) {
                        RenderUtils.drawRoundedRect(4F, 38F, tWidth - 4F, 44F, 3F, reColorBar.darker().darker().darker().rgb)

                        Stencil.write(false)
                        GL11.glDisable(GL11.GL_TEXTURE_2D)
                        GL11.glEnable(GL11.GL_BLEND)
                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                        RenderUtils.fastRoundedRect(4F, 38F, tWidth - 4F, 44F, 3F)
                        GL11.glDisable(GL11.GL_BLEND)
                        Stencil.erase(true)
                        RenderUtils.drawRect(4F, 38F, 4F + (easingHealth / convertedTarget.maxHealth) * (tWidth - 8F), 44F, reColorBar.rgb)
                        Stencil.dispose()
                    }

                    GL11.glScalef(1F, 1F, 1F)
                    GL11.glPopMatrix()

                    GL11.glPushMatrix()
                    GL11.glTranslated(renderX, renderY, 0.0)
                }
            }
        } else if (target == null) {
            easingHealth = 0F
            gotDamaged = false
            particleList.clear()
        }

        if (tSlideAnim.get() && !styleValue.get().equals("chill", true))
            GL11.glPopMatrix()
            
        GlStateManager.resetColor()

        lastTarget = target
        return getTBorder()
    }

    override fun handleDamage(ent: EntityPlayer) {
        if (target != null && ent == target)
            gotDamaged = true
    }

    private fun getTBorder(): Border = when (styleValue.get()) {
            "Flux" -> Border(0F, -1F, 90F, 47F)
            "Novoline" -> Border(-1F, -2F, 90F, 38F)
            "Slowly" -> Border(0F, 0F, 90F, 36F)
            "Exhibition" -> Border(0F, 3F, 140F, 48F)
            "Chill" -> Border(0F, 0F, 110F, 46F)
            else -> Border(0F, 0F, 120F, 36F)
        }
    
    private class Particle(var color: Color, var distX: Float, var distY: Float, var radius: Float) {
        var alpha = 1F
        var progress = 0.0
        fun render(x: Float, y: Float, fade: Boolean, speed: Float, fadeSpeed: Float) {
            if (progress >= 1.0) {
                progress = 1.0
                if (fade) alpha -= (fadeSpeed * 0.02F * RenderUtils.deltaTime)
                if (alpha < 0F) alpha = 0F
            } else
                progress += (speed * 0.025F * RenderUtils.deltaTime).toDouble()

            if (alpha <= 0F) return

            var reColored = Color(color.red / 255.0F, color.green / 255.0F, color.blue / 255.0F, alpha)
            var easeOut = EaseUtils.easeOutQuart(progress).toFloat()

            RenderUtils.drawFilledCircle(x + distX * easeOut, y + distY * easeOut, radius, reColored)
        }
    }

    private class CharRenderer(val small: Boolean) {
        var moveY = FloatArray(20)
        var moveX = FloatArray(20)

        private val numberList = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".")

        private val deFormat = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))

        init {
            for (i in 0..19) {
                moveX[i] = 0F
                moveY[i] = 0F
            }
        }

        fun renderChar(number: Float, orgX: Float, orgY: Float, initX: Float, initY: Float, scaleX: Float, scaleY: Float, shadow: Boolean, fontSpeed: Float, color: Int): Float {
            val reFormat = deFormat.format(number.toDouble()) // string
            val fontRend = if (small) Fonts.font40 else Fonts.font72
            val delta = RenderUtils.deltaTime
            val scaledRes = ScaledResolution(mc)

            var indexX = 0
            var indexY = 0
            var animX = 0F

            val cutY = initY + fontRend.FONT_HEIGHT.toFloat() * (3F / 4F)

            GL11.glEnable(3089)
            RenderUtils.makeScissorBox(0F, orgY + initY - 4F * scaleY, scaledRes.getScaledWidth().toFloat(), orgY + cutY - 4F * scaleY)
            for (char in reFormat.toCharArray()) {
                moveX[indexX] = AnimationUtils.animate(animX, moveX[indexX], fontSpeed * 0.025F * delta)
                animX = moveX[indexX]

                val pos = numberList.indexOf("$char")
                val expectAnim = (fontRend.FONT_HEIGHT.toFloat() + 2F) * pos
                val expectAnimMin = (fontRend.FONT_HEIGHT.toFloat() + 2F) * (pos - 2)
                val expectAnimMax = (fontRend.FONT_HEIGHT.toFloat() + 2F) * (pos + 2)
                
                if (pos >= 0) {
                    moveY[indexY] = AnimationUtils.animate(expectAnim, moveY[indexY], fontSpeed * 0.02F * delta)

                    GL11.glTranslatef(0F, initY - moveY[indexY], 0F)
                    numberList.forEachIndexed { index, num ->
                        if ((fontRend.FONT_HEIGHT.toFloat() + 2F) * index >= expectAnimMin && (fontRend.FONT_HEIGHT.toFloat() + 2F) * index <= expectAnimMax) {
                            fontRend.drawString(num, initX + moveX[indexX], (fontRend.FONT_HEIGHT.toFloat() + 2F) * index, color, shadow)
                        }
                    }
                    GL11.glTranslatef(0F, -initY + moveY[indexY], 0F)
                } else {
                    moveY[indexY] = 0F
                    fontRend.drawString("$char", initX + moveX[indexX], initY, color, shadow)
                }

                animX += fontRend.getStringWidth("$char")
                indexX++
                indexY++
            }
            GL11.glDisable(3089)

            return animX
        }
    }

    private fun drawHead(skin: ResourceLocation, width: Int, height: Int) {
        GL11.glColor4f(1F, 1F, 1F, 1F)
        mc.textureManager.bindTexture(skin)
        Gui.drawScaledCustomSizeModalRect(2, 2, 8F, 8F, 8, 8, width, height,
                64F, 64F)
    }

    private fun drawHead(skin: ResourceLocation, x: Int, y: Int, width: Int, height: Int) {
        GL11.glColor4f(1F, 1F, 1F, 1F)
        mc.textureManager.bindTexture(skin)
        Gui.drawScaledCustomSizeModalRect(x, y, 8F, 8F, 8, 8, width, height,
                64F, 64F)
    }

    private fun drawHead(skin: ResourceLocation, x: Int, y: Int, width: Int, height: Int, alpha: Float) {
        GL11.glColor4f(1F, 1F, 1F, alpha)
        mc.textureManager.bindTexture(skin)
        Gui.drawScaledCustomSizeModalRect(x, y, 8F, 8F, 8, 8, width, height,
                64F, 64F)
    }

    private fun drawHead(skin: ResourceLocation, x: Float, y: Float, scale: Float, width: Int, height: Int, red: Float, green: Float, blue: Float) {
        GL11.glPushMatrix()
        GL11.glTranslatef(x, y, 0F)
        GL11.glScalef(scale, scale, scale)
        GL11.glColor4f(red.coerceIn(0F, 1F), green.coerceIn(0F, 1F), blue.coerceIn(0F, 1F), 1F)
        mc.textureManager.bindTexture(skin)
        Gui.drawScaledCustomSizeModalRect(0, 0, 8F, 8F, 8, 8, width, height,
                64F, 64F)
        GL11.glPopMatrix()
        GL11.glColor4f(1f, 1f, 1f, 1f)
    }

    private fun drawArmorIcon(x: Int, y: Int, width: Int, height: Int) {
        GlStateManager.disableAlpha()
        RenderUtils.drawImage(shieldIcon, x, y, width, height)
        GlStateManager.enableAlpha()
    }
}
