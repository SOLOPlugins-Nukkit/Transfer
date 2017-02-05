package solo.transfer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityLevelChangeEvent;
import cn.nukkit.event.level.LevelLoadEvent;
import cn.nukkit.event.level.LevelUnloadEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import solo.solobasepackage.util.Message;

public class Main extends PluginBase implements Listener{
	
	public Config config;
	public Map<String, String> data = new HashMap<>();
	public Map<String, String> data2 = new HashMap<>();
	
	public HashMap<String, TransferButton> buttons = new HashMap<String, TransferButton>();
	public HashMap<String, String> queue = new HashMap<String, String>();
	
	@Override
	public void onEnable(){
		this.getDataFolder().mkdirs();
		this.config = new Config(new File(this.getDataFolder(), "buttons.yml"), Config.YAML);
		this.config.getAll().forEach((k, v) -> this.data.put(k, (String) v));
		
		this.getServer().getLevels().values().forEach((l) -> this.loadFromLevel(l));
		
		this.getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable(){
		this.getServer().getLevels().values().forEach((l) -> this.unloadFromLevel(l));
		this.config.setAll(new LinkedHashMap<String, Object>(this.data2));
		this.config.save();
	}
	
	public void loadFromLevel(Level level){
		this.data.forEach((k, v) -> {
			String levelName = k.split(":")[0];
			if(levelName.equals(level.getFolderName())){
				try{
					Position pos = new Position(
							Integer.parseInt(k.split(":")[1]),
							Integer.parseInt(k.split(":")[2]),
							Integer.parseInt(k.split(":")[3]),
							level
						);
					String ip = v.split(":")[0];
					Short port = Short.parseShort(v.split(":")[1]);
					TransferButton button = new TransferButton(pos, ip, port);
					button.spawnToAll();
					this.buttons.put(this.getHash(button), button);
				}catch(Exception e){
					
				}
			}
		});
	}
	
	public void unloadFromLevel(Level level){
		HashSet<String> remove = new HashSet<String>();
		this.buttons.forEach((k, b) -> {
			if(b.getLevel().getFolderName().equals(level.getFolderName())){
				this.data2.put(k, b.ip + ":" + Short.toString(b.port));
				b.despawnFromAll();
				remove.add(k);
			}
		});
		remove.forEach((k) -> this.buttons.remove(k));
	}
	
	@EventHandler
	public void onLevelLoad(LevelLoadEvent event){
		this.loadFromLevel(event.getLevel());
	}
	
	@EventHandler
	public void onLevelUnload(LevelUnloadEvent event){
		this.unloadFromLevel(event.getLevel());
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event){
		String name = event.getPlayer().getName().toLowerCase();
		String hash = this.getHash(event.getBlock());
		if(this.queue.containsKey(name)){
			String value = this.queue.get(name);
			if(this.buttons.containsKey(hash)){
				if(value.equals("REMOVE")){
					this.buttons.remove(hash).despawnFromAll();
					Message.normal(event.getPlayer(), "성공적으로 서버이동 버튼을 제거하였습니다.");
					this.queue.remove(name);
					
					event.setCancelled();
					return;
				}
			}else{
				String ip = value.split(":")[0];
				short port = Short.parseShort(value.split(":")[1]);
				TransferButton button = new TransferButton(event.getBlock(), ip, port);
				button.spawnToAll();
				this.buttons.put(this.getHash(button), button);
				Message.normal(event.getPlayer(), "성공적으로 서버이통 버튼을 생성하였습니다.");
				this.queue.remove(name);
				
				event.setCancelled();
			}
		}else if(this.buttons.containsKey(hash)){
			this.buttons.get(hash).onTouch(event.getPlayer());
			event.setCancelled();
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event){
		this.buttons.values().forEach((b) -> b.spawnTo(event.getPlayer()));
	}
	
	@EventHandler
	public void onLevelChange(EntityLevelChangeEvent event){
		if(event.getEntity() instanceof Player){
			Player player = (Player) event.getEntity();
			this.buttons.values().forEach((b) -> b.spawnTo(player, event.getTarget()));
		}
	}
	
	public String getHash(Position pos){
		return pos.level.getFolderName() + ":" + Integer.toString(pos.getFloorX()) + ":" + Integer.toString(pos.getFloorY()) + ":" + Integer.toString(pos.getFloorZ());
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(command.getName().equals("서버이동버튼")){
			if(args.length == 0){
				args = new String[]{"x"};
			}
			String name = sender.getName().toLowerCase();
			switch(args[0]){
				case "생성":
					String ip;
					short port;
					try{
						ip = args[1];
						port = Short.parseShort(args[2]);
					}catch(Exception e){
						Message.usage(sender, "/서버이동버튼 생성 [ip] [port]");
						return true;
					}
					this.queue.put(name, args[1] + ":" + args[2]);
					Message.normal(sender, "서버이동 버튼을 생성할 곳에 터치하세요.");
					return true;
					
				case "제거":
					this.queue.put(name, "REMOVE");
					Message.normal(sender, "제거할 서버이동 버튼을 터치하세요.");
					return true;
					
				case "취소":
					if(this.queue.containsKey(name)){
						this.queue.remove(name);
						Message.normal(sender, "진행중이던 작업을 취소하였습니다.");
					}else{
						Message.alert(sender, "진행중인 작업이 없습니다.");
					}
					return true;
					
				default:
					ArrayList<String> information = new ArrayList<String>();
					information.add("§2§l/서버이동버튼 생성 [ip] [port] §r§7- 서버이동 버튼을 생성합니다.");
					information.add("§2§l/서버이동버튼 제거 §r§7- 서버이동 버튼을 제거합니다.");
					information.add("§2§l/서버이동버튼 취소 §r§7- 진행중이던 작업을 취소합니다.");
					int page = 1;
					try{
						page = Integer.parseInt(args[1]);
					}catch(Exception e){
						
					}
					Message.page(sender, "서버이동버튼 명령어 목록", information, page);
			}
		}
		return true;
	}
	
}