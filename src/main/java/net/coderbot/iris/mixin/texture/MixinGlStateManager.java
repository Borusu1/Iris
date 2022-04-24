package net.coderbot.iris.mixin.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import net.coderbot.iris.texture.TextureTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public class MixinGlStateManager {
	@Inject(method = "_deleteTexture(I)V", at = @At("TAIL"), remap = false)
	private static void iris$onDeleteTexture(int id, CallbackInfo ci) {
		TextureTracker.INSTANCE.onDeleteTexture(id);
	}

	@Inject(method = "_deleteTextures([I)V", at = @At("TAIL"), remap = false)
	private static void iris$onDeleteTextures(int[] ids, CallbackInfo ci) {
		for (int id : ids) {
			TextureTracker.INSTANCE.onDeleteTexture(id);
		}
	}
}
