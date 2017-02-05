package solo.transfer;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.EntityMetadata;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.network.protocol.AddEntityPacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.MoveEntityPacket;
import cn.nukkit.network.protocol.RemoveEntityPacket;
import cn.nukkit.scheduler.Task;
import solo.solobasepackage.util.Message;

public class TransferButton extends Position{
	
	public String ip;
	public short port;
	
	public AddEntityPacket addPk;
	public MoveEntityPacket movePk;
	public RemoveEntityPacket removePk;
	
	public TransferButton(Position pos, String ip, short port){
		super(pos.x, pos.y, pos.z, pos.level);
		this.ip = ip;
		this.port = port;
		this.init();
	}
	
	public void init(){
		long eid = Entity.entityCount++;

		this.addPk = new AddEntityPacket();
		this.addPk.entityUniqueId = eid;
		this.addPk.entityRuntimeId = eid;
		this.addPk.type = 15;
		this.addPk.x = (float) (this.getFloorX() + 0.5);
		this.addPk.y = (float) (this.getFloorY() + 0.2);
		this.addPk.z = (float) (this.getFloorZ() + 0.5);
		this.addPk.speedX = 0;
		this.addPk.speedY = 0;
		this.addPk.speedZ = 0;
		this.addPk.yaw = 0;
		this.addPk.pitch = 0;
		
		long flags = 0;
		flags |= 1 << Entity.DATA_FLAG_INVISIBLE;
		flags |= 1 << Entity.DATA_FLAG_CAN_SHOW_NAMETAG;
		flags |= 1 << Entity.DATA_FLAG_ALWAYS_SHOW_NAMETAG;
		flags |= 1 << Entity.DATA_FLAG_NO_AI;
		EntityMetadata metadata = new EntityMetadata()
				.putLong(Entity.DATA_FLAGS, flags)
				.putShort(Entity.DATA_AIR, 400)
				.putShort(Entity.DATA_MAX_AIR, 400)
				.putString(Entity.DATA_NAMETAG, "§b§l" + "§b§l터치시 서버이동§r\n§oIP : " + this.ip + "§r\n§oPORT : " + Short.toString(this.port))
				.putLong(Entity.DATA_LEAD_HOLDER_EID, -1)
				.putFloat(Entity.DATA_SCALE, 0.0001f);
		
		this.addPk.metadata = metadata;
		
		this.movePk = new MoveEntityPacket();
		this.movePk.eid = eid;
		this.movePk.x = (float) (this.getFloorX() + 0.5);
		this.movePk.y = (float) (this.getFloorY() + 0.2);
		this.movePk.z = (float) (this.getFloorZ() + 0.5);
		
		this.removePk = new RemoveEntityPacket();
		this.removePk.eid = eid;
	}
	
	public void onTouch(Player player){
		Message.normal(player, "곧 다른 서버로 이동됩니다...");
		Server.getInstance().getScheduler().scheduleDelayedTask(new Task(){
			@Override
			public void onRun(int currentTick) {
				DataPacket pk = new DataPacket(){
					public static final byte NETWORK_ID = 0x52; // TransferPacket
						 
					public String address = TransferButton.this.ip; // Server address
					public short port = TransferButton.this.port; // Server port
						 
					@Override
					public void decode() {
						this.address = this.getString();
						this.port = (short) this.getLShort();
					}
					 
					@Override
					public void encode() {
						this.reset();
						this.putString(address);
						this.putLShort(port);
					}
					 
					@Override
					public byte pid() {
						return NETWORK_ID;
					}
				};
				player.dataPacket(pk);
			}
		}, 30);
	}
	
	public void despawnFrom(Player player){
		player.dataPacket(this.removePk);
	}
	
	public void despawnFromAll(){
		Server.getInstance().getOnlinePlayers().values().forEach((p) -> this.despawnFrom(p));
	}
	
	public void spawnTo(Player player){
		this.spawnTo(player, player.getLevel());
	}
	
	public void spawnTo(Player player, Level level){
		if(level == null || ! this.level.getFolderName().equals(level.getFolderName())){
			return;
		}
		player.dataPacket(this.addPk);
		player.dataPacket(this.movePk);
	}
	
	public void spawnToAll(){
		Server.getInstance().getOnlinePlayers().values().forEach((p) -> this.spawnTo(p));
	}
	
}