package net.coderbot.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import net.coderbot.iris.compat.sodium.impl.block_id.MaterialIdAwareVertexWriter;
import net.coderbot.iris.block_rendering.MaterialIdHolder;
import net.coderbot.iris.compat.sodium.impl.vertex_format.IrisModelVertexFormats;
import net.coderbot.iris.compat.sodium.impl.vertex_format.NormalHelper;
import net.coderbot.iris.vendored.joml.Vector3f;

import java.nio.ByteBuffer;

import static net.coderbot.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType.STRIDE;

public class XHFPModelVertexBufferWriterNio extends VertexBufferWriterNio implements ModelVertexSink, MaterialIdAwareVertexWriter {
	private MaterialIdHolder idHolder;

	public XHFPModelVertexBufferWriterNio(VertexBufferView backingBuffer) {
		super(backingBuffer, IrisModelVertexFormats.MODEL_VERTEX_XHFP);
	}

    int vertexCount = 0;
    float uSum;
    float vSum;

	private final QuadViewTerrain.QuadViewTerrainNio currentQuad = new QuadViewTerrain.QuadViewTerrainNio();
	private final Vector3f normal = new Vector3f();

	@Override
	public void copyQuadAndFlipNormal() {
		ensureCapacity(4);

		ByteBuffer src = this.byteBuffer.duplicate();
		ByteBuffer dst = this.byteBuffer.duplicate();

		src.position(this.byteBuffer.position() + this.writeOffset - STRIDE * 4);
		src.limit(src.position() + STRIDE * 4);

		dst.position(this.byteBuffer.position() + this.writeOffset);
		dst.limit(dst.position() + STRIDE * 4);

		dst.put(src);

		// Now flip vertex normals
		int packedNormal = this.byteBuffer.getInt(this.writeOffset + 32);
		int inverted = NormalHelper.invertPackedNormal(packedNormal);

		this.byteBuffer.putInt(this.writeOffset + 32, inverted);
		this.byteBuffer.putInt(this.writeOffset + 32 + STRIDE, inverted);
		this.byteBuffer.putInt(this.writeOffset + 32 + STRIDE * 2, inverted);
		this.byteBuffer.putInt(this.writeOffset + 32 + STRIDE * 3, inverted);

		// We just wrote 4 vertices, advance by 4
		for (int i = 0; i < 4; i++) {
			this.advance();
		}

		// Ensure vertices are flushed
		this.flush();
	}

	@Override
	public void writeVertex(float posX, float posY, float posZ, int color, float u, float v, int light, int chunkId) {
		uSum += u;
		vSum += v;

		short materialId = idHolder.id;
		short renderType = idHolder.renderType;

		this.writeQuadInternal(posX, posY, posZ, color, u, v, light, materialId, renderType, chunkId);
	}

	private void writeQuadInternal(float posX, float posY, float posZ, int color,
								   float u, float v, int light, short materialId, short renderType, int chunkId) {
		int i = this.writeOffset;

		vertexCount++;
		// NB: uSum and vSum must already be incremented outside of this function.

		ByteBuffer buffer = this.byteBuffer;

		buffer.putShort(i + 0, XHFPModelVertexType.encodePosition(posX));
		buffer.putShort(i + 2, XHFPModelVertexType.encodePosition(posY));
		buffer.putShort(i + 4, XHFPModelVertexType.encodePosition(posZ));
		buffer.putShort(i + 6, (short) chunkId);

		buffer.putInt(i + 8, color);

		buffer.putShort(i + 12, XHFPModelVertexType.encodeBlockTexture(u));
		buffer.putShort(i + 14, XHFPModelVertexType.encodeBlockTexture(v));

		buffer.putInt(i + 16, XHFPModelVertexType.encodeLightMapTexCoord(light));

		// NB: We don't set midTexCoord, normal, and tangent here, they will be filled in later.
		// block ID: We only set the first 2 values, any legacy shaders using z or w will get filled in based on the GLSL spec
		// https://www.khronos.org/opengl/wiki/Vertex_Specification#Vertex_format
		// TODO: can we pack this into one short?
		buffer.putShort(i + 36, materialId);
		buffer.putShort(i + 38, renderType);

		if (vertexCount == 4) {
			// TODO: Consider applying similar vertex coordinate transformations as the normal HFP texture coordinates

			uSum *= 0.25;
			vSum *= 0.25;

            buffer.putFloat(i + 20, uSum);
            buffer.putFloat(i + 20 - STRIDE, uSum);
            buffer.putFloat(i + 20 - STRIDE * 2, uSum);
            buffer.putFloat(i + 20 - STRIDE * 3, uSum);

			buffer.putFloat(i + 24, vSum);
            buffer.putFloat(i + 24 - STRIDE, vSum);
            buffer.putFloat(i + 24 - STRIDE * 2, vSum);
            buffer.putFloat(i + 24 - STRIDE * 3, vSum);

			vertexCount = 0;
			uSum = 0;
			vSum = 0;

			// normal computation
			// Implementation based on the algorithm found here:
			// https://github.com/IrisShaders/ShaderDoc/blob/master/vertex-format-extensions.md#surface-normal-vector

			currentQuad.setup(buffer, writeOffset, STRIDE);
            NormalHelper.computeFaceNormal(normal, currentQuad);
            int packedNormal = NormalHelper.packNormal(normal, 0.0f);

            buffer.putInt(i + 32, packedNormal);
            buffer.putInt(i + 32 - STRIDE, packedNormal);
            buffer.putInt(i + 32 - STRIDE * 2, packedNormal);
            buffer.putInt(i + 32 - STRIDE * 3, packedNormal);

            int tangent = currentQuad.computeTangent(normal.x(), normal.y(), normal.z());

            buffer.putInt(i + 28, tangent);
            buffer.putInt(i + 28 - STRIDE, tangent);
            buffer.putInt(i + 28 - STRIDE * 2, tangent);
            buffer.putInt(i + 28 - STRIDE * 3, tangent);
        }

		this.advance();
	}

    @Override
    public void iris$setIdHolder(MaterialIdHolder holder) {
        this.idHolder = holder;
    }
}
