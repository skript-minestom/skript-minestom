package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import com.github.hapily04.skriptminestom.util.FileUtils;
import com.github.hapily04.skriptminestom.util.MiniRegionFile;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.hollowcube.polar.PolarChunk;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.event.instance.InstanceChunkLoadEvent;
import net.minestom.server.instance.*;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biome.Biome;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData;
import org.skriptlang.skript.lang.entry.util.LiteralEntryData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Name("Create Instance/World")
@Description("""
	Creates a new instance container or a shared instance. Instances are Minestom's lightweight world equivalent.
	Shared instances are an ultra lightweight 'world' that share the chunk data (blocks) from its parent world. Entities are not shared.
	
	- Entries
	dynamic lighting -> boolean
	file -> string
	file dimension -> string (only used if world was made after 26.1, example: minecraft:overworld)
	loader -> anvil/polar (Anvil is the world format that Minecraft uses and Polar was custom in-memory format made for smaller worlds by hollowcube for Minestom)
	dimension -> dimensiontype
	preload biome -> biome (fills the entire world with the provided biome)
	[async] preload option -> normal/strict (strict will not allow default generation of new chunks, even blank ones, as the player travels the world)
	preload chunk amount -> number (the radius in chunks to preload when there is no world file to read chunks from, defaults to the server's chunk view distance)
	generator -> section allowing custom generation of each chunk as the world loads (complex generation not recommended as it can get laggy with Skript)""")
@Examples("""
	create biome under "test:lobby" stored in {_b}:
		sky color: rgb(111, 223, 249)
		fog color: rgb(253, 252, 198)
		foliage color: rgb(71, 255, 0)
		water color: rgb(111, 223, 249)
	create instance container stored in {-worlds::lobby}:
		loader: polar
		file: "worlds/lobby/lobby.polar"
		preload biome: {_b}
		async preload option: strict""")
public class EffSecCreateInstance extends EffectSection {

	private static final EntryValidator ENTRY_VALIDATOR;
	private static final List<String> VALID_LOADER_ENTRIES = List.of("anvil", "polar");
	// maybe a preload-super-strict option that only loads chunks w blocks in them instead of every chunk that the world provides?
	private static final List<String> VALID_GENERATOR_PRESET_ENTRIES = List.of("strict", "normal");
	private static final Generator BLANK_GENERATOR = unit -> unit.modifier().fill(Block.AIR);
	public static final List<Instance> RELIGHT_INSTANCES = new CopyOnWriteArrayList<>();

	static {
		ENTRY_VALIDATOR = EntryValidator.builder()
			.addEntryData(new LiteralEntryData<>("dynamic lighting", null, true, Boolean.class))
			.addEntryData(new ExpressionEntryData<>("file", null, true, String.class))
			.addEntryData(new ExpressionEntryData<>("file dimension", null, true, String.class))
			.addEntry("loader", null, true)
			.addEntryData(new ExpressionEntryData<>("dimension", null, true, DimensionType.class))
			.addEntryData(new ExpressionEntryData<>("preload biome", null, true, Biome.class))
			.addEntry("preload option", null, true)
			.addEntry("async preload option", null, true)
			.addEntryData(new ExpressionEntryData<>("preload chunk amount", null, true, Integer.class))
			.addSection("generator", true)
			.build();
		Skript.registerSection(EffSecCreateInstance.class,
			"create instance [container] (and store it|stored) in %objects%",
			"create shared instance from [instance] %instancecontainer% (and store it|stored) in %objects%");

		MinecraftServer.getGlobalEventHandler().addListener(InstanceChunkLoadEvent.class, event -> {
			Instance instance = event.getInstance();
			if (!RELIGHT_INSTANCES.contains(instance)) return;
			LightingChunk.relight(instance, List.of(event.getChunk()));
		});
	}

