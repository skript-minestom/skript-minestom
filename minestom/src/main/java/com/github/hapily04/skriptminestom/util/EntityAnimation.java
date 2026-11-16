package com.github.hapily04.skriptminestom.util;

import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.item.component.SwingAnimation;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.EntityAnimationPacket;
import net.minestom.server.network.packet.server.play.HitAnimationPacket;
import net.minestom.server.network.packet.server.play.SwingAnimationPacket;

/**
 * Entity animations exposed to Skript. Mirrors the pre-26.3 {@link EntityAnimationPacket.Animation} constants
 * so that scripts and serialized variables keep working, while sending the packet the current protocol expects.
 */
public enum EntityAnimation {

	SWING_MAIN_ARM,
	TAKE_DAMAGE,
	LEAVE_BED,
	SWING_OFF_HAND,
	CRITICAL_EFFECT,
	MAGICAL_CRITICAL_EFFECT;

	/**
	 * Creates the packet that plays this animation on the given entity.
	 * @param entity the entity playing the animation
	 * @return the packet to send
	 */
	public ServerPacket createPacket(Entity entity) {
		int entityId = entity.getEntityId();
		return switch (this) {
			case SWING_MAIN_ARM -> new SwingAnimationPacket(entityId, PlayerHand.MAIN, swingAnimation(entity, PlayerHand.MAIN));
			case SWING_OFF_HAND -> new SwingAnimationPacket(entityId, PlayerHand.OFF, swingAnimation(entity, PlayerHand.OFF));
			case TAKE_DAMAGE -> new HitAnimationPacket(entityId, 0);
			case LEAVE_BED -> new EntityAnimationPacket(entityId, EntityAnimationPacket.Animation.LEAVE_BED);
			case CRITICAL_EFFECT -> new EntityAnimationPacket(entityId, EntityAnimationPacket.Animation.CRITICAL_EFFECT);
			case MAGICAL_CRITICAL_EFFECT -> new EntityAnimationPacket(entityId, EntityAnimationPacket.Animation.MAGICAL_CRITICAL_EFFECT);
		};
	}

	/**
	 * Uses the attack animation of the item held in the given hand, matching {@link LivingEntity#swingMainHand()}.
	 */
	private static SwingAnimation swingAnimation(Entity entity, PlayerHand hand) {
		if (entity instanceof LivingEntity livingEntity)
			return livingEntity.getItemInHand(hand).get(DataComponents.ATTACK_ANIMATION, SwingAnimation.DEFAULT);
		return SwingAnimation.DEFAULT;
	}

}
