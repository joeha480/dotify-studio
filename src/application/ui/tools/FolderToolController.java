package application.ui.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import application.l10n.Messages;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

/**
 * Provides a controller for the search view.
 * @author Joel Håkansson
 *
 */
public class FolderToolController extends BorderPane {
	private static final Logger logger = Logger.getLogger(FolderToolController.class.getCanonicalName());
	private static final Node rootIcon = new ImageView(
			new Image(FolderToolController.class.getResourceAsStream("folder_16.png"))
			);
	private static final Image depIcon = 
			new Image(FolderToolController.class.getResourceAsStream("department.png"));
	TreeItem<PathInfo> rootNode = 
			new FileTreeItem();
	@FXML TreeView<PathInfo> tree;
	private final Consumer<Path> action;

	/**
	 * Creates a new search view controller.
	 */
	public FolderToolController(Consumer<Path> action) {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FolderTool.fxml"), Messages.getBundle());
			fxmlLoader.setRoot(this);
			fxmlLoader.setController(this);
			fxmlLoader.load();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to load view", e);
		}
		this.action = action;
	}
	
	@FXML void initialize() {
		tree.setShowRoot(false);
		tree.setRoot(rootNode);
		tree.setOnMouseClicked(ev->{
			if (ev.getClickCount()>=2) {
				TreeItem<PathInfo> item = tree.getSelectionModel().getSelectedItem();
				if (item!=null && item.getValue()!=null && !Files.isDirectory(item.getValue().getPath())) {
					action.accept(item.getValue().getPath());
				}
			}
		});
	}
	
	public void addPath(Path p) {
		if (Files.isDirectory(p)) {
			FileTreeItem depNode = new FileTreeItem(
					new PathInfo(p, true), 
					new ImageView(depIcon)
				);
			rootNode.getChildren().add(depNode);
			setPath(p, depNode);
		}
	}
	
	public void removePath(Path p) {
		rootNode.getChildren().removeIf(v->v.getValue().equals(p));
	}
	
	private static void setPath(Path p, FileTreeItem node) {
		try {
			Files.list(p)
			.sorted((p1, p2)->{
				if (Files.isDirectory(p1) && !Files.isDirectory(p2)) {
					return -1;
				} else if (!Files.isDirectory(p1) && Files.isDirectory(p2)) {
					return 1;
				} else {
					// Both are files or both are directories
					return p1.compareTo(p2);
				}
				})
			.forEach(v-> {
				FileTreeItem empLeaf = new FileTreeItem(v);
				if (Files.isDirectory(v)) {
					empLeaf.expandedProperty().addListener((o, ov, nv)->{
						if (nv.booleanValue()) {
							empLeaf.getChildren().clear();
							setPath(v, empLeaf);
						}
					});
				}
				node.getChildren().add(empLeaf);
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static class PathInfo {
		private final Path path;
		private final boolean fullPath;
		
		PathInfo(Path p) {
			this.path = p;
			this.fullPath = false;
		}
		
		PathInfo(Path p, boolean fullPath) {
			this.path = p;
			this.fullPath = fullPath;
		}
		

		@Override
		public String toString() {
			return fullPath?path.toString():path.getName(path.getNameCount()-1).toString();
		}

		public Path getPath() {
			return path;
		}
		
	}
	
	public static class FileTreeItem extends TreeItem<PathInfo> {

		/**
		 * 
		 */
		public FileTreeItem() {
			// TODO Auto-generated constructor stub
		}

		/**
		 * @param value
		 * @param graphic
		 */
		public FileTreeItem(PathInfo value, Node graphic) {
			super(value, graphic);
		}
		
		public FileTreeItem(Path value, Node graphic) {
			super(new PathInfo(value), graphic);
		}

		/**
		 * @param value
		 */
		public FileTreeItem(Path value) {
			super(new PathInfo(value));
		}

		@Override
		public boolean isLeaf() {
			return Optional.ofNullable(getValue()).map(v->!Files.isDirectory(v.getPath())).orElse(false);
		}
		
		
		// Implement isLeaf here to support folders properly
	}

}
