package net.querz.mcaselector.tiles;

import javafx.scene.image.Image;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.ui.Color;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.ui.ImageHelper;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class Tile {

	public static Color REGION_GRID_COLOR = Color.BLACK;
	public static Color CHUNK_GRID_COLOR = Color.DARKGRAY;
	public static Color EMPTY_CHUNK_BACKGROUND_COLOR = Color.BLACK;
	public static Color EMPTY_COLOR = new Color(0.2, 0.2, 0.2, 1);
	public static double GRID_LINE_WIDTH = 0.5;

	public static final int SIZE = 512;
	public static final int CHUNK_SIZE = 16;
	public static final int SIZE_IN_CHUNKS = 32;
	public static final int CHUNKS = 1024;

	Point2i location;

	Image image;
	Image markedChunksImage;

	boolean loading = false;
	boolean loaded = false;
	boolean marked = false;
	//a set of all marked chunks in the tile in block locations
	Set<Point2i> markedChunks = new HashSet<>();

	public Tile(Point2i location) {
		this.location = location;
	}

	public static int getZoomLevel(float scale) {
		int b = 1;
		while (b <= scale) {
			b = b << 1;
		}
		return (int) Math.ceil(b / 2.0);
	}

	public boolean isVisible(TileMap tileMap) {
		return isVisible(tileMap, 0);
	}

	//returns whether this tile is visible on screen, adding a custom radius
	//as a threshold
	//threshold is measured in tiles
	public boolean isVisible(TileMap tileMap, int threshold) {
		Point2i o = tileMap.getOffset().toPoint2i();
		Point2i min = o.sub(threshold * SIZE).blockToRegion().regionToBlock();
		Point2i max = new Point2i(
				(int) (o.getX() + tileMap.getWidth() * tileMap.getScale()),
				(int) (o.getY() + tileMap.getHeight() * tileMap.getScale())).add(threshold * SIZE).blockToRegion().regionToBlock();
		return location.getX() * SIZE >= min.getX() && location.getY() * SIZE >= min.getY()
				&& location.getX() * SIZE <= max.getX() && location.getY() * SIZE <= max.getY();
	}

	public Image getImage() {
		return image;
	}

	public Point2i getLocation() {
		return location;
	}

	public boolean isEmpty() {
		return image == null || image == ImageHelper.getEmptyTileImage();
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public void setLoading(boolean loading) {
		this.loading = loading;
	}

	public boolean isLoading() {
		return loading;
	}

	public void unload() {
		if (image != null) {
			image.cancel();
			image = null;
		}
		if (markedChunksImage != null) {
			markedChunksImage.cancel();
			markedChunksImage = null;
		}
		loaded = false;
	}

	public void mark(boolean marked) {
		this.marked = marked;
		if (marked) {
			markedChunks.clear();
			markedChunksImage = null;
		}
	}

	public void mark(Point2i chunkBlock) {
		markedChunks.add(chunkBlock);
		if (markedChunks.size() == CHUNKS) {
			mark(true);
		} else {
			markedChunksImage = null; //reset markedChunksImage
		}
	}

	public boolean isMarked() {
		return marked;
	}

	public boolean isMarked(Point2i chunkBlock) {
		return isMarked() || markedChunks.contains(chunkBlock);
	}

	public void unMark(Point2i chunkBlock) {
		if (isMarked()) {
			Point2i regionChunk = location.regionToChunk();
			for (int x = 0; x < SIZE_IN_CHUNKS; x++) {
				for (int z = 0; z < SIZE_IN_CHUNKS; z++) {
					markedChunks.add(regionChunk.add(x, z));
				}
			}
			mark(false);
		}
		markedChunks.remove(chunkBlock);
		markedChunksImage = null; //reset markedChunksImage
	}

	public void clearMarks() {
		mark(false);
		markedChunks.clear();
	}

	public Set<Point2i> getMarkedChunks() {
		return markedChunks;
	}



	public File getMCAFile() {
		return FileHelper.createMCAFilePath(location);
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public void loadFromCache(Runnable callback, Supplier<Float> scaleSupplier) {
		if (loaded) {
			Debug.dump("region at " + location + " already loaded");
			return;
		}

		if (Config.getCacheDir() == null) {
			//load empty map (start screen)
			loaded = true;
			callback.run();
			return;
		}

		String res = String.format(Config.getCacheDir().getAbsolutePath() + "/" + getZoomLevel(scaleSupplier.get()) + "/r.%d.%d.png", location.getX(), location.getY());

		Debug.dump("loading region " + location + " from cache: " + res);

		try (InputStream inputStream = new FileInputStream(res)) {
			image = new Image(inputStream);
			loaded = true;
			callback.run();
		} catch (IOException ex) {
			Debug.dump("region " + location + " not cached");
		}
	}
}
