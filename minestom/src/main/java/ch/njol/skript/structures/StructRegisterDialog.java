package ch.njol.skript.structures;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.ExprSecDialog;
import ch.njol.skript.expressions.ExprSecDialog.DialogEntries;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.util.dialog.DialogKind;
import ch.njol.skript.util.dialog.DialogWrapper;
import ch.njol.skript.util.dialog.SkriptDialogs;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.registry.DynamicRegistry;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;

@Name("Register Dialog")
@Description("""
	Builds a dialog and registers it in the server dialog registry under a namespaced key, so it
	can be shown by key and referenced by other dialogs.
	Entries are the same as the matching 'new dialog' section.

	Registry changes do not reach players who are already connected, because they would have to
	return to the configuration phase first. Use this for dialogs defined at startup, and show
	runtime-built dialogs as values instead.""")
@Examples("""
	register multi action dialog "myserver:shop":
		title: mm("<gold>Shop")
		buttons: (new dialog button labeled "Buy" running command "buy")""")
public class StructRegisterDialog extends Structure {

	static {
		Skript.registerStructure(StructRegisterDialog.class,
			"register (notice:notice|confirmation:confirmation|multi:multi[ ]action|links:server links) dialog <.+>",
			"register dialog list <.+>");
	}

	private static final class RegisterDialogEvent extends Event {
		@Override
		public HandlerList getHandlers() {
			return null;
		}
	}

	private DialogKind kind;
	private Script script;
	private @Nullable DialogEntries entries;
	private @Nullable Key key;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @Nullable EntryContainer entryContainer) {
		script = getParser().getCurrentScript();
		kind = kindFromMatch(matchedPattern, parseResult);

		String raw = parseResult.regexes.getFirst().group().trim().replace("\"", "");
		if (!Key.parseable(raw)) {
			Skript.error("'" + raw + "' is not a valid dialog key. Format is 'namespace:value'.");
			return false;
		}
		key = Key.key(raw);

		if (entryContainer == null) {
			Skript.error("A dialog registration needs entries inside it.");
			return false;
		}
		EntryContainer validated = ExprSecDialog.validatorFor(kind).validate(entryContainer.getSource());
		if (validated == null) return false;

		ParserInstance parser = getParser();
		parser.setCurrentEvent("register dialog", RegisterDialogEvent.class);
		entries = ExprSecDialog.resolve(kind, validated);
		parser.deleteCurrentEvent();

		return entries != null;
	}

	private static DialogKind kindFromMatch(int matchedPattern, SkriptParser.ParseResult parseResult) {
		if (matchedPattern == 1) return DialogKind.DIALOG_LIST;
		if (parseResult.hasTag("notice")) return DialogKind.NOTICE;
		if (parseResult.hasTag("confirmation")) return DialogKind.CONFIRMATION;
		if (parseResult.hasTag("multi")) return DialogKind.MULTI_ACTION;
		return DialogKind.SERVER_LINKS;
	}

	@Override
	public boolean load() {
		assert key != null && entries != null;
		DynamicRegistry<Dialog> registry = MinecraftServer.getDialogRegistry();

		Script owner = SkriptDialogs.getOwner(key);
		if (owner != null && owner != script) {
			Skript.error("A dialog is already registered under '" + key.asString() + "' by script '"
				+ owner.getConfig().getFileName() + "'; script '" + script.getConfig().getFileName()
				+ "' cannot register it too.");
			return false;
		}
		if (owner == null && registry.get(key) != null) {
			Skript.error("A dialog is already registered under '" + key.asString() + "' by something other than a script.");
			return false;
		}
		Structure ownerStructure = SkriptDialogs.getOwnerStructure(key);
		if (ownerStructure != null && ownerStructure != this) {
			Skript.error("A dialog is already registered under '" + key.asString()
				+ "' by another 'register dialog' structure in this script.");
			return false;
		}

		DialogWrapper dialog = ExprSecDialog.build(kind, entries, new RegisterDialogEvent());
		dialog.setKey(key);
		registry.register(key, dialog.toMinestom());
		SkriptDialogs.put(key, dialog, script, this);
		return true;
	}

	@Override
	public void unload() {
		if (key == null) return;
		if (SkriptDialogs.getOwnerStructure(key) != this) return;
		SkriptDialogs.remove(key);
		MinecraftServer.getDialogRegistry().remove(key);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "register " + kind.getName() + " dialog" + (key == null ? "" : " " + key.asString());
	}

}
