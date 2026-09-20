package ch.njol.skript.util.dialog;

public enum DialogKind {

	NOTICE("notice"),
	CONFIRMATION("confirmation"),
	MULTI_ACTION("multi action"),
	DIALOG_LIST("dialog list"),
	SERVER_LINKS("server links");

	private final String name;

	DialogKind(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
