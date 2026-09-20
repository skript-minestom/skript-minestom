package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.NBTCompound;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.github.hapily04.skriptminestom.util.NBTUtils;
import net.kyori.adventure.nbt.*;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.Operation;
import org.skriptlang.skript.lang.arithmetic.OperationInfo;
import org.skriptlang.skript.lang.arithmetic.Operator;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Name("NBT Tag")
@Description("A typed NBT tag from an NBT compound.")
@Examples("set {_tag} to string nbt tag \"CustomName\" of {_nbt}")
public class ExprNBTTag extends SimpleExpression<Object> {

	static {
		Skript.registerExpression(ExprNBTTag.class, Object.class, ExpressionType.PROPERTY,
			"%tagtype% [nbt] tag[s] %strings% of %nbtcompounds%",
			"%nbtcompounds%'[s] %tagtype% [nbt] tag[s] %strings%");
	}

	private NBTUtils.TagType literalTagType = null;
	private Expression<NBTUtils.TagType> tagType;
	private Expression<String> tags;
	private Expression<NBTCompound> compoundExpr;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		int typeIndex = 0;
		int tagIndex = 1;
		int compoundIndex = 2;
		if (matchedPattern == 1) {
			compoundIndex = 0;
			typeIndex = 1;
			tagIndex = 2;
		}
		tagType = (Expression<NBTUtils.TagType>) expressions[typeIndex];
		if (tagType instanceof Literal<NBTUtils.TagType> literal) literalTagType = literal.getSingle();
		tags = (Expression<String>) expressions[tagIndex];
		compoundExpr = (Expression<NBTCompound>) expressions[compoundIndex];
		return true;
	}


	@Override
	protected @Nullable Object[] get(Event event) {
		List<Object> tags = new ArrayList<>();
		NBTUtils.TagType tagType = this.tagType.getSingle(event);
		if (tagType == null) return new Object[0];
		BinaryTagType<?> expectedBinaryTag = tagType.getExpectedBinaryTag();
		NBTCompound[] compounds = compoundExpr.getArray(event);
		for (String tag : this.tags.getArray(event)) {
			for (NBTCompound nbtCompound : compounds) {
				CompoundBinaryTag compound = nbtCompound.getCompound();
				BinaryTag binaryTag = NBTUtils.getNestedTag(compound, tag);
				if (binaryTag == null || !binaryTag.type().equals(expectedBinaryTag)) continue;
				Object converted = tagType.convertToSkriptFriendly(binaryTag);
				if (converted != null && converted.getClass().isArray()) {
					Object[] objects = (Object[]) converted;
					tags.addAll(Arrays.asList(objects));
				} else tags.add(converted);
			}
		}
		Class<?> arrayType = tagType.getSkriptCompatibleClass();
		if (arrayType.isArray()) arrayType = arrayType.getComponentType();
		Class<?> finalArrayType = arrayType;
		return tags.toArray(size -> (Object[]) Array.newInstance(finalArrayType, size));
	}

	@Override
	public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case REMOVE, ADD, SET -> CollectionUtils.array(getReturnType());
			case DELETE -> CollectionUtils.array(Object.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		NBTUtils.TagType tagType = this.tagType.getSingle(event);
		if (tagType == null) return;
		BinaryTagType<?> expectedBinaryTag = tagType.getExpectedBinaryTag();
		for (String tag : tags.getArray(event)) {
			for (NBTCompound nbtCompound : compoundExpr.getArray(event)) {
				nbtCompound.update((compound) -> {
					BinaryTag binaryTag = NBTUtils.getNestedTag(compound, tag);
					if (binaryTag != null && !binaryTag.type().equals(expectedBinaryTag)) return compound;
					Object original = binaryTag != null ? NBTUtils.TagType.convertToSkript(binaryTag) : null;
					List<Object> objects = null;
					if (original != null && original.getClass().isArray()) objects = new ArrayList<>(List.of((Object[]) original));
					BinaryTag newBinaryTag = switch (mode) {
						case SET -> {
							BinaryTag t = NBTUtils.TagType.convertFromSkript(delta);
							if (expectedBinaryTag.equals(BinaryTagTypes.LIST) && !t.type().equals(BinaryTagTypes.LIST)) t = ListBinaryTag.builder().add(t).build();
							yield t;
						}
						case ADD -> {
							if (objects != null) {
								objects.addAll(Arrays.asList(delta));
								yield NBTUtils.TagType.convertFromSkript(objects.toArray());
							} else if (tagType.getSkriptCompatibleClass().isArray()) {
								objects = new ArrayList<>();
								objects.add(delta);
								yield NBTUtils.TagType.convertFromSkript(objects.toArray());
							}
							Object object = delta[0];
							if (object == null) yield compound;
							if (original == null) yield NBTUtils.TagType.convertFromSkript(object, expectedBinaryTag);
							yield perform(Operator.ADDITION, original, object, compound);
						}
						case REMOVE -> {
							if (objects != null) {
								objects.removeAll(Arrays.asList(delta));
								yield objects.isEmpty() ? null : NBTUtils.TagType.convertFromSkript(objects.toArray());
							}
							Object object = delta[0];
							if (object == null || original == null) yield compound;
							yield perform(Operator.SUBTRACTION, original, object, compound);
						}
						default -> null;
					};
					if (newBinaryTag == compound) return compound;
					return NBTUtils.setNestedTag(compound, tag, newBinaryTag);
				});
			}
		}
	}

	private BinaryTag perform(Operator operator, Object original, Object object, CompoundBinaryTag compound) {
		OperationInfo<?, ?, ?> info;
		if (original != null) {
			info = Arithmetics.getOperationInfo(operator, original.getClass(), object.getClass());
			if (info == null)
				return compound;
		} else {
			info = Arithmetics.getOperationInfo(operator, object.getClass(), object.getClass());
			if (info == null)
				return compound;
			original = Arithmetics.getDefaultValue(info.getLeft());
			if (original == null)
				return compound;
		}
		//noinspection unchecked,rawtypes
		Object newValue = ((Operation) info.getOperation()).calculate(original, object);
		return NBTUtils.TagType.convertFromSkript(newValue);
	}

	@Override
	public boolean isSingle() {
		if (!compoundExpr.isSingle()) return false;
		return literalTagType != null && !literalTagType.getSkriptCompatibleClass().isArray();
	}

	@Override
	public Class<?> getReturnType() {
		return literalTagType != null ? literalTagType.getSkriptCompatibleClass() : Object.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "nbt tag " + tags.toString(event, debug) + " of " + compoundExpr.toString(event, debug);
	}

}
