package ch.njol.skript.structures.command;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.sections.SecArgument;
import ch.njol.skript.sections.SecSubcommand;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.variables.Variables;
import com.github.hapily04.skriptminestom.luckperms.LuckPermsLookup;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData;
import org.skriptlang.skript.lang.entry.util.ExpressionOrSectionEntryData;
import org.skriptlang.skript.lang.structure.Structure;

import java.util.*;
import java.util.stream.Stream;

@Name("Command")
@Description("""
	Allows creation of true custom commands.
	See the 'Command Argument' section documentation for a list of supported argument types and how to use them.""")
@Examples("""
	# Rough Syntax
	command [/]<commandname> [<arguments>]:
		aliases: # [OPTIONAL] list of other names the command can go by (no / in front)
		condition: # [OPTIONAL] section allowing you to return a boolean of whether the sender should have access to the command
		subcommand [/]<commandname> [<arguments>]: # [OPTIONAL] works exactly like a command, can have argument/subcommand sections within
		arg[ument] <argument>: # [OPTIONAL] allows you to more finely tune or scope an argument. See the Command Argument section documentation for more details
		trigger: # [OPTIONAL] all arguments defined on the initial command line will be run inside this section
	
	# Simple Example
	command /spawn:
		aliases: hub
		condition:
			if any:
				sender isn't a player
				sender doesn't have permission "spawn"
			then:
				return false
			return true
		trigger:
			teleport player to {spawn}
	
	# More Involved Example
	command /foo <num: integer>:
		aliases: f
		condition:
			return whether sender has permission "foo"
		argument <i: item>:
			trigger:
				if sender isn't a player:
					send "You can't get a foo'd item because you're not a player!" to sender
					stop
				give player {_num} of {_i}
		trigger:
			if {_num} isn't set:
				send "Usage: /foo <amount> <item>" to sender
				stop
			loop {_num} times:
				send "foo" to sender
			send "You have been foo'd %{_num}% times" to sender
	
	# Expected Execution:
	# /foo 5 -> prints 'foo' 5 times in the sender's chat and tells them how many times they have been foo'd.
	# /foo -> Prints the usage message
	# /foo 5 acacia_boat -> Gives the player 5 acacia boats
	# /foo 5 acacia_boa (misspell) -> prints 'foo' 5 times in the sender's chat and tells them how many times they have been foo'd.""")
public class StructCommand extends Structure {

	public static final EntryValidator COMMAND_VALIDATOR = EntryValidator.builder()
		.addEntryData(new KeyValueEntryData<>("aliases", new String[0], true) {
			@Override
			protected @org.jspecify.annotations.Nullable String[] getValue(String value) {
				String[] split = value.split(",");
				for (int i = 0; i < split.length; i++) {
					String s = split[i].trim();
					if (s.contains(" ")) {
						Skript.error("Unexpected space in alias name '" + s + "'.");
						return null;
					}
					split[i] = s;
				}
				return split;
			}
		})
		.addEntry("permission", null, true)
		.addEntryData(new ExpressionEntryData<>("permission message", null, true, ComponentWrapper.class))
		.addEntry("executable by", null, true)
		.addEntryData(new ExpressionOrSectionEntryData<>("condition", null, null, true, Boolean.class))
		.addSection("trigger", true)
		.unexpectedNodeTester(node -> {
			String key = node.getKey();
			if (node instanceof SectionNode) return key == null || (!key.startsWith("arg") && !key.startsWith("subcommand"));
			//else if (node instanceof SimpleNode) return key == null || !key.startsWith("subcommand");
			return true;
		})
		.build();

	private static final ReturnHandler<Boolean> RETURN_HANDLER = new ReturnHandler<>() {
		@Override
		public void returnValues(Event event, Expression<? extends Boolean> value) {
			((CommandConditionEvent) event).setReturnValue(value.getSingle(event));
		}

		@Override
		public boolean isSingleReturnValue() {
			return true;
		}

		@Override
		public Class<? extends Boolean> returnValueType() {
			return Boolean.class;
		}
	};

