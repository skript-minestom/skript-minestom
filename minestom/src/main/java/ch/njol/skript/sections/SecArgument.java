package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.*;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.structures.command.StructCommand;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.suggestion.Suggestion;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData;

import java.util.*;

@SuppressWarnings("unchecked")
@Name("Command Argument")
@Description("""
	This section allows you to create and more finely control custom command arguments.
	
	List of argument types with the skript type that will be stored in the variable:
	- literal -> string
	- boolean -> boolean
	- integer -> integer
	- long -> integer
	- double -> number
	- float -> number
	- string -> string # can be unlimited length, just must be surrounded by quotation marks if there are spaces for this argument
	- word -> string # singular piece of text
	- stringarray -> strings # should eat the rest of the command (typically how bukkit commands work)
	- gamemode -> gamemode
	- particle -> particle
	- entitytype -> entitytype
	- block -> block # essentially paper's blockdata
	- entity -> entity
	- entities -> entities
	- player -> player
	- players -> players
	- item -> item
	- component -> component
	- uuid -> string
	- nbtcompound -> nbtcompound
	- blockposition -> position # relative, can use ~ in arguments for example
	- vector -> position # relative, can use ~ in arguments for example
	- 2dvector -> vector # relative, can use ~ in arguments for example. Relative part is for sender's yaw/pitch. X and Z are the values, y is 0.
	
	NOTE: Entity/player selectors (@s, @a, etc.) are supported, but the sender must have the proper permission level in order for it to show up.
	See the 'Permission Level' expression""")
@Examples("""
	# Rough Syntax
	arg[ument] <local-var-name: argumenttype>:
		suggestions: # [OPTIONAL] section allowing you to add suggestion entries to the argument suggestions expression
		default value: # [OPTIONAL] allows you to set this argument to be optional, and have the default value set if this argument wasn't provided
		format: # [OPTIONAL] allows you to set the format for the argument (default, lower case, and upper case). Currently only supports gamemode
		trigger: # [OPTIONAL] section that runs the code in it when the command is executed. Only optional if there is another argument/subcommand further in this tree
	
	# Simple Example
	command /test:
		argument <text: string>:
			default value: "hello"
			trigger:
				send {_text} to sender
	
	# Expected Execution:
	# /test -> sends 'hello' to the sender
	# /test bob -> sends 'bob' to the sender
	
	# More Involved Example
	command /tp:
		condition:
			return whether sender is a player
		argument <target-player: player>:
			argument <msg: string>:
				default value: "Someone teleported to you."
				trigger:
					teleport player to {_target-player} in {_target-player}'s instance
					send {_msg} to {_target-player}
		trigger:
			send "Usage: /tp <player> <msg>" to player
	
	# Expected Execution:
	# /tp -> sends the usage message
	# /tp Notch -> teleports the player to notch and sends 'Someone teleported to you.' to Notch
	# /tp Notch hi notch -> teleports the player to notch and sends 'hi notch' to Notch""")
public class SecArgument extends Section {

	private static final EntryValidator ENTRY_VALIDATOR = EntryValidator.builder()
		.addSection("suggestions", true)
		.addEntryData(new ExpressionEntryData<>("default value", null, true, Object.class))
		.addEntry("format", null, true)
		.addSection("trigger", true)
		.unexpectedNodeTester(node -> {
			if (node instanceof SectionNode sectionNode) {
				String key = sectionNode.getKey();
				return key == null || !key.contains("arg");
			}
			return true;
		})
		.build();

	private static final Set<String> VALID_FORMATS = Set.of("default", "lower case", "upper case");

	static {
		Skript.registerSection(SecArgument.class, "arg[ument] <.+>");
		EventValues.registerEventValue(SuggestionCallbackEvent.class, CommandSender.class, SuggestionCallbackEvent::getSender);
		EventValues.registerEventValue(SuggestionCallbackEvent.class, String.class, from -> from.context.getInput());
	}

