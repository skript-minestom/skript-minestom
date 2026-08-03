package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Item;
import ch.njol.skript.util.NBTCompound;
import ch.njol.util.Kleenean;
import com.github.hapily04.skriptminestom.util.NBTUtils;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Item with NBT")
@Description("An item with a specific NBT.")
@Examples("""
	give player stone with nbt from "{enchantment_glint_override:1b}\"""")
public class ExprItemWithNBT extends SimpleExpression<Item> {

	static {
		Skript.registerExpression(ExprItemWithNBT.class, Item.class, ExpressionType.COMBINED,
			"%item% with [nbt] %nbtcompounds%");
	}

	private Expression<Item> item;
	private Expression<NBTCompound> nbt;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		item = (Expression<Item>) expressions[0];
		nbt = (Expression<NBTCompound>) expressions[1];
		return true;
	}

	@Override
	protected @Nullable Item[] get(Event event) {
		NBTCompound nbt = this.nbt.getSingle(event);
		Item item = this.item.getSingle(event);
		if (nbt == null || item == null) return new Item[0];
		item = item.copy();
		NBTCompound itemCompoundWrapper = NBTUtils.getNBTCompound(item, nbt.isCustom());
		itemCompoundWrapper.update(c -> NBTUtils.deepMerge(c, nbt.getCompound()));
		return new Item[]{item};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Item> getReturnType() {
		return Item.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return item.toString(event, debug) + " with " + nbt.toString(event, debug);
	}

}
