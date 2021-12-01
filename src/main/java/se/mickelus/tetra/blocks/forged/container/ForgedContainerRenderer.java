package se.mickelus.tetra.blocks.forged.container;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import se.mickelus.tetra.TetraMod;

// todo 1.15: ripped out
@OnlyIn(Dist.CLIENT)
public class ForgedContainerRenderer extends TileEntityRenderer<ForgedContainerTile> {
    public static final RenderMaterial material = new RenderMaterial(AtlasTexture.LOCATION_BLOCKS, new ResourceLocation(TetraMod.MOD_ID,"blocks/forged_container/forged_container"));

    public ModelRenderer lid;
    public ModelRenderer base;

    public ModelRenderer locks[];

    private static final float openDuration = 300;

    public ForgedContainerRenderer(TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);

        lid = new ModelRenderer(128, 64, 0, 0);
        lid.addBox(0, -3, -14, 30, 3, 14, 0);
        lid.x = 1;
        lid.y = 7;
        lid.z = 15;

        locks = new ModelRenderer[4];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ModelRenderer(128, 64, 0, 0);
            locks[i].addBox(-2 + i * 6, -1, -14.03f, 2, 3, 1, 0);
            locks[i].x = 8;
            locks[i].y = 7;
            locks[i].z = 15;
        }

        base = new ModelRenderer(128, 64, 0, 17);
        base.addBox(0, 1, 0, 30, 9, 14, 0);
        base.x = 1;
        base.y = 6;
        base.z = 1;
    }

    @Override
    public void render(ForgedContainerTile tile, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer,
            int combinedLight, int combinedOverlay) {
        if (tile.isFlipped()) {
            return;
        }

        if (tile.hasLevel()) {
            matrixStack.pushPose();
            matrixStack.translate(0.5F, 0.5F, 0.5F);
            // todo: why does the model render upside down by default?
            matrixStack.mulPose(Vector3f.ZP.rotationDegrees(180));
            matrixStack.mulPose(Vector3f.YP.rotationDegrees(tile.getFacing().toYRot()));
            matrixStack.translate(-0.5F, -0.5F, -0.5F);

            IVertexBuilder vertexBuilder = material.buffer(renderTypeBuffer, RenderType::entitySolid);

            renderLid(tile, partialTicks, matrixStack, vertexBuilder, combinedLight, combinedOverlay);
            renderLocks(tile, partialTicks, matrixStack, vertexBuilder, combinedLight, combinedOverlay);
            base.render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);

            matrixStack.popPose();
        }
    }

    private void renderLid(ForgedContainerTile tile, float partialTicks, MatrixStack matrixStack, IVertexBuilder vertexBuilder,
            int combinedLight, int combinedOverlay) {
        if (tile.isOpen()) {
            float progress = Math.min(1, (System.currentTimeMillis() - tile.openTime) / openDuration);
            lid.yRot = (progress * 0.1f * ((float) Math.PI / 2F));

            matrixStack.translate(0,0, 0.3f * progress);
            lid.render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);
            matrixStack.translate(0,0, -0.3f * progress);

        } else {
            lid.yRot = 0;
            lid.render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);
        }
    }

    private void renderLocks(ForgedContainerTile tile, float partialTicks, MatrixStack matrixStack, IVertexBuilder vertexBuilder,
            int combinedLight, int combinedOverlay) {
        Boolean[] locked = tile.isLocked();
        for (int i = 0; i < locks.length; i++) {
            if (locked[i]) {
                locks[i].render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);
            }
        }
    }
}