	private List<SecArgument> subArguments = new ArrayList<>();
	private EntryContainer container;
	private Expression<?> defaultExpression;
	private Trigger trigger;
	@SuppressWarnings("rawtypes")
	private Argument argument;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult,
						SectionNode sectionNode, List<TriggerItem> triggerItems) {
		container = ENTRY_VALIDATOR.validate(sectionNode);
		if (container == null) return false;
		String group = parseResult.regexes.getFirst().group();
		char firstCharacter = group.charAt(0);
		if (group.startsWith("[")) group = group.replace("[", "");
		else group = group.replace("<", "");
		argument = StructCommand.parseArg(StructCommand.stringToArrayDeque(group), firstCharacter);
		if (argument == null) return false; // errors already made in parseArg

		String format = container.getOptional("format", String.class, false);
		if (format != null) {
			if (!(argument instanceof ArgumentEnum<?> enumArg)) {
				Skript.error("Argument can't have a format if argument type doesn't support it (gamemode, etc.).");
				return false;
			}
			if (!VALID_FORMATS.contains(format.toLowerCase(Locale.ENGLISH))) {
				Skript.error("Invalid format '" + format + "' has been provided. Valid options are default, lower case, and upper case.");
				return false;
			}
			String sFormat = format.replace(' ', '_').toUpperCase(Locale.ENGLISH);
			if (!sFormat.contains("DEFAULT")) sFormat += "D";
			enumArg.setFormat(ArgumentEnum.Format.valueOf(sFormat));
		}

		SectionNode node = container.getOptional("suggestions", SectionNode.class, false);
		if (node != null) {
			Trigger trigger = loadCode(node, "argument suggestion", SuggestionCallbackEvent.class);
			argument.setSuggestionCallback((sender, context, suggestion) -> {
				TriggerItem.walk(trigger, new SuggestionCallbackEvent(sender, context, suggestion));
			});
		}

		// default expression can be unrelated to argument type rn. without reflection this may be impossible to detect
		defaultExpression = (Expression<Object>) container.getOptional("default value", false);
		if (defaultExpression != null) {
			if (argument.getDefaultValue() != null) {
				Skript.error("Argument was already marked as optional, so a default value should not be provided.");
				return false;
			}
			if (LiteralUtils.hasUnparsedLiteral(defaultExpression)) defaultExpression = LiteralUtils.defendExpression(defaultExpression);
			if (!LiteralUtils.canInitSafely(defaultExpression)) {
				Skript.error("Invalid default value was provided.");
				return false;
			}
		}

		node = container.getOptional("trigger", SectionNode.class, false);
		if (node != null) trigger = loadCode(node, "argument trigger", StructCommand.CommandTriggerEvent.class);

		boolean hasSubArgs = false;
		for (Node n : container.getUnhandledNodes()) {
			Section section = StructCommand.getSection(n);
			if (!(section instanceof SecArgument secArg)) continue;
			subArguments.add(secArg);
			secArg.walk(new StructCommand.ScriptCommandEvent());
			hasSubArgs = true;
		}

		if (hasSubArgs && trigger != null) {
			Skript.error("Cannot have a trigger in this argument when it has sub arguments.");
			return false;
		} else if (!hasSubArgs && trigger == null) {
			Skript.error("If no sub arguments exist, a trigger must be present. (" + container.getSource().getKey() + ")");
			return false;
		}

		return true;
	}

	// todo pretty sure we can provide previous arg values here, but not sure how to implement it atm
	@Override
	public @Nullable TriggerItem walk(Event event) {
		if (defaultExpression != null) argument.setDefaultValue(defaultExpression.getSingle(event));
		return super.walk(event, false);
	}

	public boolean hasSubArguments() {
		return !subArguments.isEmpty();
	}

	public List<SecArgument> getSubArguments() {
		return Collections.unmodifiableList(subArguments);
	}

	public Trigger getCommandTrigger() {
		return trigger;
	}

	public Argument getArgument() {
		return argument;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "argument";
	}

	public static class SuggestionCallbackEvent extends Event {

		private final CommandSender sender;
		private final CommandContext context;
		private final Suggestion suggestion;

		public SuggestionCallbackEvent(CommandSender sender, CommandContext context, Suggestion suggestion) {
			this.sender = sender;
			this.context = context;
			this.suggestion = suggestion;
		}

		public CommandSender getSender() {
			return sender;
		}

		public CommandContext getContext() {
			return context;
		}

		public Suggestion getSuggestion() {
			return suggestion;
		}

		@Override
		public HandlerList getHandlers() {
			return null;
		}

	}

}
