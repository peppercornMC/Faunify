package com.pepper.faunify.entity.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pepper.faunify.Faunify;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class QuillModel<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(Faunify.MODID, "quill"), "main");
	private final ModelPart main;

	public QuillModel(ModelPart root) {
		this.main = root.getChild("main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 8).addBox(-0.5F, 3.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.25F))
		.texOffs(0, 0).addBox(-1.0F, -3.5F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0.0F, 23.95F, -0.05F, 3.1416F, 0.0F, 3.1416F));

		return LayerDefinition.create(meshdefinition, 16, 16);
	}
	
	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
