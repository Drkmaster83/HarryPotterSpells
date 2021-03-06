package com.lavacraftserver.HarryPotterSpells;

import java.util.HashMap;
import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.lavacraftserver.HarryPotterSpells.Spells.Spell;
import com.lavacraftserver.HarryPotterSpells.Utils.Targeter;

public class Listeners implements Listener {
	public HashMap<String, Integer> currentSpell = new HashMap<String, Integer>();
	HarryPotterSpells plugin;
	
	public Listeners(HarryPotterSpells instance) {
		plugin=instance;
	}
	
	@EventHandler
	public void PIE(PlayerInteractEvent e) {
		if(plugin.PM.hasPermission("HarryPotterSpells.use", e.getPlayer()) && plugin.Wand.isWand(e.getPlayer().getItemInHand())) {
			//Change spell
			if(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				Player p = e.getPlayer();
				List<String> spellList = plugin.PlayerSpellConfig.getPSC().getStringList(p.getName());
				if(spellList == null || spellList.isEmpty()) {
					plugin.PM.tell(p, "You don't know any spells.");
					return;
				}
				int spellNumber = 0, max = spellList.size() - 1;
				if(currentSpell.containsKey(p.getName())) {
					if(p.isSneaking()) {
						if(currentSpell.get(p.getName()) == 0) {
							spellNumber = max;
						} else {
							spellNumber = currentSpell.get(p.getName()) - 1;
						}
					} else if(!(currentSpell.get(p.getName()) == max)) {
						spellNumber = currentSpell.get(p.getName()) + 1;
					}
				}
				plugin.PM.newSpell(p, spellList.get(spellNumber));
				currentSpell.put(p.getName(), spellNumber);
				return;
			}
			
			//Cast spell
			if(e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				Player p = e.getPlayer();
				List<String> spellList = plugin.PlayerSpellConfig.getPSC().getStringList(p.getName());
				if(spellList == null || spellList.isEmpty()) {
					plugin.PM.tell(p, "You don't know any spells.");
					return;
				}
				int spellNumber = 0;
				if(currentSpell.containsKey(p.getName())) {
					spellNumber = currentSpell.get(p.getName());
				}
				Location l = p.getLocation();
				l.setY(l.getY() + 1);
				if(plugin.getConfig().getBoolean("spell-particle-toggle")) {
					p.getWorld().playEffect(l, Effect.ENDER_SIGNAL, 0);
				}
				Location loc;
				Spell spell = plugin.spellManager.getSpell(spellList.get(spellNumber));
				if(plugin.Targeter.isTargetEntity(p, spell.range)) {
					loc = Targeter.getTargetNoMessage(p, spell.range).getLocation();
				} else {
					loc = p.getTargetBlock(plugin.Targeter.transparentBlocks(), spell.range).getLocation();
				}
				if(plugin.spellManager.canCastSpell(p, spell, l, loc) == 0) {
					spell.cast(p);
					plugin.LogBlock.logSpell(p, spellList.get(spellNumber));
					//Cancel event if player is in creative to prevent block damage.
					if (p.getGameMode().equals(GameMode.CREATIVE)){
						e.setCancelled(true);
					}
				} else {
					plugin.PM.warn(p, plugin.spellManager.getCastSpellErrorMessage(plugin.spellManager.canCastSpell(p, spell, l, loc)));
				}
			}
			
		}
		
		//Spell sign
		if (e.getClickedBlock() != null && plugin.PM.hasPermission("HarryPotterSpells.buyfromsign", e.getPlayer()) && e.getClickedBlock().getType() == Material.SIGN) {
			String[] signText = ((Sign)e.getClickedBlock()).getLines();
			String identifier = "[" + ChatColor.GREEN + plugin.getConfig().getString("SpellSigns.textForLine1") + ChatColor.RESET + "]";
			if(signText[0].equals(identifier)) {
				double amount = (double)Integer.parseInt(signText[2]);
				Spell spell = plugin.spellManager.getSpell(signText[1]);
				EconomyResponse r = plugin.Vault.econ.depositPlayer(e.getPlayer().getName(), amount);
				if(spell.playerKnows(e.getPlayer())) {
					plugin.PM.warn(e.getPlayer(), "You already know that spell.");
				} else {
					if(r.transactionSuccess()) {
						plugin.PM.tell(e.getPlayer(), plugin.Vault.econ.format(amount) + " has been deducted from your account.");
						spell.teach(e.getPlayer());
					} else {
						plugin.PM.warn(e.getPlayer(), "An error occured during the transation: " + r.errorMessage);
					}
				}
			}
		}
	}
	
	@EventHandler
	public void PIEE(PlayerInteractEntityEvent e) {
		if(plugin.PM.hasPermission("HarryPotterSpells.use", e.getPlayer()) && plugin.Wand.isWand(e.getPlayer().getItemInHand())) {
			Player p = e.getPlayer();
			List<String> spellList = plugin.PlayerSpellConfig.getPSC().getStringList(p.getName());
			if(spellList == null || spellList.isEmpty()) {
				plugin.PM.tell(p, "You don't know any spells.");
				return;
			}
			int spellNumber = 0, max = spellList.size() - 1;
			if(currentSpell.containsKey(p.getName())) {
				if(p.isSneaking()) {
					if(currentSpell.get(p.getName()) == 0) {
						spellNumber = max;
					} else {
						spellNumber = currentSpell.get(p.getName()) - 1;
					}
				} else if(!(currentSpell.get(p.getName()) == max)) {
					spellNumber = currentSpell.get(p.getName()) + 1;
				}
			}
			plugin.PM.newSpell(p, spellList.get(spellNumber));
			currentSpell.put(p.getName(), spellNumber);
			return;
		}
	}
	
	@EventHandler
	public void onPlayerBlockPlace(BlockPlaceEvent e) {
		if(plugin.PM.hasPermission("HarryPotterSpells.sign.create", e.getPlayer()) && e.getBlockPlaced().getType() == Material.SIGN) {
			Sign sign = (Sign)e.getBlockPlaced();
			String identifier = "[" + plugin.getConfig().getString("SpellSigns.textForLine1") + "]";
			String[] signText = sign.getLines();
			boolean error = false;
			if(signText[0].equals(identifier)) {
				if(!plugin.spellManager.isSpell(signText[1])) {
					sign.setLine(1, "[" + ChatColor.RED + plugin.getConfig().getString("SpellSigns.textForLine1") + ChatColor.RESET + "]");
					plugin.PM.warn(e.getPlayer(), "That spell was not recognized.");
					error = true;
				} else {
					error = false;
				}
				if(!Double.isNaN((double)Integer.parseInt(signText[2]))) {
					sign.setLine(1, "[" + ChatColor.RED + plugin.getConfig().getString("SpellSigns.textForLine1") + ChatColor.RESET + "]");
					plugin.PM.warn(e.getPlayer(), "The price is not a number.");
					error = true;
				} else {
					error = false;
				}
				if(!error) {
					sign.setLine(1, "[" + ChatColor.GREEN + plugin.getConfig().getString("SpellSigns.textForLine1") + ChatColor.RESET + "]");
					plugin.PM.tell(e.getPlayer(), "SpellSign created!");
				}
				sign.update();
			}
		}
	}

}