	static {
		Skript.registerStructure(StructCommand.class, COMMAND_VALIDATOR, "command <.+>");
		EventValues.registerEventValue(CommandConditionEvent.class, CommandSender.class, CommandConditionEvent::getSender);
		EventValues.registerEventValue(CommandConditionEvent.class, String.class, CommandConditionEvent::getCommandString);
		EventValues.registerEventValue(CommandTriggerEvent.class, CommandSender.class, CommandTriggerEvent::getSender);
	}

	private EntryContainer container;
	private String toParse;
	private Command command;
	private Argument<?>[] baseArgs = new Argument[0];

	public StructCommand() {} // for skript

	public StructCommand(EntryContainer container, String toParse) {
		this.container = container;
		this.toParse = toParse;
	}

	public StructCommand(Command command) {
		this.command = command;
	}

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @Nullable EntryContainer entryContainer) {
		container = entryContainer;
		toParse = parseResult.regexes.getFirst().group().replaceFirst("/", "");
		return true;
	}

	@Override
	public boolean load() {
		ParserInstance parser = getParser();
		parser.setCurrentEvent("command", ScriptCommandEvent.class);
		Command command = parseCommand();
		if (command == null) {
			parser.deleteCurrentEvent();
			return false;
		}
		this.command = command;
		if (!argumentsAndSubcommands()) {
			parser.deleteCurrentEvent();
			return false;
		}
		MinecraftServer.getCommandManager().register(command);
		refreshPlayerCommands();
		parser.deleteCurrentEvent();
		return true;
	}

	public boolean argumentsAndSubcommands() {
		for (Node node : container.getUnhandledNodes()) {
			Section section = getSection(node);
			switch (section) {
				case SecSubcommand sub -> {
					StructCommand cmd = sub.getCommand();
					if (cmd.command == null) {
						cmd.command = cmd.parseCommand();
						cmd.argumentsAndSubcommands();
						Command command = cmd.command;
						if (command == null) return false;
					}
					this.command.addSubcommand(cmd.command);
				}
				case SecArgument arg -> {
					arg.walk(new ScriptCommandEvent());
					if (!argumentTree(command, new ArrayList<>(), arg)) return false;
				}
				case null, default -> {
					return false;
				}
			}
		}
		return true;
	}

	private boolean argumentTree(Command command, List<SecArgument> args, SecArgument current) {
		args.add(current);
		if (!current.hasSubArguments()) {
			Trigger trigger = current.getCommandTrigger();
			if (trigger == null) return false;
			Argument<?>[] argArr = Stream.concat(Arrays.stream(baseArgs), args.stream().map(SecArgument::getArgument)).toArray(Argument[]::new);
			command.addSyntax((sender, context) -> {
				runCommandTrigger(trigger, sender, context, argArr);
			}, argArr);
			return true;
		}
		for (SecArgument secArgument : current.getSubArguments()) {
			if (!argumentTree(command, new ArrayList<>(args), secArgument)) return false;
		}
		return true;
	}

	private Command parseCommand() {
		ArrayDeque<Character> chars = stringToArrayDeque(toParse);

		String commandName = null;
		List<Argument<?>> args = new ArrayList<>();
		while (!chars.isEmpty()) {
			char c = chars.pop();
			if (c == ' ') continue;
			if (commandName == null) {
				String name = parseName(c, chars);
				//if (name == null) return false; // an error occurred while parsing name
				commandName = name;
			} else if (c == '<' || c == '[') {
				Argument<?> arg = parseArg(chars, c);
				if (arg == null) return null; // an error occurred while parsing arg
				args.add(arg);
			} else {
				Skript.error("Unknown character '" + c + "' was found whilst trying to parse a command.");
				return null;
			}
		}
		Argument<?>[] argArray = args.toArray(new Argument[0]);
		baseArgs = argArray;
		String[] aliases = container.getOptional("aliases", String[].class, true);

		ParserInstance parser = getParser();
		parser.setCurrentEvent("command trigger", CommandTriggerEvent.class);
		assert commandName != null; // has to be set in while loop
		Trigger trigger = getTrigger("command /" + commandName, container.getOptional("trigger", SectionNode.class, false));
		parser.setCurrentEvent("command", ScriptCommandEvent.class);
		assert aliases != null; // default value of empty string array
		Command command = new Command(commandName, aliases);
		if (!initCondition(command)) return null; // error initializing condition
		if (!args.isEmpty()) command.addSyntax((sender, context) -> {
			runCommandTrigger(trigger, sender, context, argArray);
		}, argArray);
		command.setDefaultExecutor((sender, context) -> {
			runCommandTrigger(trigger, sender, context);
		});
		return command;
	}

	private boolean initCondition(Command command) {
		Expression<ComponentWrapper> permissionMessage = container.getOptional("permission message", Expression.class, false);
		if (permissionMessage != null && !container.hasEntry("permission")) {
			Skript.error("If a permission message is provided, a permission node must also be provided.");
			return false;
		}
		String permission = container.getOptional("permission", String.class, false);

		String executableBy = container.getOptional("executable by", String.class, false);
		Class<? extends CommandSender> executableByClass = null;
		if (executableBy != null) {
			executableBy = executableBy.toLowerCase(Locale.ENGLISH);
			switch (executableBy) {
				case "player", "players" -> executableByClass = Player.class;
				case "console" -> executableByClass = ConsoleSender.class;
				default -> {
					Skript.error("Unknown executor '" + executableBy + "' in 'executable by' entry while parsing command '" + command.getName() + "'.");
					return false;
				}
			}
		}
		Class<? extends CommandSender> finalExecutableByClass = executableByClass;

		getParser().setCurrentEvent("command condition", CommandConditionEvent.class);
		ExpressionOrSectionEntryData.ExpressionOrSection<Boolean> conditionEntry =
			container.getOptional("condition", ExpressionOrSectionEntryData.ExpressionOrSection.class, false);

		Trigger condition = conditionEntry == null || conditionEntry.section() == null ? null
			: getReturnableTrigger("command condition", conditionEntry.section());
		Expression<? extends Boolean> conditionExpr = conditionEntry == null ? null : conditionEntry.expression();
		command.setCondition((sender, commandString) -> {
			CommandConditionEvent event = new CommandConditionEvent(sender, commandString);
			if (condition != null) {
				TriggerItem.walk(condition, event);
				if (!(event.returnValue != null && event.returnValue)) return false;
			} else if (conditionExpr != null && Boolean.FALSE.equals(conditionExpr.getSingle(event))) return false;

			if (permission != null && !LuckPermsLookup.hasPermission(sender, permission)) {
				if (commandString != null && permissionMessage != null) {
					Component component = ComponentWrapper.getOrElse(permissionMessage, event, null);
					if (component != null) sender.sendMessage(component);
				}
				return false;
			}

			if (finalExecutableByClass != null) return finalExecutableByClass.isAssignableFrom(sender.getClass());
			return true;
		});
		return true;
	}

	private void runCommandTrigger(Trigger trigger, CommandSender sender, CommandContext context, Argument<?>... args) {
		CommandTriggerEvent event = new CommandTriggerEvent(sender);
		for (Argument<?> arg : args) {
			Object o = context.get(arg);
			o = ArgumentType.convertToSkriptObject(o, sender, arg);
			String id = arg.getId();
			if (o == null) continue;
			if (o.getClass().isArray()) {
				Object[] arr = (Object[]) o;
				for (int i = 0; i < arr.length; i++) {
					Variables.setVariable(id + Variable.SEPARATOR + i, arr[i], event, true);
				}
			}
			else Variables.setVariable(id, o, event, true);
		}
		TriggerItem.walk(trigger, event);
	}


	private ReturnableTrigger<Boolean> getReturnableTrigger(String name, SectionNode node) {
		if (node == null) return null;
		return RETURN_HANDLER.loadReturnableTrigger(node, name, new SimpleEvent());
	}

	private Trigger getTrigger(String name, SectionNode node) {
		if (node == null) return null;
		return new Trigger(getParser().getCurrentScript(), name, new SimpleEvent(), ScriptLoader.loadItems(node));
	}

	private String parseName(char startingChar, ArrayDeque<Character> chars) {
		StringBuilder name = new StringBuilder("" + startingChar);
		while (!chars.isEmpty()) {
			if (chars.peek() == ' ') return name.toString();
			char c = chars.pop();
			if (c != '*' && c != ':') { // for when we set the local variables
				name.append(c);
				continue;
			}
			Skript.error("Unknown character '" + c + "' was found whilst trying to parse the name of a command.");
			return null;
		}
		return name.toString();
	}

	public static Argument<?> parseArg(ArrayDeque<Character> chars, char startingCharacter) {
		StringBuilder sb = new StringBuilder();
		String name = null;
		String argType = null;

		boolean optional = false;
		if (startingCharacter == '[') {
			optional = true;
			if (chars.isEmpty() || chars.peek() != '<') {
				Skript.error("Expected '<' after '[' while parsing an argument.");
				return null;
			}
			chars.pop(); // we should have verified by here that next char is <, so pop it
		}

		while (!chars.isEmpty()) {
			char c = chars.pop();
			if (name != null) {
				if (c != '>') sb.append(c);
				else {
					if (optional) {
						if ((chars.isEmpty() || chars.peek() != ']')) {
							Skript.error("Expected ending ']', but none was found while parsing command arg named '" + name + "'.");
							return null;
						}
						chars.pop(); // pop last ]
					}
					argType = sb.toString();
					break;
				}
			} else {
				if (c == ':') {
					if (!chars.isEmpty() && chars.peek() == ' ') chars.pop(); // pop extra space
					name = sb.toString();
					sb.setLength(0); // reset it as we will reuse it for building arg type
					continue;
				}
				if (Character.isAlphabetic(c) || Character.isDigit(c) || c == '_' || c == '-' || c == '/') {
					sb.append(c);
					continue;
				}
				Skript.error("Unknown character '" + c + "' was found whilst trying to parse the name of an argument.");
				return null;
			}
		}

		if (argType == null) {
			Skript.error("No ending brace '>' was found while parsing command arg named '" + name + "'.");
			return null;
		}

		return buildArg(name, argType, optional);
	}

	private static Argument<?> buildArg(String name, String typeInput, boolean optional) {
		String initialInput = typeInput.split(" ")[0];
		for (ArgumentType type : ArgumentType.values()) {
			if (!type.matchesInitialInput(initialInput)) continue;
			Argument<?> arg = type.getProvider().apply(name, typeInput);
			if (arg == null) continue;
			if (optional) arg.setDefaultValue(() -> null);
			return arg;
		}
		Skript.error("No argument type was found given '" + typeInput + "'.");
		return null;
	}

	public static ArrayDeque<Character> stringToArrayDeque(String s) {
		char[] chars = s.toCharArray();
		List<Character> charList = new ArrayList<>(chars.length);
		for (char c : chars) {
			charList.add(c);
		}
		return new ArrayDeque<>(charList);
	}

	public static Section getSection(Node node) {
		String key = node.getKey();
		if (key == null) return null;
		if (node instanceof SectionNode sectionNode) return Section.parse(key, null, sectionNode, Collections.emptyList());
		//else if (node instanceof SimpleNode && key.startsWith("subcommand")) return EffectSection.parse(key, null, null, Collections.emptyList());
		return null;
	}

	@Override
	public void unload() {
		if (command == null) return;
		MinecraftServer.getCommandManager().unregister(command);
		refreshPlayerCommands();
	}

	private void refreshPlayerCommands() {
		for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
			player.refreshCommands();
		}
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		return "command";
	}

	static class CommandConditionEvent extends Event {

		private final CommandSender sender;
		private final String commandString;
		private Boolean returnValue;

		public CommandConditionEvent(CommandSender sender, String commandString) {
			this.sender = sender;
			this.commandString = commandString;
		}

		public CommandSender getSender() {
			return sender;
		}

		public String getCommandString() {
			return commandString;
		}

		public void setReturnValue(Boolean returnValue) {
			this.returnValue = returnValue;
		}

		@Override
		public HandlerList getHandlers() {
			return null;
		}

	}

	public static class CommandTriggerEvent extends Event {

		private final CommandSender sender;

		public CommandTriggerEvent(CommandSender sender) {
			this.sender = sender;
		}

		public CommandSender getSender() {
			return sender;
		}

		@Override
		public HandlerList getHandlers() {
			return null;
		}

	}

	 public static class ScriptCommandEvent extends Event {

		@Override
		public HandlerList getHandlers() {
			return null;
		}

	}

}
