/*
 * Copyright � 2014 - 2015 Alexander01998 and contributors
 * All rights reserved.
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tk.wurst_client.utils;

import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import tk.wurst_client.WurstClient;
import tk.wurst_client.options.Options;

public class EntityUtils
{
	public static boolean lookChanged;
	public static float yaw;
	public static float pitch;
	
	public synchronized static void faceEntityClient(EntityLivingBase entity)
	{
		float[] rotations = getRotationsNeeded(entity);
		if(rotations != null)
		{
			Minecraft.getMinecraft().thePlayer.rotationYaw =
				limitAngleChange(
					Minecraft.getMinecraft().thePlayer.prevRotationYaw,
					rotations[0], 55);// NoCheat+
			// bypass!!!
			Minecraft.getMinecraft().thePlayer.rotationPitch = rotations[1];
		}
	}
	
	public synchronized static void faceEntityPacket(EntityLivingBase entity)
	{
		float[] rotations = getRotationsNeeded(entity);
		if(rotations != null)
		{
			yaw =
				limitAngleChange(
					Minecraft.getMinecraft().thePlayer.prevRotationYaw,
					rotations[0], 55);// NoCheat+
			pitch = rotations[1];
			lookChanged = true;
		}
	}
	
	public static float[] getRotationsNeeded(Entity entity)
	{
		if(entity == null)
			return null;
		double diffX = entity.posX - Minecraft.getMinecraft().thePlayer.posX;
		double diffY;
		if(entity instanceof EntityLivingBase)
		{
			EntityLivingBase entityLivingBase = (EntityLivingBase)entity;
			diffY =
				entityLivingBase.posY
					+ entityLivingBase.getEyeHeight()
					* 0.9
					- (Minecraft.getMinecraft().thePlayer.posY + Minecraft
						.getMinecraft().thePlayer.getEyeHeight());
		}else
			diffY =
				(entity.boundingBox.minY + entity.boundingBox.maxY)
					/ 2.0D
					- (Minecraft.getMinecraft().thePlayer.posY + Minecraft
						.getMinecraft().thePlayer.getEyeHeight());
		double diffZ = entity.posZ - Minecraft.getMinecraft().thePlayer.posZ;
		double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
		float yaw =
			(float)(Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
		float pitch = (float)-(Math.atan2(diffY, dist) * 180.0D / Math.PI);
		return new float[]{
			Minecraft.getMinecraft().thePlayer.rotationYaw
				+ MathHelper.wrapAngleTo180_float(yaw
					- Minecraft.getMinecraft().thePlayer.rotationYaw),
			Minecraft.getMinecraft().thePlayer.rotationPitch
				+ MathHelper.wrapAngleTo180_float(pitch
					- Minecraft.getMinecraft().thePlayer.rotationPitch)};
		
	}
	
	private final static float limitAngleChange(final float current,
		final float intended, final float maxChange)
	{
		float change = intended - current;
		if(change > maxChange)
			change = maxChange;
		else if(change < -maxChange)
			change = -maxChange;
		return current + change;
	}
	
	public static int getDistanceFromMouse(EntityLivingBase entity)
	{
		float[] neededRotations = getRotationsNeeded(entity);
		if(neededRotations != null)
		{
			float neededYaw =
				Minecraft.getMinecraft().thePlayer.rotationYaw
					- neededRotations[0], neededPitch =
				Minecraft.getMinecraft().thePlayer.rotationPitch
					- neededRotations[1];
			float distanceFromMouse =
				MathHelper.sqrt_float(neededYaw * neededYaw + neededPitch
					* neededPitch);
			return (int)distanceFromMouse;
		}
		return -1;
	}
	
	public static boolean isCorrectEntity(Object o, boolean ignoreFriends)
	{
		// non-entities
		if(!(o instanceof Entity))
			return false;
		
		// friends
		if(ignoreFriends && o instanceof EntityPlayer)
			if(WurstClient.INSTANCE.friends.contains(((EntityPlayer)o)
				.getName()))
				return false;
		
		Options.Target targetOptions = WurstClient.INSTANCE.options.target;
		
		// invisible entities
		if(((Entity)o).isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer))
			return targetOptions.invisible_mobs && o instanceof EntityLiving
				|| targetOptions.invisible_players && o instanceof EntityPlayer;
		
		// players
		if(o instanceof EntityPlayer)
			return ((EntityPlayer)o).isPlayerSleeping()
				&& targetOptions.sleeping_players
				|| !((EntityPlayer)o).isPlayerSleeping()
				&& targetOptions.players;
		
		// animals
		if(o instanceof EntityAgeable || o instanceof EntityAmbientCreature
			|| o instanceof EntityWaterMob)
			return targetOptions.animals;
		
		// monsters
		if(o instanceof EntityMob || o instanceof EntitySlime
			|| o instanceof EntityFlying)
			return targetOptions.monsters;
		
		// golems
		if(o instanceof EntityGolem)
			return targetOptions.golems;
		
		return false;
	}
	
	public static EntityLivingBase getClosestEntity(boolean ignoreFriends)
	{
		EntityLivingBase closestEntity = null;
		for(Object o : Minecraft.getMinecraft().theWorld.loadedEntityList)
			if(isCorrectEntity(o, ignoreFriends))
			{
				EntityLivingBase en = (EntityLivingBase)o;
				if(!(o instanceof EntityPlayerSP)
					&& !en.isDead
					&& en.getHealth() > 0
					&& Minecraft.getMinecraft().thePlayer.canEntityBeSeen(en)
					&& !en.getName().equals(
						Minecraft.getMinecraft().thePlayer.getName()))
					if(closestEntity == null
						|| Minecraft.getMinecraft().thePlayer
							.getDistanceToEntity(en) < Minecraft.getMinecraft().thePlayer
							.getDistanceToEntity(closestEntity))
						closestEntity = en;
			}
		return closestEntity;
	}
	
	public static ArrayList<EntityLivingBase> getCloseEntities(
		boolean ignoreFriends, float range)
	{
		ArrayList<EntityLivingBase> closeEntities =
			new ArrayList<EntityLivingBase>();
		for(Object o : Minecraft.getMinecraft().theWorld.loadedEntityList)
			if(isCorrectEntity(o, ignoreFriends))
			{
				EntityLivingBase en = (EntityLivingBase)o;
				if(!(o instanceof EntityPlayerSP)
					&& !en.isDead
					&& en.getHealth() > 0
					&& Minecraft.getMinecraft().thePlayer.canEntityBeSeen(en)
					&& !en.getName().equals(
						Minecraft.getMinecraft().thePlayer.getName())
					&& Minecraft.getMinecraft().thePlayer
						.getDistanceToEntity(en) <= range)
					closeEntities.add(en);
			}
		return closeEntities;
	}
	
	public static EntityLivingBase getClosestEntityRaw(boolean ignoreFriends)
	{
		EntityLivingBase closestEntity = null;
		for(Object o : Minecraft.getMinecraft().theWorld.loadedEntityList)
			if(isCorrectEntity(o, ignoreFriends))
			{
				EntityLivingBase en = (EntityLivingBase)o;
				if(!(o instanceof EntityPlayerSP) && !en.isDead
					&& en.getHealth() > 0)
					if(closestEntity == null
						|| Minecraft.getMinecraft().thePlayer
							.getDistanceToEntity(en) < Minecraft.getMinecraft().thePlayer
							.getDistanceToEntity(closestEntity))
						closestEntity = en;
			}
		return closestEntity;
	}
	
	public static EntityLivingBase getClosestEnemy(EntityLivingBase friend)
	{
		EntityLivingBase closestEnemy = null;
		for(Object o : Minecraft.getMinecraft().theWorld.loadedEntityList)
			if(isCorrectEntity(o, true))
			{
				EntityLivingBase en = (EntityLivingBase)o;
				if(!(o instanceof EntityPlayerSP) && o != friend && !en.isDead
					&& en.getHealth() <= 0 == false
					&& Minecraft.getMinecraft().thePlayer.canEntityBeSeen(en))
					if(closestEnemy == null
						|| Minecraft.getMinecraft().thePlayer
							.getDistanceToEntity(en) < Minecraft.getMinecraft().thePlayer
							.getDistanceToEntity(closestEnemy))
						closestEnemy = en;
			}
		return closestEnemy;
	}
	
	public static EntityLivingBase searchEntityByIdRaw(UUID ID)
	{
		EntityLivingBase newEntity = null;
		for(Object o : Minecraft.getMinecraft().theWorld.loadedEntityList)
			if(isCorrectEntity(o, false))
			{
				EntityLivingBase en = (EntityLivingBase)o;
				if(!(o instanceof EntityPlayerSP) && !en.isDead)
					if(newEntity == null && en.getUniqueID().equals(ID))
						newEntity = en;
			}
		return newEntity;
	}
	
	public static EntityLivingBase searchEntityByName(String name)
	{
		EntityLivingBase newEntity = null;
		for(Object o : Minecraft.getMinecraft().theWorld.loadedEntityList)
			if(isCorrectEntity(o, false))
			{
				EntityLivingBase en = (EntityLivingBase)o;
				if(!(o instanceof EntityPlayerSP) && !en.isDead
					&& Minecraft.getMinecraft().thePlayer.canEntityBeSeen(en))
					if(newEntity == null && en.getName().equals(name))
						newEntity = en;
			}
		return newEntity;
	}
	
	public static EntityLivingBase searchEntityByNameRaw(String name)
	{
		EntityLivingBase newEntity = null;
		for(Object o : Minecraft.getMinecraft().theWorld.loadedEntityList)
			if(isCorrectEntity(o, false))
			{
				EntityLivingBase en = (EntityLivingBase)o;
				if(!(o instanceof EntityPlayerSP) && !en.isDead)
					if(newEntity == null && en.getName().equals(name))
						newEntity = en;
			}
		return newEntity;
	}
}
