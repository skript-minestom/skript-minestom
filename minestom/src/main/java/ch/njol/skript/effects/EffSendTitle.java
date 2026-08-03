package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.command.CommandSender;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

@Name("Title - Send")
@Description("""
	Sends a title and/or subtitle to an audience with an optional fade in, stay, and/or fade out time.
	If sending only the subtitle, it will only be shown if the audience currently has a title displayed. Otherwise, it will be shown when the audience is next shown a title.
	Additionally, if no input is given for the times, the previous times of the last sent title will be used (or default values). Use the <a href='#EffResetTitle'>reset title</a> effect to restore the default values for the times.""")
@Example("send title \"Competition Started\" with subtitle \"Have fun, Stay safe!\" to player for 5 seconds")
@Example("send title \"Hi %player%\" to player")
@Example("send title \"Loot Drop\" with subtitle \"starts in 3 minutes\" to all players")
@Example("send title \"Hello %player%!\" with subtitle \"Welcome to our server\" to player for 5 seconds with fadein 1 second and fade out 1 second")
@Example("send subtitle \"Party!\" to all players")
@Since("2.3, 2.15 (support for showing anything)")
public class EffSendTitle extends Effect {

	static {
		String suffix = "[to %senders%] [for %-timespan%] [with fade[(-| )]in %-timespan%] [[and] [with] fade[(-| )]out %-timespan%]";
		Skript.registerEffect(EffSendTitle.class, "send title %component% [with subtitle %-component%] " + suffix,
			"send subtitle %component% " + suffix);
	}

	private @Nullable Expression<ComponentWrapper> title;
	private @Nullable Expression<ComponentWrapper> subtitle;
	private Expression<CommandSender> senders;
	private @Nullable Expression<Timespan> fadeIn;
	private @Nullable Expression<Timespan> stay;
	private @Nullable Expression<Timespan> fadeOut;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (matchedPattern == 0) {
			title = (Expression<ComponentWrapper>) exprs[0];
		}
		Expression<?> subtitle = exprs[1 - matchedPattern];
		if (subtitle != null) {
			this.subtitle = (Expression<ComponentWrapper>) subtitle;
			if (this.subtitle == null) {
				return false;
			}
		}
		senders = (Expression<CommandSender>) exprs[2 - matchedPattern];
		stay = (Expression<Timespan>) exprs[3 - matchedPattern];
		fadeIn = (Expression<Timespan>) exprs[4 - matchedPattern];
		fadeOut = (Expression<Timespan>) exprs[5 - matchedPattern];
		return true;
	}

	// TODO THE TIMER IS LONGER THAN IT SHOULD BE (3x?)
	@Override
	protected void execute(Event event) {
		Component title = ComponentWrapper.getOrElse(this.title, event, null);
		Component subtitle = ComponentWrapper.getOrElse(this.subtitle, event, null);

		boolean specifiesTimes = false;
		Duration stay;
		if (this.stay == null) {
			stay = Title.DEFAULT_TIMES.stay();
		} else {
			Timespan stayTimespan = this.stay.getSingle(event);
			if (stayTimespan == null) {
				return;
			}
			stay = stayTimespan.getDuration();
			specifiesTimes = true;
		}
		Duration fadeIn;
		if (this.fadeIn == null) {
			fadeIn = Title.DEFAULT_TIMES.fadeIn();
		} else {
			Timespan fadeInTimespan = this.fadeIn.getSingle(event);
			if (fadeInTimespan == null) {
				return;
			}
			fadeIn = fadeInTimespan.getDuration();
			specifiesTimes = true;
		}
		Duration fadeOut;
		if (this.fadeOut == null) {
			fadeOut = Title.DEFAULT_TIMES.fadeOut();
		} else {
			Timespan fadeOutTimespan = this.fadeOut.getSingle(event);
			if (fadeOutTimespan == null) {
				return;
			}
			fadeOut = fadeOutTimespan.getDuration();
			specifiesTimes = true;
		}

		Audience audience = Audience.audience(senders.getArray(event));
		if (specifiesTimes) {
			audience.sendTitlePart(TitlePart.TIMES, Times.times(fadeIn, stay, fadeOut));
		}
		if (subtitle != null) {
			audience.sendTitlePart(TitlePart.SUBTITLE, subtitle);
		}
		if (title != null) {
			audience.sendTitlePart(TitlePart.TITLE, title);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
		builder.append("send");
		if (title != null) {
			builder.append("title", title);
		}
		if (subtitle != null) {
			if (title != null) {
				builder.append("with");
			}
			builder.append("subtitle", subtitle);
		}
		builder.append("to", senders);
		if (stay != null) {
			builder.append("for", stay);
		}
		if (fadeIn != null) {
			builder.append("with fade in", fadeIn);
		}
		if (fadeOut != null) {
			builder.append("with fade out", fadeOut);
		}
		return builder.toString();
	}

}
