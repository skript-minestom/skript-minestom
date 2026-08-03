package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.*;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import ch.njol.util.Validate;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.Nullable;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Name("Send Resource Pack")
@Description("Sends a resource pack to one or more players.")
@Examples("""
	send resource pack from "https://example.com/pack.zip" with uuid "..." with hash "..." to player:
	    if resource pack status is successfully loaded:
	        broadcast "Pack loaded!\"""")
public class EffSecSendPack extends EffectSection {

	static {
		Skript.registerSection(EffSecSendPack.class,
			"send [resource] pack [from [url]] %string% with (uuid|id) %string% with hash %string% " +
				"[with prompt %-component%] to %players% [force:(forcefully|with force)] [replace:to replace [current packs]]");
		EventValues.registerEventValue(ResourcePackCallbackEvent.class, Player.class, ResourcePackCallbackEvent::getRecipient);
	}

	private Expression<String> url;
	private Expression<String> uuid;
	private Expression<String> hash;
	@Nullable
	private Expression<ComponentWrapper> prompt;
	private Expression<Player> recipients;
	private boolean force = false;
	private boolean replace = false;
	@Nullable
	private Trigger callback;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, @Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
		url = (Expression<String>) expressions[0];
		uuid = (Expression<String>) expressions[1];
		hash = (Expression<String>) expressions[2];
		prompt = (Expression<ComponentWrapper>) expressions[3];
		recipients = (Expression<Player>) expressions[4];
		force = parseResult.hasTag("force");
		replace = parseResult.hasTag("replace");
		if (sectionNode != null) callback = loadCode(sectionNode, "resource pack callback", ResourcePackCallbackEvent.class);
		return true;
	}

	@Override
	protected @Nullable TriggerItem walk(Event event) {
		String url = this.url.getSingle(event);
		if (url == null) return null;
		String sUuid = uuid.getSingle(event);
		if (sUuid == null || !Validate.isUUID(sUuid)) return null;
		UUID uuid = UUID.fromString(sUuid);
		String hash = this.hash.getSingle(event);
		if (hash == null) return null;
		Component prompt = ComponentWrapper.getOrElse(this.prompt, event, null);
		Player[] players = recipients.getArray(event);
		Object variables = Variables.copyVariables(event);
		ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
			.packs(ResourcePackInfo.resourcePackInfo(uuid, URI.create(url), hash))
			.prompt(prompt)
			.required(force)
			.replace(replace)
			.callback((u, status, audience) -> {
				if (callback == null) return;
				ResourcePackCallbackEvent callbackEvent = new ResourcePackCallbackEvent(u.toString(), status, (Player) audience);
				Variables.setLocalVariables(callbackEvent, variables);
				TriggerItem.walk(callback, callbackEvent);
				Variables.removeLocals(callbackEvent);
			}).build();
		for (Player player : players) {
			player.sendResourcePacks(request);
		}
		return super.walk(event, false);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "send resource pack from url " + url.toString(event, debug) + " with uuid " + uuid.toString(event, debug) +
			" with hash " + hash.toString(event, debug) + (prompt != null ? " with prompt " + prompt.toString(event, debug) : "") +
			" to " + recipients.toString(event, debug) + (force ? " with force" : "");
	}

	// todo create expressions to work for the callback
	public static class ResourcePackCallbackEvent extends Event {

		private static final HandlerList HANDLERS = new HandlerList();

		private final String uuid;
		private final ResourcePackStatus status;
		private final Player recipient;

		public ResourcePackCallbackEvent(String uuid, ResourcePackStatus status, Player recipient) {
			this.uuid = uuid;
			this.status = status;
			this.recipient = recipient;
		}

		public String getUuid() {
			return uuid;
		}

		public ResourcePackStatus getStatus() {
			return status;
		}

		public Player getRecipient() {
			return recipient;
		}

		@Override
		public HandlerList getHandlers() {
			return HANDLERS;
		}

		public static HandlerList getHandlerList() {
			return HANDLERS;
		}

	}

}
