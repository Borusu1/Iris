package net.coderbot.iris.gl.buffer;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gl.IrisRenderSystem;
import org.lwjgl.opengl.GL43C;

import java.util.Collections;

public class ActiveBufferHolder implements ShaderStorageBufferHolder {
	private ShaderStorageBuffer[] buffers;
	private boolean destroyed;

	public ActiveBufferHolder(Int2LongArrayMap overrides) {
		destroyed = false;
		buffers = new ShaderStorageBuffer[Collections.max(overrides.keySet()) + 1];
		overrides.forEach((index, size) -> {
			int buffer = GlStateManager._glGenBuffers();
			GlStateManager._glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, buffer);
			IrisRenderSystem.bufferStorage(GL43C.GL_SHADER_STORAGE_BUFFER, size, 0);
			IrisRenderSystem.clearBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, GL43C.GL_R8, 0, size, GL43C.GL_RED, GL43C.GL_BYTE, new int[] {0});
			Iris.logger.warn("Creating SSBO " + index + " with size " + size + " at buffer location " + buffer);
			buffers[index] = new ShaderStorageBuffer(buffer, index, size);
		});
		GlStateManager._glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
	}

	@Override
	public int getBuffer(int location) {
		if (buffers.length < location) {
			return 0;
		}

		return buffers[location].getBuffer();
	}

	@Override
	public void destroyBuffers() {
		for (ShaderStorageBuffer buffer : buffers) {
			buffer.destroy();
		}
		buffers = null;
		destroyed = true;
	}
}
