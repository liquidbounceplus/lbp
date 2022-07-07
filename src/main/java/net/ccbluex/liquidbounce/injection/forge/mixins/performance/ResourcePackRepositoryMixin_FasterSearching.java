/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.performance;

import net.ccbluex.liquidbounce.features.module.modules.misc.Patcher;
import net.ccbluex.liquidbounce.patcher.hooks.misc.ResourcePackRepositoryHook;
import net.minecraft.client.resources.ResourcePackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResourcePackRepository.class)
public class ResourcePackRepositoryMixin_FasterSearching {

    @Inject(method = "updateRepositoryEntriesAll", at = @At("HEAD"), cancellable = true)
    private void patcher$searchUsingSet(CallbackInfo ci) {
        if (Patcher.labyModMoment.get()) {
            // todo: move this hook into this class
            //  the funky "repository.new Entry(file)" line has me stumped
            ResourcePackRepositoryHook.updateRepositoryEntriesAll((ResourcePackRepository) (Object) this);
            ci.cancel();
        }
    }
}
