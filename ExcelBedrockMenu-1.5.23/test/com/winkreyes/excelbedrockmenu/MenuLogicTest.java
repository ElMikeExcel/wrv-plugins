package com.winkreyes.excelbedrockmenu;
public class MenuLogicTest {
    static int fails=0;
    static void check(boolean c,String m){ if(!c){fails++;System.out.println("FAIL "+m);} else System.out.println("ok   "+m);}
    static String[] parseStringArray(String src,String name){
        int s=src.indexOf(name+" = {"); if(s<0) return new String[0];
        int e=src.indexOf("};",s); if(e<0) return new String[0];
        java.util.List<String> out=new java.util.ArrayList<>();
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("\"([^\"]*)\"").matcher(src.substring(s,e));
        while(m.find()) out.add(m.group(1));
        return out.toArray(new String[0]);
    }
    static int idxOf(String[] arr,String v){ for(int i=0;i<arr.length;i++) if(arr[i].equals(v)) return i; return -1; }
    public static void main(String[] a){
        try{
            String src=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            String yml=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("commands_menu.yml")));
            check(src.contains("\"Party\", \"Utility Pouch\", \"Spellbook\", \"Excel Shop\", \"My Cosmetics\","),"button array runs Party, then Utility Pouch (1.5.14), Spellbook (1.5.18), before Excel Shop (My Cosmetics follows it in 1.5.22)");
            check(src.contains("\"spawn\", \"tpmenu\", \"excelparty\", \"pouch\", \"spellinfo\", \"shop\""),"command array runs excelparty at the same index as Party, pouch right after it");
            check(!src.contains("performCommand(\"party\")")&&!src.contains("\"spawn\", \"tpmenu\", \"party\""),"never runs bare /party (mcMMO)");
            check(!src.contains("\"Spawn Shop\"")||src.indexOf("\"Spawn Shop\"")<src.indexOf("class ExcelBedrockMenu"),"no Spawn Shop label in code");
            check(src.contains("{\"/shop\", \"shop\", \"Open the Excel Shop\"}"),"/shop kept, Excel Shop description");
            check(yml.contains("Open the Excel Shop")&&!yml.contains("Open the Spawn Shop"),"commands_menu.yml renamed");
        }catch(Exception e){check(false,"file read "+e);}
        String l=ExcelBedrockMenu.lockedLabel("Adventurers Guild");
        check(l.equals("Adventurers Guild"),"locked label text");
        boolean ascii=l.chars().allMatch(c->c<128);
        check(ascii,"locked label is plain ASCII");
        check(ExcelBedrockMenu.isSpawnWorld(true,"world"),"overworld accepted");
        check(!ExcelBedrockMenu.isSpawnWorld(true,"em_adventurers_guild"),"guild world rejected");
        check(!ExcelBedrockMenu.isSpawnWorld(false,"world_nether"),"nether rejected");
        check(!ExcelBedrockMenu.isSpawnWorld(false,"world_the_end"),"end rejected");
        check(!ExcelBedrockMenu.needsSpawnFallback(true,"world"),"no fallback when console spawn worked");
        check(ExcelBedrockMenu.needsSpawnFallback(true,"em_adventurers_guild"),"fallback when still in guild");
        check(ExcelBedrockMenu.needsSpawnFallback(false,"world"),"fallback when command unavailable");
        // v1.5.8: teleport confirm routing (Mike's standing menu standard)
        check(ExcelBedrockMenu.requiresConfirm("spawn",false),"spawn tap opens confirm (no instant teleport)");
        check(ExcelBedrockMenu.requiresConfirm("spawn",true),"spawn confirm does not depend on guild state");
        check(ExcelBedrockMenu.requiresConfirm("ag",false),"guild button confirms while unlocked (it teleports)");
        check(!ExcelBedrockMenu.requiresConfirm("ag",true),"no guild confirm while sealed (/ag opens the quest page, not a teleport)");
        check(ExcelBedrockMenu.requiresConfirm("home",false),"catalogue /home confirms");
        String[] menuOthers={"spellinfo","tpmenu","excelparty","shop","jobs browse","mcstats","skillsperks","leaderboard","motd","servercommands","serverwebsite","pouch","menujoin"};
        boolean menuOk=true;
        for(String c:menuOthers){ if(ExcelBedrockMenu.requiresConfirm(c,false)||ExcelBedrockMenu.requiresConfirm(c,true)) menuOk=false; }
        check(menuOk,"non-teleport menu buttons never confirm");
        String[] catalogOthers={"menu","tpmenu","tpaccept","trapped","warps","balance","balancetop","jobs stats","mcstats","mctop","party","leaderboard","excelshop setup","excelshop manage","deathprotection status","mobveil status","claim","claimslist","seals","em","motd","list","playermap","compass","pouch","menujoin","servercommands","serverwebsite"};
        boolean catOk=true;
        for(String c:catalogOthers){ if(ExcelBedrockMenu.requiresConfirm(c,false)) catOk=false; }
        check(catOk,"catalogue non-teleports never confirm (tpaccept + trapped deliberately excluded)");
        check(ExcelBedrockMenu.teleportConfirmTitle("spawn",false).equals("Teleport to spawn?"),"spawn confirm title");
        check(ExcelBedrockMenu.teleportConfirmTitle("ag",false).equals("Teleport to the Adventurers Guild?"),"guild confirm title");
        check(ExcelBedrockMenu.teleportConfirmTitle("home",false).equals("Teleport home?"),"home confirm title");
        check(ExcelBedrockMenu.teleportConfirmTitle("ag",true)==null,"no title while sealed");
        check(ExcelBedrockMenu.teleportConfirmTitle("shop",false)==null,"no title for non-teleports");
        check(ExcelBedrockMenu.confirmChoiceTeleports(Boolean.TRUE),"confirm Yes teleports");
        check(!ExcelBedrockMenu.confirmChoiceTeleports(Boolean.FALSE),"confirm Cancel does nothing");
        check(!ExcelBedrockMenu.confirmChoiceTeleports(null),"form closed without choosing does nothing");
        String allConfirmText=ExcelBedrockMenu.teleportConfirmTitle("spawn",false)
                +ExcelBedrockMenu.teleportConfirmTitle("ag",false)+ExcelBedrockMenu.teleportConfirmTitle("home",false)
                +ExcelBedrockMenu.teleportConfirmContent("spawn")+ExcelBedrockMenu.teleportConfirmContent("ag")
                +ExcelBedrockMenu.teleportConfirmContent("home")+ExcelBedrockMenu.CONFIRM_YES+ExcelBedrockMenu.CONFIRM_NO;
        check(allConfirmText.chars().allMatch(c->c<128),"all confirm texts are plain ASCII");
        check(ExcelBedrockMenu.CONFIRM_YES.equals("Yes, teleport"),"confirm Yes label text");
        check(ExcelBedrockMenu.CONFIRM_NO.equals("Cancel"),"confirm Cancel label text");
        try{
            String src2=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            check(!src2.contains("{\"/sethome\"")&&!src2.contains("{\"/home\", \"home\"")&&!src2.contains("{\"/sell hand\""),"catalogue rows removed: sethome, home, sell hand");
            check(src2.contains("{\"/menujoin\", \"menujoin\""),"menujoin catalogue row kept (toggle untouched)");
            check(src2.contains("GeyserForms.sendModalForm"),"teleport confirm uses the existing Cumulus ModalForm helper");
            check(src2.contains("confirmTeleport(player, command"),"main-menu routing calls the confirm step");
            check(src2.contains("runCatalogEntry(player, run)"),"catalogue routing calls the confirm gate");
            check(src2.contains("if (requiresConfirm(command, isGuildLocked()))"),"action routing is gated by the confirm decision");
            String yml2=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("plugin.yml")));
            check(yml2.contains("version: 1.5.23"),"plugin.yml is 1.5.23");
            check(src2.contains("websiteFormContent()")&&src2.contains("websiteChatLines()"),"showWebsite uses the pinned form/chat helpers");
            check(src2.contains("WEBSITE_SHORT_URL"),"Java website answer carries the short link too");
        }catch(Exception e){check(false,"1.5.10 source checks "+e);}
        // v1.5.10: short link alongside the long one (Mike, Oct 18, 2026)
        check(ExcelBedrockMenu.WEBSITE_SHORT_URL.equals("https://tinyurl.com/winkreyesvalley"),"short URL constant");
        check(ExcelBedrockMenu.WEBSITE_SHORT_DISPLAY.equals("tinyurl.com/winkreyesvalley"),"short display constant");
        String form=ExcelBedrockMenu.websiteFormContent();
        check(form.contains("tinyurl.com/winkreyesvalley"),"website form shows the short link");
        check(form.contains("elmikeexcel.github.io/winkreyesvalley"),"website form still shows the full address");
        check(form.indexOf("tinyurl.com/winkreyesvalley")<form.indexOf("elmikeexcel.github.io/winkreyesvalley"),"short link comes before the full address on the form");
        check(form.contains("Short and easy to type"),"form labels the short link as the easy one to type");
        check(form.contains("both addresses are in your chat too"),"form keeps the chat-copy note, now for both addresses");
        check(form.chars().allMatch(c->c<128),"website form text is plain ASCII");
        String[] chatLines=ExcelBedrockMenu.websiteChatLines();
        check(chatLines.length==2,"chat copy is two lines (short + full)");
        check(chatLines[0].contains("https://tinyurl.com/winkreyesvalley"),"chat copy line 1 carries the short link");
        check(chatLines[1].contains("https://elmikeexcel.github.io/winkreyesvalley/"),"chat copy line 2 carries the full address");
        // v1.5.11: Utility Pouch button (Bedrock parity with the Java menu's Pouch button)
        // (Set expectations updated in place twice: 1.5.12 gained Skills
        // & Perks at index 7; 1.5.14 moved Utility Pouch from index 12
        // to index 3, so the full-set pins below carry the 1.5.14 order.
        // The dedicated 1.5.14 block at the end pins the move itself.)
        try{
            String src3=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            String[] buttons=parseStringArray(src3,"BUTTONS");
            String[] commands=parseStringArray(src3,"COMMANDS");
            check(buttons.length==22,"menu now has 22 buttons (1.5.21's 20 + My Cosmetics, Ask the Guide, Maker's mark in 1.5.22, minus mcMMO Stats which moved into Skills & Perks)");
            check(commands.length==22,"command array has 22 entries, matching the buttons");
            String[] wantButtons={"Spawn","Teleport to a player","Party","Utility Pouch","Spellbook","Excel Shop","My Cosmetics","Adventurers Guild","Jobs & Money","Skills & Perks","Leaderboard","Lost & Found","Trash Bin","Server Info","Server Commands","Server Website","What's New","Ask the Guide","Wand controls: Classic or Sneak bars","Auto-sort my chests: on/off","Maker's mark and Gear stats","Auto-open on join"};
            check(java.util.Arrays.equals(buttons,wantButtons),"full button set is the 1.5.11 set + Skills & Perks, in the 1.5.14 order (Pouch moved up), Spellbook after the Pouch (1.5.18), Auto-sort before Auto-open (1.5.18)");
            String[] wantCommands={"spawn","tpmenu","excelparty","pouch","spellinfo","shop","cosmetics","ag","jobs browse","skillsperks","leaderboard","lostfound","trash","motd","servercommands","serverwebsite","whatsnew","ask","wandstyle","autosort","mark","menujoin"};
            check(java.util.Arrays.equals(commands,wantCommands),"full command set is the 1.5.11 set + skillsperks, in the 1.5.14 order (pouch moved up), spellinfo after pouch (1.5.18)");
            check(idxOf(buttons,"Utility Pouch")==3,"Utility Pouch sits at index 3 (moved up in 1.5.14; was 12 through 1.5.13)");
            check(idxOf(commands,"pouch")==idxOf(buttons,"Utility Pouch"),"pouch command at the same index as the Utility Pouch button");
            check(buttons[2].equals("Party")&&buttons[4].equals("Spellbook")&&buttons[5].equals("Excel Shop"),"Utility Pouch is after Party; Spellbook (1.5.18) sits between it and Excel Shop");
            check(commands[2].equals("excelparty")&&commands[4].equals("spellinfo")&&commands[5].equals("shop"),"pouch command is after excelparty; spellinfo (1.5.18) between it and shop");
            check("Utility Pouch".chars().allMatch(c->c<128),"Utility Pouch label is plain ASCII");
            check(!ExcelBedrockMenu.requiresConfirm("pouch",false)&&!ExcelBedrockMenu.requiresConfirm("pouch",true),"pouch never opens the teleport confirm (it is not a teleport)");
            check(ExcelBedrockMenu.teleportConfirmTitle("pouch",false)==null,"no confirm title for pouch");
            check(!src3.contains("case \"pouch\""),"pouch has no special case: it runs through the default performCommand path");
        }catch(Exception e){check(false,"1.5.11 pouch checks "+e);}
        // v1.5.12: Skills & Perks button + content form (MENU-SPEC section 2)
        try{
            String src4=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            String[] buttons4=parseStringArray(src4,"BUTTONS");
            String[] commands4=parseStringArray(src4,"COMMANDS");
            check(idxOf(buttons4,"Skills & Perks")==9,"Skills & Perks sits at index 9 (7 through 1.5.13; 8 after the 1.5.14 Pouch move; 9 after the 1.5.18 Spellbook)");
            check(buttons4[8].equals("Jobs & Money")&&buttons4[10].equals("Leaderboard"),"Skills & Perks stays at index 9, now right after Jobs & Money (1.5.22: mcMMO Stats moved into the Skills & Perks form, My Cosmetics added above), before Leaderboard");
            check(idxOf(commands4,"skillsperks")==idxOf(buttons4,"Skills & Perks"),"skillsperks token at the same index as the Skills & Perks button");
            check("Skills & Perks".chars().allMatch(c->c<128),"Skills & Perks label is plain ASCII");
            check(src4.contains("case \"skillsperks\""),"skillsperks is special-cased in runAction (it opens a form, not a command)");
            check(src4.contains("openSkillsPerks(player)"),"the skillsperks case opens the Skills & Perks form");
            check(!src4.contains("performCommand(\"skillsperks\")")&&!src4.contains("performCommand(command)")||src4.contains("case \"skillsperks\""),"skillsperks never reaches the default performCommand path");
            // No-teleport rule: the button is NOT in the confirm-standard coverage
            check(!ExcelBedrockMenu.requiresConfirm("skillsperks",false)&&!ExcelBedrockMenu.requiresConfirm("skillsperks",true),"Skills & Perks never opens the teleport confirm (it teleports nowhere)");
            check(ExcelBedrockMenu.teleportConfirmTitle("skillsperks",false)==null,"no confirm title for Skills & Perks");
            // Form copy: the MENU-SPEC section 2 body, verbatim, at 0 seals
            String want0="mcMMO skills are live now: 19 skills and 83 perks, and levels run from 1 to 1,000. The game announces each new perk in chat the moment you unlock it.\n\nCheck yours any time:\n/mcstats shows all your levels.\nTyping a skill's name (like /smelting) shows that skill's own page.\n/mctop shows the leaderboards.\n\nMagic is sealed for now. It arrives as the Adventurers Guild seals fall (0 of 5 today).";
            check(ExcelBedrockMenu.skillsPerksContent(0).equals(want0),"Skills & Perks body matches the spec copy exactly at 0 seals");
            // ... and with a nonzero live count (the count is data, never hard-coded)
            String c3=ExcelBedrockMenu.skillsPerksContent(3);
            check(c3.contains("3 of 5 seals broken"),"seal count renders from GuildGate state (nonzero case, 1.5.22 awake wording)");
            check(!c3.contains("0 of 5"),"nonzero render does not keep the 0 count");
            check(ExcelBedrockMenu.skillsPerksContent(5).contains("5 of 5 seals broken"),"seal count renders at all 5 fallen");
            check(src4.contains("sealsBrokenCount"),"the count is read live from GuildGate (sealsBrokenCount)");
            // The form's buttons: Skill Trees follow-on first, Close second
            check(java.util.Arrays.equals(ExcelBedrockMenu.SKILLS_PERKS_BUTTONS,new String[]{"My mcMMO stats","Skill Trees on the Website","Close"}),"Skills & Perks form buttons are My mcMMO stats (1.5.22) + Skill Trees on the Website + Close");
            // Follow-on form: the address as plain text + the spec's line
            String web=ExcelBedrockMenu.skillsPerksWebsiteContent();
            check(web.contains("elmikeexcel.github.io/winkreyesvalley"),"Skill Trees follow-on shows the website address as plain text");
            check(web.contains("Open it and tap Skill Trees"),"follow-on carries the spec's tap-Skill-Trees line");
            check(web.contains("every ladder is there."),"follow-on line ends as specced");
            check(!web.contains("http"),"follow-on shows no raw URL (plain display address only, Bedrock cannot open links)");
        }catch(Exception e){check(false,"1.5.12 skills checks "+e);}
        // v1.5.13: /pouch joins the /servercommands catalogue (Server Info,
        // immediately after /compass). The main-menu Utility Pouch button
        // has existed since 1.5.11; the catalogue row is what was missing.
        try{
            String src5=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            check(src5.contains("{\"/pouch\", \"pouch\", \"Park your menu compass and Magic wand; contents survive death\"}"),"catalogue carries the exact /pouch row");
            int hits=0, hitGroup=-1, hitIdx=-1;
            for(int g=0;g<ExcelBedrockMenu.catalogGroupCount();g++){
                for(int i=0;i<ExcelBedrockMenu.catalogEntryCount(g);i++){
                    if(ExcelBedrockMenu.catalogDisplay(g,i).equals("/pouch")){hits++;hitGroup=g;hitIdx=i;}
                }
            }
            check(hits==1,"/pouch appears exactly once in the catalogue");
            check(hitGroup>=0&&ExcelBedrockMenu.catalogGroupName(hitGroup).equals("Server Info"),"/pouch sits in the Server Info group");
            check(hitGroup>=0&&"pouch".equals(ExcelBedrockMenu.catalogRun(hitGroup,hitIdx)),"/pouch entry is runnable (runs pouch as the player)");
            check(hitGroup>=0&&ExcelBedrockMenu.catalogDescription(hitGroup,hitIdx).equals("Park your menu compass and Magic wand; contents survive death"),"/pouch description text");
            check(hitGroup>=0&&hitIdx>0&&ExcelBedrockMenu.catalogDisplay(hitGroup,hitIdx-1).equals("/compass"),"/pouch sits immediately after /compass");
            check(hitGroup>=0&&ExcelBedrockMenu.catalogEntryCount(hitGroup)==10,"Server Info group now has 10 entries (9 + /whatsnew in 1.5.19)");
            check(ExcelBedrockMenu.catalogTotalEntries()==50,"catalogue totals 50 entries (41 + pouch in 1.5.13 + two childproof rows in 1.5.16 + /whatsnew in 1.5.19 + /cosmetics and the 4-row Your Items & Help group in 1.5.23)");
            String yml5=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("commands_menu.yml")));
            check(yml5.contains("si_pouch")&&yml5.contains("'[player] pouch'"),"commands_menu.yml mirror carries si_pouch running [player] pouch");
            String sm5=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("server_menu.yml")));
            check(sm5.contains("utility_pouch")&&sm5.contains("'&6Utility Pouch'"),"bundled server_menu.yml mirror carries the Utility Pouch entry");
        }catch(Exception e){check(false,"1.5.13 pouch catalogue checks "+e);}
        // v1.5.14: Utility Pouch moves up the main menu (Mike, Oct 21,
        // 2026: it is something people use quite often to store their
        // stuff away). Index 12 -> 3; the move itself is pinned here:
        // lengths unchanged, pouch pinned at 3 in both arrays, and
        // removing the pouch entry leaves the exact 1.5.13 order of
        // the other 13 entries (no other entry moved relative to its
        // neighbors).
        try{
            String src6=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            String[] buttons6=parseStringArray(src6,"BUTTONS");
            String[] commands6=parseStringArray(src6,"COMMANDS");
            check(buttons6.length==22&&commands6.length==22,"1.5.14 move kept 14 buttons / 14 commands; 1.5.18 adds Spellbook, Wand controls and Auto-sort (17); What's New (18); 1.5.20 Lost & Found and Trash Bin (20); 1.5.22 +3 -1 (22)");
            check(idxOf(buttons6,"Utility Pouch")==3,"Utility Pouch now sits at index 3 (4th, top daily-driver cluster)");
            check(idxOf(commands6,"pouch")==3,"pouch command at index 3, matching the button");
            check(buttons6[0].equals("Spawn")&&buttons6[1].equals("Teleport to a player")&&buttons6[2].equals("Party"),"top of the menu above the Pouch is unchanged: Spawn / Teleport to a player / Party");
            // 1.5.22: mcMMO Stats left the main menu, so the 1.5.13 order is pinned without it (12 entries).
            String[] wantRestB={"Spawn","Teleport to a player","Party","Excel Shop","Adventurers Guild","Jobs & Money","Skills & Perks","Leaderboard","Server Info","Server Commands","Server Website","Auto-open on join"};
            String[] wantRestC={"spawn","tpmenu","excelparty","shop","ag","jobs browse","skillsperks","leaderboard","motd","servercommands","serverwebsite","menujoin"};
            java.util.Set<String> new1522B=new java.util.HashSet<>(java.util.Arrays.asList("My Cosmetics","Ask the Guide","Maker's mark and Gear stats"));
            java.util.Set<String> new1522C=new java.util.HashSet<>(java.util.Arrays.asList("cosmetics","ask","mark"));
            java.util.List<String> restB=new java.util.ArrayList<>(), restC=new java.util.ArrayList<>();
            for(String s:buttons6) if(!new1522B.contains(s)&&!s.equals("Utility Pouch")&&!s.equals("Spellbook")&&!s.startsWith("Auto-sort")&&!s.startsWith("Wand controls")&&!s.equals("What's New")&&!s.equals("Lost & Found")&&!s.equals("Trash Bin")) restB.add(s);
            for(String s:commands6) if(!new1522C.contains(s)&&!s.equals("pouch")&&!s.equals("spellinfo")&&!s.equals("autosort")&&!s.equals("wandstyle")&&!s.equals("whatsnew")&&!s.equals("lostfound")&&!s.equals("trash")) restC.add(s);
            check(restB.size()==12&&java.util.Arrays.equals(restB.toArray(new String[0]),wantRestB),"removing the Pouch (and later additions) leaves the 1.5.13 button order untouched (minus mcMMO Stats, 1.5.22)");
            check(restC.size()==12&&java.util.Arrays.equals(restC.toArray(new String[0]),wantRestC),"removing pouch (and later additions) leaves the 1.5.13 command order untouched (minus mcstats, 1.5.22)");
        }catch(Exception e){check(false,"1.5.14 pouch move checks "+e);}
        // v1.5.15: Bedrock form taps forwarded to ExcelMenuStats (reflection, fail-silent)
        try{
            String gf=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/GeyserForms.java")));
            String cs=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ClickStats.java")));
            String py=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("plugin.yml")));
            check(gf.contains("if (id >= 0 && id < buttons.length) ClickStats.tap(player, title, buttons[id], id);\n                            onSelect.accept(id);"),"SimpleForm tap logged before routing, closed form (-1) not logged");
            check(gf.contains("ClickStats.tap(player, title, first ? button1 : button2, first ? 0 : 1);\n                            onResult.accept(first);"),"ModalForm tap logged before routing");
            check(cs.contains("getMethod(\"recordTap\", Player.class, String.class, String.class, int.class, String.class)"),"hook calls ExcelMenuStats.recordTap by reflection");
            check(cs.contains("catch (Throwable t)"),"hook never throws into the form handler");
            check(!py.contains("ExcelMenuStats"),"no plugin.yml dependency on ExcelMenuStats");
            check(gf.split("ClickStats.tap",-1).length-1==3,"exactly three hook call sites (1.5.22 adds the Ask the Guide Send tap)");
        }catch(Exception e){check(false,"1.5.15 hook pins: "+e);}
        // v1.5.16: childproof picker (front end for the commands.yml /childproof alias)
        {
            int g=ExcelBedrockMenu.catalogGroupIndexFor("claims");
            check(g>=0&&ExcelBedrockMenu.catalogGroupName(g).equals("Claims & Land"),"claims group found");
            int at=-1,un=-1,tr=-1;
            for(int i=0;i<ExcelBedrockMenu.catalogEntryCount(g);i++){
                String d=ExcelBedrockMenu.catalogDisplay(g,i);
                if(d.equals("/childproofmenu"))at=i; if(d.equals("/unchildproof <player>"))un=i; if(d.equals("/trust <player>"))tr=i;
            }
            check(at==tr+1&&un==at+1,"childproof rows sit right after /trust");
            check("childproofmenu".equals(ExcelBedrockMenu.catalogRun(g,at)),"/childproofmenu row is a tap button running childproofmenu");
            check(ExcelBedrockMenu.catalogRun(g,un)==null,"/unchildproof <player> is display-only (needs a name)");
            check(java.util.Arrays.asList(ExcelBedrockMenu.catalogGroupFormButtons(g)).contains("/childproofmenu"),"Bedrock Claims form has the /childproofmenu button");
            check(!ExcelBedrockMenu.requiresConfirm("childproofmenu",false),"childproofmenu is not a teleport (its own confirm form)");
            check(ChildproofMenu.childproofCommand(".KidPlayer").equals("childproof .KidPlayer"),"picker runs the alias with the exact name (Bedrock dot kept)");
            check(ChildproofMenu.label("~Alex",".KidPlayer").equals("Alex (.KidPlayer)"),"button shows nickname + real name");
            check(ChildproofMenu.label("\u00a7aOwnerPlayer","OwnerPlayer").equals("OwnerPlayer"),"no duplicate when nick = name");
            check(ChildproofMenu.label(null,"Bob").equals("Bob"),"null display name falls back to name");
            check(ChildproofMenu.confirmContent("X").contains("doors, buttons, levers and beds")&&ChildproofMenu.confirmContent("X").contains("/unchildproof X"),"confirm text says what it does and how to undo");
            check(ChildproofMenu.YES.equals("Yes, childproof")&&ChildproofMenu.NO.equals("Cancel"),"confirm buttons");
            String help=String.join("\n",ChildproofMenu.helpLines());
            check(help.contains("/childproof Alex")&&help.contains("/unchildproof Alex")&&help.contains("type the player's name"),"help has concrete examples + type-the-name note");
            String ok=ChildproofMenu.resultLine(true,".KidPlayer","access");
            check(ok.contains("Done!")&&ok.contains("/unchildproof .KidPlayer")&&ok.contains("can't build"),"childproof success line uses the real name in the undo example");
            check(ChildproofMenu.resultLine(false,"Bob","none").contains("/childproof Bob"),"unchildproof success line has the redo example");
            check(ChildproofMenu.resultLine(true,"Bob","outside").contains("ALL your claims"),"outside-claim warning");
            check(ChildproofMenu.resultLine(true,"Bob","other:build").contains("/trustlist"),"leftover trust is flagged, not called done");
            check(!ChildproofMenu.resultLine(true,"Bob","unknown").contains("Done"),"unknown state never claims success");
            check(!ChildproofMenu.resultLine(true,"Bob","none").contains("Done"),"childproof that ended with no access is not called done");
            try{
                String py=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("plugin.yml")));
                check(py.contains("version: 1.5.23")&&py.contains("  childproofmenu:"),"plugin.yml 1.5.18 registers /childproofmenu");
            }catch(Exception e){check(false,"plugin.yml read "+e);}
        }
        // v1.5.18: Spellbook button (Mike, Oct 24, 2026). Runs ExcelWandBars' /spellinfo.
        try{
            String s8=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            String[] b8=parseStringArray(s8,"BUTTONS"); String[] c8=parseStringArray(s8,"COMMANDS");
            check(idxOf(b8,"Spellbook")==4&&idxOf(c8,"spellinfo")==4,"Spellbook button and spellinfo command both at index 4");
            check(b8[3].equals("Utility Pouch")&&b8[5].equals("Excel Shop"),"Spellbook sits right after Utility Pouch, before Excel Shop");
            java.util.List<String> rb=new java.util.ArrayList<>(java.util.Arrays.asList(b8)); rb.remove("Spellbook"); rb.remove("Auto-sort my chests: on/off"); rb.remove("Wand controls: Classic or Sneak bars"); rb.remove("What's New"); rb.remove("Lost & Found"); rb.remove("Trash Bin"); rb.removeAll(java.util.Arrays.asList("My Cosmetics","Ask the Guide","Maker's mark and Gear stats"));
            check(java.util.Arrays.equals(rb.toArray(new String[0]),new String[]{"Spawn","Teleport to a player","Party","Utility Pouch","Excel Shop","Adventurers Guild","Jobs & Money","Skills & Perks","Leaderboard","Server Info","Server Commands","Server Website","Auto-open on join"}),"removing Spellbook and Auto-sort (and later additions) leaves the exact 1.5.17 button order (minus mcMMO Stats, 1.5.22)");
            java.util.List<String> rc=new java.util.ArrayList<>(java.util.Arrays.asList(c8)); rc.remove("spellinfo"); rc.remove("autosort"); rc.remove("wandstyle"); rc.remove("whatsnew"); rc.remove("lostfound"); rc.remove("trash"); rc.removeAll(java.util.Arrays.asList("cosmetics","ask","mark"));
            check(java.util.Arrays.equals(rc.toArray(new String[0]),new String[]{"spawn","tpmenu","excelparty","pouch","shop","ag","jobs browse","skillsperks","leaderboard","motd","servercommands","serverwebsite","menujoin"}),"removing spellinfo and autosort (and later additions) leaves the exact 1.5.17 command order (minus mcstats, 1.5.22)");
            check(!ExcelBedrockMenu.requiresConfirm("spellinfo",false)&&!ExcelBedrockMenu.requiresConfirm("spellinfo",true),"Spellbook never opens the teleport confirm");
            check(s8.contains("case \"spellinfo\":")&&s8.contains("Bukkit.getPluginCommand(\"spellinfo\") == null"),"spellinfo is guarded: missing ExcelWandBars gives a message, not an unknown command");
            check("Spellbook".chars().allMatch(c->c<128),"Spellbook label is plain ASCII");
            int as=idxOf(b8,"Auto-sort my chests: on/off");
            check(as>=0&&as==idxOf(c8,"autosort"),"Auto-sort button and autosort command line up");
            check(as==b8.length-3&&b8[b8.length-2].equals("Maker's mark and Gear stats")&&b8[b8.length-1].equals("Auto-open on join"),"Auto-sort, then Maker's mark (1.5.22), then Auto-open on join (the switches together, Java Settings order)");
            check(s8.contains("case \"autosort\":")&&s8.contains("Bukkit.getPluginCommand(\"autosort\") == null"),"autosort guarded: missing AutoSort gives a message");
            check(!ExcelBedrockMenu.requiresConfirm("autosort",false)&&!ExcelBedrockMenu.requiresConfirm("autosort",true),"Auto-sort never opens the teleport confirm");
            check(!java.util.Arrays.asList(c8).contains("sort"),"menu uses /autosort, never the bare /sort alias");
            int wc=idxOf(b8,"Wand controls: Classic or Sneak bars");
            check(wc>=0&&wc==idxOf(c8,"wandstyle")&&wc==as-1,"Wand controls (1.5.18) lines up with wandstyle, just before Auto-sort");
            check(s8.contains("case \"wandstyle\":")&&s8.contains("Bukkit.getPluginCommand(\"wandstyle\") == null"),"wandstyle guarded: missing ExcelWandBars gives a message");
            check(!ExcelBedrockMenu.requiresConfirm("wandstyle",false)&&!ExcelBedrockMenu.requiresConfirm("wandstyle",true),"Wand controls never opens the teleport confirm");
        }catch(Exception e){check(false,"1.5.18 spellbook checks "+e);}
        // v1.5.19: What's New button (ExcelWhatsNew 0.1.0)
        try{
            String s9=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            String[] b9=parseStringArray(s9,"BUTTONS"); String[] c9=parseStringArray(s9,"COMMANDS");
            int w=idxOf(b9,"What's New");
            check(w>=0&&w==idxOf(c9,"whatsnew"),"What's New button lines up with the whatsnew command");
            check(w>0&&b9[w-1].equals("Server Website"),"What's New sits right after Server Website");
            check(b9[w+1].equals("Ask the Guide")&&b9[w+2].startsWith("Wand controls"),"Ask the Guide (1.5.22) follows What's New, as on the Java Help row; Wand controls after it");
            check(s9.contains("case \"whatsnew\":")&&s9.contains("Bukkit.getPluginCommand(\"whatsnew\") == null"),"whatsnew guarded: missing ExcelWhatsNew gives a message");
            check(!ExcelBedrockMenu.requiresConfirm("whatsnew",false)&&!ExcelBedrockMenu.requiresConfirm("whatsnew",true),"What's New never opens the teleport confirm");
            check(!java.util.Arrays.asList(c9).contains("news"),"menu runs /whatsnew, not the /news alias");
            int g=ExcelBedrockMenu.catalogGroupIndexFor("server info"); int found=-1;
            for(int i=0;i<ExcelBedrockMenu.catalogEntryCount(g);i++) if("whatsnew".equals(ExcelBedrockMenu.catalogRun(g,i))) found=i;
            check(found==ExcelBedrockMenu.catalogEntryCount(g)-1,"/whatsnew is the last runnable row of Server Info");
            check(b9.length==22&&c9.length==22,"22 buttons, 22 commands (1.5.20 adds two, 1.5.22 +3 -1)");
        }catch(Exception e){check(false,"1.5.19 whatsnew checks "+e);}
        // v1.5.20: Lost & Found and Trash Bin buttons (Mike, Oct 25, 2026: match the Java compass)
        try{
            String s10=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            String[] b10=parseStringArray(s10,"BUTTONS"); String[] c10=parseStringArray(s10,"COMMANDS");
            int lf=idxOf(b10,"Lost & Found"), tb=idxOf(b10,"Trash Bin");
            check(lf==11&&tb==12,"Lost & Found at index 11, Trash Bin at index 12");
            check(lf==idxOf(c10,"lostfound")&&tb==idxOf(c10,"trash"),"Lost & Found lines up with lostfound, Trash Bin with trash");
            check(tb==lf+1,"the two new buttons sit next to each other, Lost & Found first (Java Me row order: slot 14 then 16)");
            check(b10[lf-1].equals("Leaderboard")&&b10[lf-2].equals("Skills & Perks"),"they close the Me cluster right after Leaderboard");
            check(b10[tb+1].equals("Server Info"),"Server Info follows Trash Bin (help cluster unchanged)");
            check(java.util.Collections.frequency(java.util.Arrays.asList(b10),"Lost & Found")==1&&java.util.Collections.frequency(java.util.Arrays.asList(b10),"Trash Bin")==1,"each new label appears once");
            check(java.util.Collections.frequency(java.util.Arrays.asList(c10),"lostfound")==1&&java.util.Collections.frequency(java.util.Arrays.asList(c10),"trash")==1,"each new command appears once");
            java.util.List<String> rb=new java.util.ArrayList<>(java.util.Arrays.asList(b10)); rb.remove("Lost & Found"); rb.remove("Trash Bin"); rb.removeAll(java.util.Arrays.asList("My Cosmetics","Ask the Guide","Maker's mark and Gear stats"));
            check(java.util.Arrays.equals(rb.toArray(new String[0]),new String[]{"Spawn","Teleport to a player","Party","Utility Pouch","Spellbook","Excel Shop","Adventurers Guild","Jobs & Money","Skills & Perks","Leaderboard","Server Info","Server Commands","Server Website","What's New","Wand controls: Classic or Sneak bars","Auto-sort my chests: on/off","Auto-open on join"}),"removing the two new buttons (and the 1.5.22 ones) leaves the live 1.5.19-whatsnew button order (minus mcMMO Stats, 1.5.22)");
            java.util.List<String> rc=new java.util.ArrayList<>(java.util.Arrays.asList(c10)); rc.remove("lostfound"); rc.remove("trash"); rc.removeAll(java.util.Arrays.asList("cosmetics","ask","mark"));
            check(java.util.Arrays.equals(rc.toArray(new String[0]),new String[]{"spawn","tpmenu","excelparty","pouch","spellinfo","shop","ag","jobs browse","skillsperks","leaderboard","motd","servercommands","serverwebsite","whatsnew","wandstyle","autosort","menujoin"}),"removing the two new commands (and the 1.5.22 ones) leaves the live 1.5.19-whatsnew command order (minus mcstats, 1.5.22)");
            check(!java.util.Arrays.asList(c10).contains("recover")&&!java.util.Arrays.asList(c10).contains("bin"),"menu runs /lostfound and /trash, not the /recover or /bin aliases");
            check(!java.util.Arrays.asList(c10).contains("trash chest")&&!java.util.Arrays.asList(c10).contains("trash undo"),"Trash Bin opens the plain bin (same as Java [player] trash)");
            check(!ExcelBedrockMenu.requiresConfirm("lostfound",false)&&!ExcelBedrockMenu.requiresConfirm("lostfound",true),"Lost & Found never opens the teleport confirm");
            check(!ExcelBedrockMenu.requiresConfirm("trash",false)&&!ExcelBedrockMenu.requiresConfirm("trash",true),"Trash Bin never opens the teleport confirm");
            check(ExcelBedrockMenu.teleportConfirmTitle("lostfound",false)==null&&ExcelBedrockMenu.teleportConfirmTitle("trash",false)==null,"no confirm title for either");
            check(s10.contains("case \"lostfound\":")&&s10.contains("runGuarded(player, \"lostfound\","),"lostfound is guarded (missing plugin or permission gives a message)");
            check(s10.contains("case \"trash\":")&&s10.contains("runGuarded(player, \"trash\","),"trash is guarded (missing plugin or permission gives a message)");
            check(s10.contains("cmd.testPermissionSilent(player)"),"guard uses the command's own permission node (exceldeathrecord.lostfound / exceltrash.use)");
            check(ExcelBedrockMenu.guardOutcome(false,false)==0&&ExcelBedrockMenu.guardOutcome(false,true)==0,"missing command -> missing message, never runs");
            check(ExcelBedrockMenu.guardOutcome(true,false)==1,"no permission -> denied message, never runs");
            check(ExcelBedrockMenu.guardOutcome(true,true)==2,"command present + permission -> runs as the player");
            check(s10.contains("Lost & Found isn't available right now")&&s10.contains("The Trash Bin isn't available right now"),"missing-plugin messages name the feature");
            check("Lost & Found".chars().allMatch(c->c<128)&&"Trash Bin".chars().allMatch(c->c<128),"new labels are plain ASCII");
            int g=ExcelBedrockMenu.catalogGroupIndexFor("server info");
            check(ExcelBedrockMenu.catalogEntryCount(g)==10&&ExcelBedrockMenu.catalogTotalEntries()==50,"Server Info unchanged at 10; catalogue 45 until 1.5.23, 50 after (+/cosmetics, +Your Items & Help)");
        }catch(Exception e){check(false,"1.5.20 lost & found / trash checks "+e);}
        // v1.5.21: group text colors (Mike, Oct 25, 2026: "Yes color the text")
        try{
            String s11=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            String[] b11=parseStringArray(s11,"BUTTONS"); String[] c11=parseStringArray(s11,"COMMANDS");
            check(java.util.Arrays.stream(b11).noneMatch(x->x.contains("\u00a7")),"BUTTONS keep plain labels (no codes in the array)");
            java.util.Map<String,String> want=new java.util.HashMap<>();
            for(String x:new String[]{"spawn","tpmenu","excelparty","ag"}) want.put(x,"\u00a71");
            for(String x:new String[]{"pouch","spellinfo","jobs browse","skillsperks","leaderboard","lostfound","trash"}) want.put(x,"\u00a75");
            want.put("shop","\u00a72"); want.put("cosmetics","\u00a72");
            for(String x:new String[]{"motd","servercommands","serverwebsite","whatsnew","ask"}) want.put(x,"\u00a7n");
            for(String x:new String[]{"wandstyle","autosort","mark","menujoin"}) want.put(x,"\u00a78");
            check(want.size()==c11.length,"every one of the 22 commands has a group");
            boolean all=true; for(String x:c11) if(!want.get(x).equals(ExcelBedrockMenu.groupColor(x))) {all=false;System.out.println("  wrong color for "+x);}
            check(all,"group colors: Travel 1, Me 5, Shops 2, Help n (copper), Settings 8");
            check(ExcelBedrockMenu.groupColor("nosuch").equals(""),"unknown command gets no color");
            String[] m0=ExcelBedrockMenu.menuLabels(false), m1=ExcelBedrockMenu.menuLabels(true);
            check(m0.length==b11.length&&m1.length==b11.length,"menuLabels keeps 22 labels, locked or not");
            boolean same=true; for(int i=0;i<b11.length;i++){ if(!ExcelBedrockMenu.stripCodes(m0[i]).equals(b11[i])) same=false; if(!m0[i].startsWith(ExcelBedrockMenu.groupColor(c11[i]))) same=false; }
            check(same,"each sent label = its group color + the exact plain label");
            int ag=idxOf(c11,"ag");
            check(ExcelBedrockMenu.stripCodes(m1[ag]).equals(ExcelBedrockMenu.lockedLabel("Adventurers Guild"))&&m1[ag].startsWith("\u00a71"),"locked Guild label still plain text underneath, blue");
            check(ExcelBedrockMenu.stripCodes(m0[idxOf(c11,"autosort")]).equals("Auto-sort my chests: on/off")&&ExcelBedrockMenu.stripCodes(m0[idxOf(c11,"menujoin")]).equals("Auto-open on join"),"toggle labels unchanged under the color");
            check(ExcelBedrockMenu.stripCodes("\u00a71A\u00a7nB\u00a7lC\u00a7rD").equals("ABCD"),"stripCodes removes Java and Bedrock-only codes");
            check(ExcelBedrockMenu.stripCodes(null)==null,"stripCodes(null) is null");
            check(s11.contains("String[] labels = menuLabels(isGuildLocked());"),"openMenu sends the colored labels");
            check(s11.contains("String command = COMMANDS[id];"),"routing is still by button index, not by label text");
            String cs=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ClickStats.java")));
            check(cs.contains("ExcelBedrockMenu.stripCodes(button)"),"menu stats log the plain label");
            check(!java.util.Arrays.asList(ExcelBedrockMenu.catalogGroupFormButtons(0)).stream().anyMatch(x->x.contains("\u00a7")),"other forms (Server Commands groups) are not colored");
        }catch(Exception e){check(false,"1.5.21 color checks "+e);}
        // v1.5.22: Java parity (Mike, Oct 26, 2026: "always do onto one as you would do the other")
        try{
            String s12=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            String gf12=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/GeyserForms.java")));
            String sm12=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("server_menu.yml")));
            String[] b=parseStringArray(s12,"BUTTONS"); String[] c=parseStringArray(s12,"COMMANDS");
            int mc=idxOf(b,"My Cosmetics"), at=idxOf(b,"Ask the Guide"), mm=idxOf(b,"Maker's mark and Gear stats");
            check(mc==6&&c[6].equals("cosmetics")&&b[5].equals("Excel Shop"),"My Cosmetics at index 6, right after Excel Shop (Java Shops row order), runs cosmetics");
            check(at==17&&c[17].equals("ask")&&b[16].equals("What's New"),"Ask the Guide at index 17, right after What's New (Java Help row order), runs ask");
            check(mm==20&&c[20].equals("mark")&&b[19].startsWith("Auto-sort")&&b[21].equals("Auto-open on join"),"Maker's mark at index 20, between Auto-sort and Auto-open (Java Settings row order), runs mark");
            check(idxOf(b,"mcMMO Stats")<0&&idxOf(c,"mcstats")<0,"mcMMO Stats is no longer its own main button (Java removed it Oct 25)");
            check(java.util.Arrays.asList(ExcelBedrockMenu.SKILLS_PERKS_BUTTONS).indexOf("My mcMMO stats")==ExcelBedrockMenu.SKILLS_PERKS_MCSTATS,"mcMMO stats stays one tap away inside Skills & Perks");
            check(s12.contains("if (id == SKILLS_PERKS_MCSTATS)")&&s12.contains("player.performCommand(\"mcstats\")"),"the Skills & Perks stats button runs /mcstats, like the Java Skills & Perks items");
            check(ExcelBedrockMenu.SKILLS_PERKS_BUTTONS[ExcelBedrockMenu.SKILLS_PERKS_WEBSITE].equals("Skill Trees on the Website"),"website follow-on index constant matches its label");
            check(ExcelBedrockMenu.groupColor("cosmetics").equals("\u00a72")&&ExcelBedrockMenu.groupColor("shop").equals("\u00a72"),"My Cosmetics is Shops green like Excel Shop");
            check(ExcelBedrockMenu.groupColor("ask").equals("\u00a7n"),"Ask the Guide is Help copper");
            check(ExcelBedrockMenu.groupColor("mark").equals("\u00a78"),"Maker's mark is Settings gray");
            // same commands as Java
            check(sm12.contains("'[player] cosmetics'"),"Java My Cosmetics runs [player] cosmetics: Bedrock runs the same command");
            check(sm12.contains("'[player] mark on'")&&sm12.contains("'[player] mark stats on'"),"Java runs /mark on|off and /mark stats on|off: Bedrock bare /mark opens ExcelMakersMark's two-switch form for the same two settings");
            check(sm12.contains("Type &e/ask &7and your question in chat"),"Java Ask the Guide points at /ask: Bedrock Send runs /ask <question>");
            // guards
            check(s12.contains("runGuarded(player, \"cosmetics\",")&&s12.contains("runGuarded(player, \"mark\","),"cosmetics and mark are guarded (missing plugin or permission gives a message)");
            check(ExcelBedrockMenu.ASK_PERMISSION.equals("excelguide.ask")&&s12.contains("player.hasPermission(ASK_PERMISSION)"),"ask guard also checks excelguide.ask (ExcelGuide checks it inside /ask, not on the command)");
            check(s12.contains("Bukkit.getPluginCommand(\"ask\")")&&s12.contains("The Guide isn't available right now")&&s12.contains("You can't ask the Guide right now"),"ask is guarded before the form opens");
            for(String x:new String[]{"cosmetics","ask","mark"}) check(!ExcelBedrockMenu.requiresConfirm(x,false)&&!ExcelBedrockMenu.requiresConfirm(x,true)&&ExcelBedrockMenu.teleportConfirmTitle(x,false)==null,x+" never opens the teleport confirm");
            // ask sanitizer
            check("ask How do I claim land?".equals(ExcelBedrockMenu.askCommandFor("How do I claim land?")),"plain question becomes /ask <question>");
            check(ExcelBedrockMenu.askCommandFor("")==null&&ExcelBedrockMenu.askCommandFor("   ")==null&&ExcelBedrockMenu.askCommandFor(null)==null,"empty or blank input runs nothing");
            check("ask spawn".equals(ExcelBedrockMenu.askCommandFor("/spawn")),"a typed command is only asked about, never run as itself");
            check("ask op me".equals(ExcelBedrockMenu.askCommandFor("//op me")),"several leading slashes all dropped");
            check("ask red text".equals(ExcelBedrockMenu.askCommandFor("\u00a7cred \u00a7ltext")),"color codes removed");
            check("ask a b".equals(ExcelBedrockMenu.askCommandFor("a\nb")),"newlines become spaces");
            check(ExcelBedrockMenu.askCommandFor("x".repeat(500)).length()==4+ExcelBedrockMenu.ASK_MAX,"question capped at ASK_MAX characters");
            check(ExcelBedrockMenu.ASK_HINT.contains("/ask")&&ExcelBedrockMenu.ASK_HINT.contains("Guide,"),"form-failure fallback is the Java button's own hint line");
            check((ExcelBedrockMenu.ASK_TITLE+ExcelBedrockMenu.ASK_PROMPT+ExcelBedrockMenu.ASK_PLACEHOLDER).chars().allMatch(ch->ch<128),"Ask form text is plain ASCII");
            check(gf12.contains("static boolean sendInputForm(")&&gf12.contains("org.geysermc.cumulus.form.CustomForm")&&gf12.contains("getMethod(\"asInput\", int.class)"),"Ask form is a Cumulus CustomForm text box read with asInput(0)");
            check(gf12.contains("ClickStats.tap(player, title, \"Send\", 0);"),"menu stats log only the fixed Send label, never the question");
            // Magic text
            String m0=ExcelBedrockMenu.skillsPerksMagicLine(0), m1=ExcelBedrockMenu.skillsPerksMagicLine(1);
            check(m0.contains("sealed")&&m0.contains("(0 of 5 today)"),"0 seals keeps the sealed line");
            check(m1.startsWith("Magic is awake: 1 of 5 seals broken.")&&!m1.contains("sealed for now"),"from Seal 1 the line says Magic is awake (live since Oct 23)");
            check(m1.contains("Old Quartermaster in the spawn village"),"awake line says where the first wand comes from (matches the website)");
            check(m1.contains("Spellbook"),"awake line points at the Spellbook button");
            check(ExcelBedrockMenu.skillsPerksMagicLine(9).contains("5 of 5"),"count capped at 5");
            check(ExcelBedrockMenu.skillsPerksContent(1).chars().allMatch(ch->ch<128),"Skills & Perks body stays plain ASCII");
            String[] ml=ExcelBedrockMenu.menuLabels(false);
            check(ml.length==22&&ml[mc].equals("\u00a72My Cosmetics")&&ml[at].equals("\u00a7nAsk the Guide")&&ml[mm].equals("\u00a78Maker's mark and Gear stats"),"sent labels carry their group colors");
            check(java.util.Arrays.stream(b).allMatch(x->x.chars().allMatch(ch->ch<128)),"all 22 labels plain ASCII");
        }catch(Exception e){check(false,"1.5.22 parity checks "+e);}
        // v1.5.23: Server Commands list matches the Java Server Commands pages
        try{
            String s13=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            check(ExcelBedrockMenu.catalogGroupCount()==8,"8 catalogue groups (Your Items & Help added)");
            String[] old7={"Getting Around","Money & Jobs","Skills & Levels","Shops & Trading","Claims & Land","Guild & Quests","Server Info"};
            boolean same=true; for(int g=0;g<7;g++) if(!ExcelBedrockMenu.catalogGroupName(g).equals(old7[g])) same=false;
            check(same,"the 7 older groups keep their order and numbers (/servercommands 1..7 unchanged)");
            check(ExcelBedrockMenu.catalogGroupName(7).equals("Your Items & Help"),"group 8 is Your Items & Help (same name as the Java page 2 header)");
            check(ExcelBedrockMenu.catalogGroupIndexFor("8")==7&&ExcelBedrockMenu.catalogGroupIndexFor("your")==7,"/servercommands 8 and /servercommands your open it");
            String[] wantD={"/lostfound","/trash","/mark","/ask"}, wantR={"lostfound","trash","mark","ask"};
            check(ExcelBedrockMenu.catalogEntryCount(7)==4,"Your Items & Help has 4 rows");
            boolean rows=true; for(int i=0;i<4;i++) if(!ExcelBedrockMenu.catalogDisplay(7,i).equals(wantD[i])||!wantR[i].equals(ExcelBedrockMenu.catalogRun(7,i))) rows=false;
            check(rows,"rows /lostfound, /trash, /mark, /ask in the Java slot order, all tap buttons");
            check(java.util.Arrays.equals(ExcelBedrockMenu.catalogGroupFormButtons(7),wantD),"Bedrock form buttons for the group");
            int sh=ExcelBedrockMenu.catalogGroupIndexFor("shops");
            check(ExcelBedrockMenu.catalogEntryCount(sh)==6&&ExcelBedrockMenu.catalogDisplay(sh,0).equals("/shop")&&ExcelBedrockMenu.catalogDisplay(sh,1).equals("/cosmetics"),"/cosmetics sits right after /shop in Shops & Trading");
            check("cosmetics".equals(ExcelBedrockMenu.catalogRun(sh,1))&&ExcelBedrockMenu.catalogDescription(sh,1).equals("See your colors, effects and sets, and equip them"),"/cosmetics row runs cosmetics with the Java page 2 words");
            check(ExcelBedrockMenu.catalogRunForButton(sh,1).equals("cosmetics")&&ExcelBedrockMenu.catalogRunForButton(sh,2).equals("excelshop setup"),"button index routing shifts correctly after the new row");
            int hits=0; for(int g=0;g<ExcelBedrockMenu.catalogGroupCount();g++) for(int i=0;i<ExcelBedrockMenu.catalogEntryCount(g);i++){String d=ExcelBedrockMenu.catalogDisplay(g,i); if(d.equals("/cosmetics")||d.equals("/lostfound")||d.equals("/trash")||d.equals("/mark")||d.equals("/ask")) hits++;}
            check(hits==5,"each of the five commands appears exactly once in the catalogue");
            check(ExcelBedrockMenu.CATALOG_VIA_ACTION.equals(new java.util.HashSet<>(java.util.Arrays.asList("cosmetics","lostfound","trash","mark","ask"))),"exactly the five new rows route through runAction");
            check(s13.contains("if (CATALOG_VIA_ACTION.contains(run)) {\n            runAction(player, run);\n            return;\n        }"),"runCatalogEntry sends them to the guarded compass actions");
            check(s13.contains("case \"ask\":\n                // v1.5.22: ExcelGuide owns /ask. Bedrock gets a text box"),"/ask row opens the text box (never a bare /ask)");
            for(String r:wantR) check(!ExcelBedrockMenu.requiresConfirm(r,false)&&!ExcelBedrockMenu.requiresConfirm(r,true),"/"+r+" never opens the teleport confirm");
            boolean ascii13=true; for(int i=0;i<4;i++) if(!(ExcelBedrockMenu.catalogDisplay(7,i)+ExcelBedrockMenu.catalogDescription(7,i)).chars().allMatch(ch->ch<128)) ascii13=false;
            check(ascii13&&ExcelBedrockMenu.catalogGroupName(7).chars().allMatch(ch->ch<128),"new rows plain ASCII (no em dashes)");
            String panel=String.join("\n",ExcelBedrockMenu.commandsPanelLines(-1));
            check(panel.contains("Your Items & Help")&&panel.contains("/lostfound")&&panel.contains("/cosmetics"),"Java /servercommands chat panel lists the new rows too");
        }catch(Exception e){check(false,"1.5.23 catalogue checks "+e);}
        System.out.println(fails==0?"ALL PASS":"FAILED "+fails);
        if(fails>0)System.exit(1);
    }
}