	private int matchedPattern;
	private Expression<Object> storage;
	@Nullable
	private Expression<InstanceContainer> originalInstance;
	private boolean dynamicLighting = false;
	@Nullable
	private Expression<String> worldFile;
	@Nullable
	private Expression<String> worldFileDimension;
	@Nullable
	private String loader;
	@Nullable
	private Expression<DimensionType> dimension;
	@Nullable
	private Expression<Biome> biome;
	@Nullable
	private String preloadOption;
	@Nullable
	private Expression<Integer> preloadChunkAmount;
	boolean asyncPreload = false;
	@Nullable
	private Trigger generator;
	private boolean phonyAnvilLoader = false;
	@SuppressWarnings({"unchecked"})
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult,
						SectionNode sectionNode, List<TriggerItem> triggerItems) {
		this.matchedPattern = matchedPattern;
		if (!isDelayed.isFalse()) {
			Skript.error("An instance creation section cannot be delayed.");
			return false;
		}
		if (sectionNode != null) {
			if (matchedPattern == 1) {
				Skript.error("A shared instance cannot have a loader, generator, or dimension. It copies everything except for entities from its original instance.");
				return false;
			}
			EntryContainer container = ENTRY_VALIDATOR.validate(sectionNode);
			if (container == null) return false; // shouldn't be null because section node isn't null

			Boolean dynamicLighting = container.getOptional("dynamic lighting", Boolean.class, false);
			if (dynamicLighting != null) this.dynamicLighting = dynamicLighting;

			this.worldFile = container.getOptional("file", Expression.class, false);

			String loader = container.getOptional("loader", String.class, false);
			if (loader != null) {
				if (this.worldFile == null) {
					Skript.error("If a loader is provided, a valid storage location must also be provided (file location is missing).");
					return false;
				}
				if (!VALID_LOADER_ENTRIES.contains(loader)) {
					Skript.error("An invalid chunk loader has been provided: '" + loader + "'.");
					return false;
				}
				this.loader = loader;
			}

			this.worldFileDimension = container.getOptional("file dimension", Expression.class, false);
			if (loader != null && !loader.equals("anvil") && worldFileDimension != null) {
				Skript.error("Entry 'file dimension' should only be provided if the loader is 'anvil' and the world was created " +
					"after 26.1.");
				return false;
			}

			dimension = container.getOptional("dimension", Expression.class, false);
			biome = container.getOptional("preload biome", Expression.class, false);

			String preloadOption = container.getOptional("preload option", String.class, false);
			String asyncPreloadOption = container.getOptional("async preload option", String.class, false);
			if (preloadOption != null && asyncPreloadOption != null) {
				Skript.error("You can only have one preload option, not two.");
				return false;
			}
			if (preloadOption == null) {
				preloadOption = asyncPreloadOption;
				if (preloadOption != null) asyncPreload = true;
			}
			if (preloadOption != null) {
				if (!VALID_GENERATOR_PRESET_ENTRIES.contains(preloadOption)) {
					Skript.error("An invalid preload option has been provided: '" + preloadOption + "'.");
					return false;
				}
				this.preloadOption = preloadOption;
			}

			preloadChunkAmount = container.getOptional("preload chunk amount", Expression.class, false);
			if (preloadChunkAmount != null && this.preloadOption == null) {
				Skript.error("Entry 'preload chunk amount' only applies when a preload option is also provided.");
				return false;
			}

			SectionNode generator = container.getOptional("generator", SectionNode.class, false);
			if (generator != null) this.generator = loadCode(generator, "generator", TerrainGenerateEvent.class);
		}
		// only store expressions if it's successfully initialized
		if (matchedPattern == 0) storage = (Expression<Object>) expressions[0];
		else {
			originalInstance = (Expression<InstanceContainer>) expressions[0];
			storage = (Expression<Object>) expressions[1];
		}
		return true;
	}

	@Override
	protected @Nullable TriggerItem walk(Event event) {
		Instance instance;
		Biome b = this.biome == null ? null : this.biome.getSingle(event);
		RegistryKey<Biome> biome = b != null ? MinecraftServer.getBiomeRegistry().getKey(b) : null;
		if (matchedPattern == 0) { // creating new instance container
			if (dimension != null) {
				DimensionType dimension = this.dimension.getSingle(event);
				if (dimension == null) {
					SkriptLogger.LOGGER.warn("Dimension provided, but its value is none. Creating base instance container.");
					instance = MinecraftServer.getInstanceManager().createInstanceContainer();
				} else {
					RegistryKey<DimensionType> dimensionKey = MinecraftServer.getDimensionTypeRegistry().getKey(dimension);
					if (dimensionKey == null) {
						SkriptLogger.LOGGER.warn("Dimension provided, but it doesn't seem like it was registered? Creating base instance container.");
						instance = MinecraftServer.getInstanceManager().createInstanceContainer();
					} else instance = MinecraftServer.getInstanceManager().createInstanceContainer(dimensionKey);
				}
			} else instance = MinecraftServer.getInstanceManager().createInstanceContainer();

			InstanceContainer container = (InstanceContainer) instance;
			if (dynamicLighting) {
				instance.setChunkSupplier(LightingChunk::new);
				RELIGHT_INSTANCES.add(container);
			}
			File trueWorldFile = null;
			if (worldFile == null) phonyAnvilLoader = true;
			else {
				String worldFile = this.worldFile.getSingle(event);
				if (worldFile == null) return super.walk(event, false);
				trueWorldFile = new File(FileUtils.getServerDirectory(), worldFile);
				if (loader != null) {
					Path worldPath = trueWorldFile.toPath();
					if (loader.equalsIgnoreCase("anvil")) {
						Key dimensionKey = null;
						if (this.worldFileDimension != null) {
							String worldFileDimension = this.worldFileDimension.getSingle(event);
							if (!Key.parseable(worldFileDimension)) {
								SkriptLogger.LOGGER.error("Key 'file dimension' was provided whilst trying to create an instance " +
									"in {}, but it was not in the 'key:value' format.", getParser().getCurrentScript().nameAndPath());
								return super.walk(event, false);
							} else dimensionKey = Key.key(worldFileDimension);

						}
						container.setChunkLoader(dimensionKey != null ? new AnvilLoader(worldPath, dimensionKey) : new AnvilLoader(worldPath));
					} else {
						try {
							PolarLoader loader = new PolarLoader(worldPath);
							loader.setLoadLighting(!dynamicLighting);
							container.setChunkLoader(loader);
						} catch (IOException e) {
							SkriptLogger.LOGGER.error("Runtime error while trying to set chunk loader to polar: {}", e.getMessage());
						}
					}
				}
			}

			if (generator != null) {
				Object variables = Variables.copyLocalVariables(event);
				container.setGenerator(unit -> {
					TerrainGenerateEvent generateEvent = new TerrainGenerateEvent(unit);
					Variables.setLocalVariables(generateEvent, Variables.copyVariables(variables));
					TriggerItem.walk(generator, generateEvent);
					Variables.removeLocals(generateEvent);
				});
			}
			if (preloadOption != null) preLoadChunks(container, trueWorldFile, preloadOption.equals("strict"), biome, preloadChunkAmount(event));
		} else {
			assert originalInstance != null; // it won't be null because it's in the pattern
			InstanceContainer instanceContainer = originalInstance.getSingle(event);
			if (instanceContainer == null) return super.walk(event, false);
			instance = MinecraftServer.getInstanceManager().createSharedInstance(instanceContainer);
			instance.enableAutoChunkLoad(instanceContainer.hasEnabledAutoChunkLoad());
		}

		storage.change(event, new Instance[]{instance}, Changer.ChangeMode.SET); // store the created instance on the variable
		return super.walk(event, false);
	}

	public static class TerrainGenerateEvent extends Event {

		private final GenerationUnit unit;

		public TerrainGenerateEvent(GenerationUnit unit) {
			this.unit = unit;
		}

		public GenerationUnit getUnit() {
			return unit;
		}

		@Override
		public HandlerList getHandlers() {
			return null;
		}

	}

	private void fillChunkWithBiome(Instance instance, Chunk chunk, RegistryKey<Biome> biome) {
		if (biome == null) return;
		DimensionType dt = instance.getCachedDimensionType();
		int minY = dt.minY();
		int maxY = dt.maxY();
		int startX = chunk.getChunkX() << 4;
		int startZ = chunk.getChunkZ() << 4;
		for (int x = startX; x < startX + 16; x++) {
			for (int z = startZ; z < startZ + 16; z++) {
				for (int y = minY; y < maxY; y++) {
					chunk.setBiome(x, y, z, biome);
				}
			}
		}
	}

	private int preloadChunkAmount(Event event) {
		if (preloadChunkAmount == null) return ServerFlag.CHUNK_VIEW_DISTANCE;
		Integer amount = preloadChunkAmount.getSingle(event);
		if (amount == null || amount < 0) {
			SkriptLogger.LOGGER.warn("Entry 'preload chunk amount' was provided, but its value is not a valid chunk amount. " +
				"Falling back to the server's chunk view distance.");
			return ServerFlag.CHUNK_VIEW_DISTANCE;
		}
		return amount;
	}

	private void preLoadChunks(InstanceContainer container, @Nullable File file, boolean strict, RegistryKey<Biome> biome, int chunkViewDistance) {
		ChunkLoader loader = container.getChunkLoader();
		boolean loadServerRenderDistance = true;
		IntSet queuedChunks = new IntArraySet();
		if (loader instanceof PolarLoader polarLoader) {
			Collection<PolarChunk> chunks = polarLoader.world().chunks();
			if (!chunks.isEmpty()) loadServerRenderDistance = false;
			for (PolarChunk chunk : chunks) {
				int x = chunk.x();
				int z = chunk.z();
				addNearbyChunks(pack(x, z), queuedChunks);
			}
		} else if (loader instanceof AnvilLoader) {
			if (!phonyAnvilLoader && file != null) {
				try {
					int loadedChunks = 0;
					File regionFolder = new File(file, "region");
					if (regionFolder.isDirectory()) {
						File[] mcaFiles = regionFolder.listFiles((_, name) -> name.endsWith(".mca"));
						if (mcaFiles != null) {
							for (File mcaFile : mcaFiles) {
								String[] parts = mcaFile.getName().split("\\.");
								int regionX = Integer.parseInt(parts[1]);
								int regionZ = Integer.parseInt(parts[2]);
								MiniRegionFile miniRegionFile = new MiniRegionFile(mcaFile);
								for (int localX = 0; localX < 32; localX++) {
									for (int localZ = 0; localZ < 32; localZ++) {
										int chunkX = regionX * 32 + localX;
										int chunkZ = regionZ * 32 + localZ;
										if (!miniRegionFile.hasChunkData(chunkX, chunkZ)) continue;
										loadedChunks++;
										addNearbyChunks(pack(chunkX, chunkZ), queuedChunks);
									}
								}
							}
						}
					}
					loadServerRenderDistance = loadedChunks == 0;
				} catch (Exception e) {
					System.err.println("Runtime error occurred while trying to preload an anvil world: " + e.getMessage());
					return; // don't set chunk loader to no op
				}
			}
		}

		// this will only be called if a provided world file that we're loading from doesn't exist or has no chunks in it
		if (loadServerRenderDistance) {
			int chunkViewDistancePlus = chunkViewDistance+1;
			for (int x = -chunkViewDistancePlus; x < chunkViewDistancePlus; x++) {
				for (int z = -chunkViewDistancePlus; z < chunkViewDistancePlus; z++) {
					addNearbyChunks(pack(x, z), queuedChunks);
				}
			}
			for (int chunk : queuedChunks) {
				int x = getChunkX(chunk);
				int z = getChunkZ(chunk);
				loadRenderDistanceChunk(container, x, z, strict, chunkViewDistance);
			}
		} else {
			for (int chunk : queuedChunks) {
				int x = getChunkX(chunk);
				int z = getChunkZ(chunk);
				CompletableFuture<Chunk> future = container.loadChunk(x, z);
				if (biome != null) future.whenComplete((chunk1, throwable) -> {
					if (throwable != null) return;
					fillChunkWithBiome(container, chunk1, biome);
				});
				if (!asyncPreload) future.join();
			}
		}
		if (strict) {
			container.enableAutoChunkLoad(false);
			container.setChunkLoader(ChunkLoader.noop());
		}
	}

	// dut_
	private void addNearbyChunks(int coordinates, IntSet set) {
		int originX = getChunkX(coordinates);
		int originZ = getChunkZ(coordinates);
		for (int x = -1; x < 2; x++) {
			for (int z = -1; z < 2; z++) {
				int newChunkX = originX+x;
				int newChunkZ = originZ+z;
				set.add(pack(newChunkX, newChunkZ));
			}
		}
	}

	private int pack(int x, int z) {
		return (z << 16) | (x & 0xFFFF);
	}

	private int getChunkX(int packed) {
		return (short) packed;
	}

	private int getChunkZ(int packed) {
		return (short) (packed >>> 16);
	}

	private void loadRenderDistanceChunk(Instance container, int x, int z, boolean strict, int chunkViewDistance) {
		if (container.isChunkLoaded(x, z)) return;
		if (strict && (Math.abs(x) > chunkViewDistance || Math.abs(z) > chunkViewDistance)) { // loading outside chunks, shouldn't have extra blocks if we're about to generate
			container.generateChunk(x, z, BLANK_GENERATOR);
			return;
		}
		container.loadChunk(x, z);
	}

	/**
	 * Loads chunks around a point in a square fashion, ensuring there's a border of empty chunks
	 *
	 * @param instance the {@link Instance} to load the chunks in
	 */
	private void loadNearbyChunks(Instance instance, int originX, int originZ, ChunkLoadOperation operation) {
		for (int x = -1; x < 2; x++) {
			for (int z = -1; z < 2; z++) {
				int newChunkX = originX+x;
				int newChunkZ = originZ+z;
				operation.loadChunk(instance, newChunkX, newChunkZ);
			}
		}
	}

	private void loadNearbyChunks(Instance instance, Chunk originChunk) {
		loadNearbyChunks(instance, originChunk.getChunkX(), originChunk.getChunkZ(), Instance::loadChunk);
	}

	@FunctionalInterface
	interface ChunkLoadOperation {

		void loadChunk(Instance instance, int chunkX, int chunkZ);

	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "todo sec create instance toString"; // todo finish toString
	}

}
