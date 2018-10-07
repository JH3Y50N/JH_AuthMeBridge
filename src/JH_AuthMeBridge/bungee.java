package JH_AuthMeBridge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class bungee extends Plugin implements Listener  {

	@Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, this);
        try {
			loadConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
        getProxy().getPluginManager().registerCommand(this, new Command("jh_authmebridge") {
			@Override
			public void execute(CommandSender sender, String[] args) {
				if(!sender.hasPermission("jh_authmebridge.reload")){
					sender.sendMessage(new TextComponent("§cYou do not have permission to run this command!"));
					return;
				}
				 try {
					loadConfig();
					sender.sendMessage(new TextComponent("§aSuccessfully reloaded configuration"));
				} catch (IOException e) {
					sender.sendMessage(new TextComponent("§aError loading configuration errors were shown in console"));
					e.printStackTrace();
				}
				
			}
		});
        System.out.println("[JH_AuthMeBridge] It started successfully.");
	}
	
	Configuration config;
	private void loadConfig() throws IOException{
		if(!getDataFolder().exists()){
			getDataFolder().mkdir();
		}
		
		File file = new File(getDataFolder(), "config.yml");
		
		if(!file.exists()){
			file.createNewFile();
			try (InputStream in = getResourceAsStream("config.yml");
				OutputStream out = new FileOutputStream(file)) {
				ByteStreams.copy(in, out);
			}
		}
		config = YamlConfiguration.getProvider(YamlConfiguration.class).load(file);
		comandos.clear();
		lobbyes.clear();
		for(String s : getConfig().getStringList("CommandsAllowed")){
			comandos.add(s.toLowerCase());
		}
		for(String s : getConfig().getStringList("Lobbyes")){
			lobbyes.add(s.toLowerCase());
		}
	}
	private Configuration getConfig()
	{
	    return config;
	}
	public void saveConfig(String config){
		File file = new File(getDataFolder(), "config.yml");
		try {
			YamlConfiguration.getProvider(YamlConfiguration.class).save(getConfig(), file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	List<String> lobbyes = new ArrayList<>();
	HashMap<String, Boolean> logados = new HashMap<String, Boolean>();
	List<String> comandos = new ArrayList<>();
    @EventHandler
    public void onPluginMessage(PluginMessageEvent e){
    	if (e.getTag().equalsIgnoreCase("BungeeCord")) {          
            try {
            	DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));
                String channel = in.readUTF();
                if(channel.equals("JH_AuthMeBridge")){
                    String input = in.readUTF();
                    if(isLogged(input))return;
                    logados.put(input, true);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
      
        }
    }
    @EventHandler
	public void onPlayerLeave(PlayerDisconnectEvent event) {
    	if(isLogged(event.getPlayer())){
    		logados.remove(event.getPlayer().getName());
    	}
    }
    
    @EventHandler(priority=EventPriority.LOWEST)
	public void onChat(ChatEvent event){
		if (event.isCancelled())return;
		if (!(event.getSender() instanceof ProxiedPlayer))return;
		ProxiedPlayer player = (ProxiedPlayer)event.getSender();
		if(isLogged(player))return;
		if(event.isCommand()){			
			String command = event.getMessage().split(" ")[0].toLowerCase();
			if(comandos.contains(command)){
				return;
			}
		}
		if(!isLogged(player)){
			event.setCancelled(true);
			TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', getConfig().getString("NeedLoginToEnter")));
			player.sendMessage(message);
		}
	}
    
    @EventHandler
	public void onServerSwitch(ServerSwitchEvent event) {
    	if(isLogged(event.getPlayer())){  		
            try {
            	ByteArrayOutputStream stream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(stream);
                out.writeUTF("JH_AuthMeBridge");
                out.writeUTF(event.getPlayer().getName());
                event.getPlayer().getServer().sendData("BungeeCord", stream.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }    
            return;
    	}	
		if(!lobbyes.contains(event.getPlayer().getServer().getInfo().getName().toLowerCase())) {
			TextComponent kickReason = new TextComponent(ChatColor.translateAlternateColorCodes('&', getConfig().getString("KickMessageNeedLogged")));
			kickReason.setColor(ChatColor.RED);
			event.getPlayer().disconnect(kickReason);
		}
    }
    
    public Boolean isLogged(ProxiedPlayer player){
    	return logados.containsKey(player.getName());
    }
    
    public Boolean isLogged(String player){
    	return logados.containsKey(player);
    }
}
