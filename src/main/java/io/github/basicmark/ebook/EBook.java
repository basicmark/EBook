package io.github.basicmark.ebook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class EBook extends JavaPlugin implements Listener {
	static final String displayPrefix = "" + ChatColor.BOLD + ChatColor.AQUA;
	List<String> starterBooks;
	private Map<String, List<String>> ebooks;

	public void loadConfig() {
		FileConfiguration config = getConfig();
		
		if (config != null) {
			ConfigurationSection ebookConfigs = config.getConfigurationSection("ebooks");

			if (ebookConfigs != null) {
				for (String ebookName : ebookConfigs.getKeys(false)) {
					getServer().getLogger().info("Loading E-Book " + ebookName);
					List<String> raw_pages = ebookConfigs.getStringList(ebookName + ".pages");
					List<String> pages = new ArrayList<String>();

					/* Replace & with the correct formating char */
					for (String raw_page : raw_pages) {
						pages.add(raw_page.replace('&', ChatColor.COLOR_CHAR));
					}
					ebooks.put(ebookName, pages);
				}
			}
			
			starterBooks = config.getStringList("starter");
		}
	}

	public void onEnable(){
		getLogger().info("Enabling EBook");

		ebooks = new HashMap<String, List<String>>();

		// Create/load the config file
		saveDefaultConfig();
		loadConfig();
		getServer().getPluginManager().registerEvents(this, this);
	}

	public void onDisable(){
		getLogger().info("Disabling EBook");
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("ebook")){
			if ((args.length == 1) && args[0].equals("reload")) {
				if (sender.hasPermission("ebook.cmd.reload")) {
					reloadConfig();
					loadConfig();
				} else {
					sender.sendMessage(ChatColor.RED + "You don't have permission to run this command");
				}
				return true;
			} else if ((args.length == 1) && ebooks.containsKey(args[0])) {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					givePlayer(player, args[0]);
				} else {
					sender.sendMessage(ChatColor.RED + "This command can only be issued by players");
				}
				return true;
			}
		}
		return false;
	}

	private String getDisplayName(String bookName) {
		return "" + displayPrefix + bookName.replace('_', ' ');
	}
	
	private String getName(String bookDisplayName) {
		return bookDisplayName.replace(displayPrefix, "").replace(' ', '_');
	}

	private void givePlayer(Player player, String book) {
		ItemStack newBook = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta bookMeta = (BookMeta) newBook.getItemMeta();
		List<String> customPages = ebooks.get(book);

		bookMeta.setDisplayName(getDisplayName(book));
		bookMeta.setPages(customPages);
		newBook.setItemMeta(bookMeta);

		HashMap<Integer,ItemStack> remainingItems = player.getInventory().addItem(newBook);
		if (!remainingItems.isEmpty()) {
			/* There should only ever be one item but better safe then sorry! */
			Iterator<ItemStack> isi = remainingItems.values().iterator();
			while (isi.hasNext()) {
				ItemStack dropItem = isi.next();
				player.getWorld().dropItemNaturally(player.getLocation(), dropItem);
			}
		}
		player.updateInventory();
	}

	private boolean isEBook(ItemStack item) {
		if (item.getType().equals((Material.WRITTEN_BOOK))) {
			ItemMeta meta = item.getItemMeta();
			if (meta.getDisplayName() == null)
				return false;
			if (ebooks.containsKey(getName(meta.getDisplayName()))) {
				return true;
			}
		}
		return false;
	}

	private List<String> getPages(ItemStack item) {
		if (isEBook(item)) {
			ItemMeta meta = item.getItemMeta();
			return ebooks.get(getName(meta.getDisplayName()));
		}
		return null;
	}

	@EventHandler
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if ((player.getLastPlayed() == 0) && (starterBooks != null)) {
			for (String book: starterBooks) {
				givePlayer(player, book);
			}
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		ItemStack clickedItem = event.getItem();
		Player player = event.getPlayer();
		if ((clickedItem != null) && isEBook(clickedItem)) {

			BookMeta bookMeta = (BookMeta) clickedItem.getItemMeta();
			List<String> customPages = getPages(clickedItem);

			/*
			 * Loading the new pages into the book causes extra control characters to be added
			 * so before we can do that check we must load and retrieve the (modified) pages. 
			 */
			List<String> currentPages = bookMeta.getPages();
			bookMeta.setPages(customPages);
			clickedItem.setItemMeta(bookMeta);
			bookMeta = (BookMeta) clickedItem.getItemMeta();

			if (!bookMeta.getPages().equals(currentPages)) {
				player.sendMessage("Your E-Book has recieved an update! Please reopen the book to see the new pages.");
			}
			clickedItem.setItemMeta(bookMeta);
		}
	}
}

