package de.tr7zw.redcollectors.nbtapi;

import org.bukkit.entity.Entity;

import de.tr7zw.redcollectors.nbtapi.utils.MinecraftVersion;
import de.tr7zw.redcollectors.nbtapi.utils.annotations.AvaliableSince;
import de.tr7zw.redcollectors.nbtapi.utils.annotations.CheckUtil;

/**
 * NBT class to access vanilla tags from Entities. Entities don't support custom
 * tags. Use the NBTInjector for custom tags. Changes will be instantly applied
 * to the Entity, use the merge method to do many things at once.
 * 
 * @author tr7zw
 *
 */
public class NBTEntity extends NBTCompound {

	private final Entity ent;

	/**
	 * @param entity Any valid Bukkit Entity
	 */
	public NBTEntity(Entity entity) {
		super(null, null);
		ent = entity;
	}

	@Override
	public Object getCompound() {
		return NBTReflectionUtil.getEntityNBTTagCompound(NBTReflectionUtil.getNMSEntity(ent));
	}

	@Override
	protected void setCompound(Object compound) {
		NBTReflectionUtil.setEntityNBTTag(compound, NBTReflectionUtil.getNMSEntity(ent));
	}
	
	/**
	 * Gets the NBTCompound used by spigots PersistentDataAPI. This method is only
	 * available for 1.14+!
	 * 
	 * @return NBTCompound containing the data of the PersistentDataAPI
	 */
	@AvaliableSince(version = MinecraftVersion.MC1_14_R1)
	public NBTCompound getPersistentDataContainer() {
		if (hasKey("BukkitValues")) {
			return getCompound("BukkitValues");
		} else {
			NBTContainer container = new NBTContainer();
			container.addCompound("BukkitValues").setString("__nbtapi", "Marker to make the PersistentDataContainer have content");
			mergeCompound(container);
			return getCompound("BukkitValues");
		}
	}

	
}
