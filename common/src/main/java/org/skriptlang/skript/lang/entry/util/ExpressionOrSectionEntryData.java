package org.skriptlang.skript.lang.entry.util;

import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryData;
import org.skriptlang.skript.lang.entry.SectionEntryData;

public class ExpressionOrSectionEntryData<T> extends EntryData<ExpressionOrSectionEntryData.ExpressionOrSection<T>> {

	private final ExpressionEntryData<T> expressionData;
	private final SectionEntryData sectionData;

	public ExpressionOrSectionEntryData(
		String key,
		@Nullable Expression<? extends T> defaultExpression,
		@Nullable SectionNode defaultSection,
		boolean optional,
		Class<? extends T> expressionReturnType
	) {
		super(key, null, optional);
		this.expressionData = new ExpressionEntryData<>(key, defaultExpression, optional, expressionReturnType);
		this.sectionData = new SectionEntryData(key, defaultSection, optional);
	}

	@Override
	public @Nullable ExpressionOrSection<T> getValue(Node node) {
		if (node instanceof SectionNode && sectionData.canCreateWith(node)) {
			return new ExpressionOrSection<>(sectionData.getValue(node), null);
		}

		if (node instanceof ch.njol.skript.config.SimpleNode && expressionData.canCreateWith(node)) {
			Expression<? extends T> expression = expressionData.getValue(node);
			if (expression == null) return null;
			return new ExpressionOrSection<>(null, expression);
		}

		return null;
	}

	@Override
	public boolean canCreateWith(Node node) {
		String key = node.getKey();
		if (key == null) return false;
		if (node instanceof SectionNode) {
			return sectionData.canCreateWith(node);
		}
		return expressionData.canCreateWith(node);
	}

	public record ExpressionOrSection<T>(@Nullable SectionNode section, @Nullable Expression<? extends T> expression) {}
}

