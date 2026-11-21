package lat.bastionmc.rewards;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public final class BastionMCRewards extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private FileConfiguration progressCfg, state;
    private File stateFile;
    private static final String[] RANKS = {"default","magma","piglin","carmesi","basalto","vikingo","bastion"};
    private final Map<UUID, View> views = new HashMap<>();
    private record View(String type, int page) {}

    @Override public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "progressive.yml").exists()) saveResource("progressive.yml", false);
        loadFiles();
        Objects.requireNonNull(getCommand("recompensas")).setExecutor(this);
        Objects.requireNonNull(getCommand("recompensas")).setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BastionMCRewards: 7 rangos y 100 dias configurables listos.");
    }
    private void loadFiles() {
        reloadConfig();
        progressCfg = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "progressive.yml"));
        stateFile = new File(getDataFolder(), "players.yml");
        state = YamlConfiguration.loadConfiguration(stateFile);
    }
    @Override public void onDisable() { saveState(); }
    private void saveState() { try { state.save(stateFile); } catch(IOException ex) { getLogger().severe("No se pudo guardar players.yml: " + ex.getMessage()); } }
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("bastionrewards.admin")) { tell(sender,"no-permission"); return true; }
            loadFiles(); tell(sender,"reloaded"); return true;
        }
        if (!(sender instanceof Player player)) { sender.sendMessage("Solo jugadores pueden abrir el menu. Usa /recompensas reload."); return true; }
        if (args.length == 0) { openMain(player); return true; }
        if (args[0].equalsIgnoreCase("principales")) { openRanks(player); return true; }
        if (args[0].equalsIgnoreCase("progresivas")) { openProgress(player,0); return true; }
        tell(sender,"usage"); return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length!=1) return List.of();
        List<String> words=new ArrayList<>(List.of("principales","progresivas"));
        if(sender.hasPermission("bastionrewards.admin")) words.add("reload");
        return words.stream().filter(s->s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
    }
    private void tell(CommandSender sender,String key){ sender.sendMessage(color(getConfig().getString("messages.prefix","")+getConfig().getString("messages."+key,""))); }
    private void tell(Player p,String key,Map<String,String> vars){ p.sendMessage(color(repl(getConfig().getString("messages.prefix","")+getConfig().getString("messages."+key,""),vars))); }
    private String repl(String s, Map<String,String> vars) { for(var e: vars.entrySet()) s=s.replace("%"+e.getKey()+"%",e.getValue()); return s; }
    private String color(String input) {
        if(input==null) return "";
        // Apply optional gradient tag, with legacy HEX formatting on every character.
        Pattern grad=Pattern.compile("<gradient:(#[A-Fa-f0-9]{6}):(#[A-Fa-f0-9]{6})>(.*?)</gradient>",Pattern.DOTALL);
        Matcher gm=grad.matcher(input); StringBuffer sb=new StringBuffer();
        while(gm.find()) {
            String content=gm.group(3); int first=Integer.parseInt(gm.group(1).substring(1),16),last=Integer.parseInt(gm.group(2).substring(1),16);
            StringBuilder b=new StringBuilder();
            for(int i=0;i<content.length();i++) { double t=content.length()<2?0:(double)i/(content.length()-1); int r=(int)Math.round(((first>>16)&255)*(1-t)+((last>>16)&255)*t),g=(int)Math.round(((first>>8)&255)*(1-t)+((last>>8)&255)*t),bl=(int)Math.round((first&255)*(1-t)+(last&255)*t); b.append(String.format("&#%02X%02X%02X",r,g,bl)).append(content.charAt(i)); }
            gm.appendReplacement(sb,Matcher.quoteReplacement(b.toString()));
        }
        gm.appendTail(sb);
        Matcher m=Pattern.compile("&#([0-9A-Fa-f]{6})").matcher(sb.toString()); StringBuffer hex=new StringBuffer();
        while(m.find()) { String h=m.group(1); StringBuilder b=new StringBuilder("§x"); for(char c:h.toCharArray())b.append('§').append(c); m.appendReplacement(hex,Matcher.quoteReplacement(b.toString())); }
        m.appendTail(hex); return ChatColor.translateAlternateColorCodes('&',hex.toString());
    }
    private Material mat(String name,Material fallback) { Material m=Material.matchMaterial(name==null?"":name); return m==null?fallback:m; }
    private ItemStack item(String path, FileConfiguration cfg, Material def, String name, List<String> lore, Map<String,String> vars) {
        ItemStack it=new ItemStack(mat(cfg.getString(path+".material"),def), Math.max(1,Math.min(64,cfg.getInt(path+".amount",1))));
        ItemMeta im=it.getItemMeta();
        im.setDisplayName(color(repl(cfg.getString(path+".name",name),vars)));
        List<String> lines=cfg.isList(path+".lore")?cfg.getStringList(path+".lore"):lore;
        im.setLore(lines.stream().map(s->color(repl(s,vars))).toList()); it.setItemMeta(im); return it;
    }
    private Inventory inv(String title,int rows){ return Bukkit.createInventory(null, Math.max(1,Math.min(6,rows))*9,color(title)); }
    private void fill(Inventory inv,String path){
        if(!getConfig().getBoolean(path+".filler.enabled",false)) return;
        Material material=mat(getConfig().getString(path+".filler.material"),Material.GRAY_STAINED_GLASS_PANE);
        ItemStack blank=new ItemStack(material); ItemMeta im=blank.getItemMeta();im.setDisplayName(color(getConfig().getString(path+".filler.name"," ")));blank.setItemMeta(im);
        for(int i=0;i<inv.getSize();i++)if(inv.getItem(i)==null)inv.setItem(i,blank);
    }
    private void set(Inventory inv,int slot,ItemStack it){ if(slot>=0&&slot<inv.getSize())inv.setItem(slot,it); }
    private void openMain(Player p){
        String path="main-menu"; Inventory inv=inv(getConfig().getString(path+".title","Recompensas"),getConfig().getInt(path+".rows",3));
        set(inv,getConfig().getInt(path+".daily.slot",11),item(path+".daily",getConfig(),Material.CHEST,"&ePrincipales",List.of(),Map.of()));
        set(inv,getConfig().getInt(path+".progressive.slot",15),item(path+".progressive",getConfig(),Material.CLOCK,"&bProgresivas",List.of(),Map.of()));
        fill(inv,path); views.put(p.getUniqueId(),new View("main",0));p.openInventory(inv);sound(p,"open");
    }
    private boolean entitled(Player p,String rank){String perm=getConfig().getString("ranks."+rank+".permission","bastionrewards.rank."+rank);return p.hasPermission(perm);}
    private long cooldown(){return Math.max(1,getConfig().getLong("settings.daily-hours",24))*3600000L;}
    private String time(long millis){long secs=Math.max(0,(millis+999)/1000),h=secs/3600,m=(secs%3600)/60;return h+"h "+m+"m";}
    private boolean hasRewards(ConfigurationSection sec){return sec!=null&&(!sec.getStringList("commands").isEmpty()||!sec.getMapList("items").isEmpty());}
    private void openRanks(Player p){
        String path="rank-menu"; Inventory inv=inv(getConfig().getString(path+".title","Principales"),getConfig().getInt(path+".rows",3));
        for(String rank:RANKS){String base="ranks."+rank; ConfigurationSection sec=getConfig().getConfigurationSection(base);if(sec==null)continue;
            long last=state.getLong("players."+p.getUniqueId()+".rank."+rank,0),remain=last+cooldown()-System.currentTimeMillis();
            String status=!entitled(p,rank)?getConfig().getString(path+".locked-lore"):!hasRewards(sec)?getConfig().getString(path+".empty-lore"):remain>0?getConfig().getString(path+".waiting-lore"):getConfig().getString(path+".ready-lore");
            set(inv,getConfig().getInt(base+".slot",10),item(base,getConfig(),Material.CHEST,rank,List.of("%status%"),Map.of("status",status.replace("%time%",time(remain)))));
        }
        set(inv,getConfig().getInt(path+".back.slot",22),item(path+".back",getConfig(),Material.ARROW,"Volver",List.of(),Map.of()));fill(inv,path);
        views.put(p.getUniqueId(),new View("ranks",0));p.openInventory(inv);sound(p,"open");
    }
    private int maxDays(){return Math.max(1,Math.min(100,getConfig().getInt("settings.progressive-days",100)));}
    private int[] rewardSlots(){ List<Integer> list=getConfig().getIntegerList("progressive-menu.reward-slots");if(list.isEmpty())list=List.of(10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43);return list.stream().mapToInt(Integer::intValue).toArray(); }
    private String statePath(Player p){return "players."+p.getUniqueId()+".progressive";}
    private int nextDay(Player p){return Math.min(maxDays()+1,state.getInt(statePath(p)+".claimed",0)+1);}
    private long lastProgress(Player p){return state.getLong(statePath(p)+".last",0);}
    private void checkReset(Player p){
        if(!getConfig().getBoolean("settings.progressive-reset-if-missed",true))return;
        long last=lastProgress(p), gap=Math.max(25,getConfig().getLong("settings.progressive-max-gap-hours",48))*3600000L;
        if(last>0&&nextDay(p)<=maxDays()&&System.currentTimeMillis()-last>gap){state.set(statePath(p)+".claimed",0);state.set(statePath(p)+".last",0);saveState();tell(p,"reset");}
    }
    private ConfigurationSection dayCfg(int day){return progressCfg.getConfigurationSection("days."+day);}
    private void openProgress(Player p,int requestedPage){
        checkReset(p);String path="progressive-menu";int[] slots=rewardSlots();int totalPages=(maxDays()+slots.length-1)/slots.length;int page=Math.max(0,Math.min(totalPages-1,requestedPage));
        Inventory inv=inv(repl(getConfig().getString(path+".title","Racha %page%"),Map.of("page",String.valueOf(page+1))),getConfig().getInt(path+".rows",6));
        int next=nextDay(p); long remain=lastProgress(p)+cooldown()-System.currentTimeMillis();
        for(int i=0;i<slots.length;i++) {int day=page*slots.length+i+1;if(day>maxDays())break;
            String status=day<next?getConfig().getString(path+".claimed-lore"):day>next?getConfig().getString(path+".locked-lore"):!hasRewards(dayCfg(day))?getConfig().getString(path+".empty-lore"):remain>0?getConfig().getString(path+".waiting-lore"):getConfig().getString(path+".available-lore");
            Map<String,String> vars=Map.of("day",String.valueOf(day),"progress",String.valueOf(Math.min(next-1,maxDays())),"status",status.replace("%time%",time(remain)));
            String base="days."+day;
            // User-specific day display values override defaults.
            FileConfiguration display=progressCfg.isConfigurationSection(base)?progressCfg:getConfig();String cfgPath=display==progressCfg?base:path+".item-default";
            set(inv,slots[i],item(cfgPath,display,Material.BARREL,"&bDía #"+day,List.of("%status%"),vars));
        }
        if(page>0)set(inv,getConfig().getInt(path+".previous.slot",45),item(path+".previous",getConfig(),Material.ARROW,"Anterior",List.of(),Map.of()));
        if(page+1<totalPages)set(inv,getConfig().getInt(path+".next.slot",53),item(path+".next",getConfig(),Material.ARROW,"Siguiente",List.of(),Map.of()));
        set(inv,getConfig().getInt(path+".back.slot",49),item(path+".back",getConfig(),Material.BARRIER,"Volver",List.of(),Map.of()));fill(inv,path);
        views.put(p.getUniqueId(),new View("progress",page));p.openInventory(inv);sound(p,"open");
    }
    private void sound(Player p,String key){try{p.playSound(p.getLocation(),org.bukkit.Registry.SOUNDS.get(NamespacedKey.minecraft(getConfig().getString("sounds."+key,"UI_BUTTON_CLICK").toLowerCase(Locale.ROOT).replace('_','.')) ),0.7f,1f);}catch(Exception ignored){}}
    @EventHandler public void onClose(InventoryCloseEvent e){if(e.getPlayer() instanceof Player p)views.remove(p.getUniqueId());}
    @EventHandler public void onQuit(org.bukkit.event.player.PlayerQuitEvent e){views.remove(e.getPlayer().getUniqueId());}
    @EventHandler public void onClick(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;View v=views.get(p.getUniqueId());if(v==null)return;
        e.setCancelled(true);if(e.getClickedInventory()!=e.getView().getTopInventory())return;
        int slot=e.getRawSlot();if(slot<0||slot>=e.getView().getTopInventory().getSize())return;
        if(v.type.equals("main")){
            if(slot==getConfig().getInt("main-menu.daily.slot",11))openRanks(p);
            else if(slot==getConfig().getInt("main-menu.progressive.slot",15))openProgress(p,0);
        }else if(v.type.equals("ranks")){
            if(slot==getConfig().getInt("rank-menu.back.slot",22)){openMain(p);return;}
            for(String rank:RANKS)if(slot==getConfig().getInt("ranks."+rank+".slot",-1)){claimRank(p,rank);return;}
        }else if(v.type.equals("progress")){
            if(slot==getConfig().getInt("progressive-menu.back.slot",49)){openMain(p);return;}
            if(slot==getConfig().getInt("progressive-menu.previous.slot",45)){openProgress(p,v.page-1);return;}
            if(slot==getConfig().getInt("progressive-menu.next.slot",53)){openProgress(p,v.page+1);return;}
            int[] slots=rewardSlots();for(int i=0;i<slots.length;i++)if(slot==slots[i]){claimProgress(p,v.page*slots.length+i+1);return;}
        }
    }
    @EventHandler public void onDrag(InventoryDragEvent e){if(views.containsKey(e.getWhoClicked().getUniqueId()))e.setCancelled(true);}
    private void claimRank(Player p,String rank){
        if(!entitled(p,rank)){tell(p,"no-permission");sound(p,"error");return;}
        ConfigurationSection cfg=getConfig().getConfigurationSection("ranks."+rank);
        if(!hasRewards(cfg)){tell(p,"empty");sound(p,"error");return;}
        String key="players."+p.getUniqueId()+".rank."+rank;long remain=state.getLong(key,0)+cooldown()-System.currentTimeMillis();
        if(remain>0){tell(p,"not-ready",Map.of("time",time(remain)));sound(p,"error");return;}
        if(!deliver(p,cfg)){tell(p,"error");return;}
        state.set(key,System.currentTimeMillis());saveState();tell(p,"claimed",Map.of("name",rank.toUpperCase(Locale.ROOT)));sound(p,"claim");openRanks(p);
    }
    private void claimProgress(Player p,int day){
        checkReset(p);if(day<1||day>maxDays()||day!=nextDay(p)){tell(p,"locked");sound(p,"error");return;}
        ConfigurationSection cfg=dayCfg(day);if(!hasRewards(cfg)){tell(p,"empty");sound(p,"error");return;}
        long remain=lastProgress(p)+cooldown()-System.currentTimeMillis();if(remain>0){tell(p,"not-ready",Map.of("time",time(remain)));sound(p,"error");return;}
        if(!deliver(p,cfg)){tell(p,"error");return;}
        state.set(statePath(p)+".claimed",day);state.set(statePath(p)+".last",System.currentTimeMillis());saveState();
        tell(p,"progressive-claimed",Map.of("day",String.valueOf(day)));sound(p,"claim");openProgress(p,(day-1)/rewardSlots().length);
    }
    private boolean deliver(Player p,ConfigurationSection cfg){
        List<Map<?,?>> items=cfg.getMapList("items");List<ItemStack> stacks=new ArrayList<>();
        for(Map<?,?> entry:items){Material m=mat(String.valueOf(entry.containsKey("material") ? entry.get("material") : "AIR"),Material.AIR);if(m.isAir()){getLogger().warning("Material invalido en premios: "+entry);return false;}
            int amount=1;try{amount=Integer.parseInt(String.valueOf(entry.containsKey("amount") ? entry.get("amount") : 1));}catch(NumberFormatException ignored){}if(amount<1||amount>2304)return false;
            while(amount>0){int n=Math.min(amount,m.getMaxStackSize());stacks.add(new ItemStack(m,n));amount-=n;}
        }
        // Cannot claim if there is not enough space; no items lost.
        ItemStack[] test=p.getInventory().getStorageContents().clone();
        for(ItemStack stack:stacks){int remaining=stack.getAmount();for(int i=0;i<test.length&&remaining>0;i++){ItemStack existing=test[i];if(existing!=null&&existing.isSimilar(stack)){int free=existing.getMaxStackSize()-existing.getAmount(),put=Math.min(remaining,free);if(put>0){existing=existing.clone();existing.setAmount(existing.getAmount()+put);test[i]=existing;remaining-=put;}}}for(int i=0;i<test.length&&remaining>0;i++)if(test[i]==null||test[i].getType().isAir()){int put=Math.min(remaining,stack.getMaxStackSize());test[i]=new ItemStack(stack.getType(),put);remaining-=put;}if(remaining>0){p.sendMessage(color(getConfig().getString("messages.prefix","")+"&cNecesitas más espacio en el inventario."));return false;}}
        for(ItemStack stack:stacks)p.getInventory().addItem(stack);
        for(String command:cfg.getStringList("commands")){
            String finalCmd=command.replace("%player%",p.getName()).replace("{player}",p.getName());
            if(finalCmd.startsWith("/")) finalCmd=finalCmd.substring(1);
            try {Bukkit.dispatchCommand(Bukkit.getConsoleSender(),finalCmd);}catch(Exception ex){getLogger().severe("Comando de recompensa fallido: "+finalCmd+" -> "+ex.getMessage());}
        }
        return true;
    }
}
