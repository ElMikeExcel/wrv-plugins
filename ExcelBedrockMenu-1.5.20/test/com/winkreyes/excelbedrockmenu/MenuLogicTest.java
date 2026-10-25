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
            check(src.contains("\"Party\", \"Utility Pouch\", \"Spellbook\", \"Excel Shop\", \"Adventurers Guild\""),"button array runs Party, then Utility Pouch (1.5.14), Spellbook (1.5.18), before Excel Shop");
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
            check(yml2.contains("version: 1.5.20"),"plugin.yml is 1.5.20");
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
            check(buttons.length==20,"menu now has 20 buttons (1.5.11's 13 + Skills & Perks + Spellbook, Wand controls and Auto-sort in 1.5.18 + What's New in 1.5.19 + Lost & Found and Trash Bin in 1.5.20)");
            check(commands.length==20,"command array has 20 entries, matching the buttons");
            String[] wantButtons={"Spawn","Teleport to a player","Party","Utility Pouch","Spellbook","Excel Shop","Adventurers Guild","Jobs & Money","mcMMO Stats","Skills & Perks","Leaderboard","Lost & Found","Trash Bin","Server Info","Server Commands","Server Website","What's New","Wand controls: Classic or Sneak bars","Auto-sort my chests: on/off","Auto-open on join"};
            check(java.util.Arrays.equals(buttons,wantButtons),"full button set is the 1.5.11 set + Skills & Perks, in the 1.5.14 order (Pouch moved up), Spellbook after the Pouch (1.5.18), Auto-sort before Auto-open (1.5.18)");
            String[] wantCommands={"spawn","tpmenu","excelparty","pouch","spellinfo","shop","ag","jobs browse","mcstats","skillsperks","leaderboard","lostfound","trash","motd","servercommands","serverwebsite","whatsnew","wandstyle","autosort","menujoin"};
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
            check(buttons4[8].equals("mcMMO Stats")&&buttons4[10].equals("Leaderboard"),"Skills & Perks is immediately after mcMMO Stats, before Leaderboard (spec placement, indices shifted one by 1.5.14)");
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
            check(c3.contains("(3 of 5 today)"),"seal count renders from GuildGate state (nonzero case)");
            check(!c3.contains("(0 of 5 today)"),"nonzero render does not keep the 0 count");
            check(ExcelBedrockMenu.skillsPerksContent(5).contains("(5 of 5 today)"),"seal count renders at all 5 fallen");
            check(src4.contains("sealsBrokenCount"),"the count is read live from GuildGate (sealsBrokenCount)");
            // The form's buttons: Skill Trees follow-on first, Close second
            check(java.util.Arrays.equals(ExcelBedrockMenu.SKILLS_PERKS_BUTTONS,new String[]{"Skill Trees on the Website","Close"}),"Skills & Perks form buttons are Skill Trees on the Website + Close");
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
            check(ExcelBedrockMenu.catalogTotalEntries()==45,"catalogue totals 45 entries (41 + pouch in 1.5.13 + two childproof rows in 1.5.16 + /whatsnew in 1.5.19)");
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
            check(buttons6.length==20&&commands6.length==20,"1.5.14 move kept 14 buttons / 14 commands; 1.5.18 adds Spellbook, Wand controls and Auto-sort (17); What's New (18); 1.5.20 Lost & Found and Trash Bin (20)");
            check(idxOf(buttons6,"Utility Pouch")==3,"Utility Pouch now sits at index 3 (4th, top daily-driver cluster)");
            check(idxOf(commands6,"pouch")==3,"pouch command at index 3, matching the button");
            check(buttons6[0].equals("Spawn")&&buttons6[1].equals("Teleport to a player")&&buttons6[2].equals("Party"),"top of the menu above the Pouch is unchanged: Spawn / Teleport to a player / Party");
            String[] wantRestB={"Spawn","Teleport to a player","Party","Excel Shop","Adventurers Guild","Jobs & Money","mcMMO Stats","Skills & Perks","Leaderboard","Server Info","Server Commands","Server Website","Auto-open on join"};
            String[] wantRestC={"spawn","tpmenu","excelparty","shop","ag","jobs browse","mcstats","skillsperks","leaderboard","motd","servercommands","serverwebsite","menujoin"};
            java.util.List<String> restB=new java.util.ArrayList<>(), restC=new java.util.ArrayList<>();
            for(String s:buttons6) if(!s.equals("Utility Pouch")&&!s.equals("Spellbook")&&!s.startsWith("Auto-sort")&&!s.startsWith("Wand controls")&&!s.equals("What's New")&&!s.equals("Lost & Found")&&!s.equals("Trash Bin")) restB.add(s);
            for(String s:commands6) if(!s.equals("pouch")&&!s.equals("spellinfo")&&!s.equals("autosort")&&!s.equals("wandstyle")&&!s.equals("whatsnew")&&!s.equals("lostfound")&&!s.equals("trash")) restC.add(s);
            check(restB.size()==13&&java.util.Arrays.equals(restB.toArray(new String[0]),wantRestB),"removing the Pouch leaves the 1.5.13 button order of the other 13 untouched");
            check(restC.size()==13&&java.util.Arrays.equals(restC.toArray(new String[0]),wantRestC),"removing pouch leaves the 1.5.13 command order of the other 13 untouched");
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
            check(gf.split("ClickStats.tap",-1).length-1==2,"exactly two hook call sites");
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
                check(py.contains("version: 1.5.20")&&py.contains("  childproofmenu:"),"plugin.yml 1.5.18 registers /childproofmenu");
            }catch(Exception e){check(false,"plugin.yml read "+e);}
        }
        // v1.5.18: Spellbook button (Mike, Oct 24, 2026). Runs ExcelWandBars' /spellinfo.
        try{
            String s8=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            String[] b8=parseStringArray(s8,"BUTTONS"); String[] c8=parseStringArray(s8,"COMMANDS");
            check(idxOf(b8,"Spellbook")==4&&idxOf(c8,"spellinfo")==4,"Spellbook button and spellinfo command both at index 4");
            check(b8[3].equals("Utility Pouch")&&b8[5].equals("Excel Shop"),"Spellbook sits right after Utility Pouch, before Excel Shop");
            java.util.List<String> rb=new java.util.ArrayList<>(java.util.Arrays.asList(b8)); rb.remove("Spellbook"); rb.remove("Auto-sort my chests: on/off"); rb.remove("Wand controls: Classic or Sneak bars"); rb.remove("What's New"); rb.remove("Lost & Found"); rb.remove("Trash Bin");
            check(java.util.Arrays.equals(rb.toArray(new String[0]),new String[]{"Spawn","Teleport to a player","Party","Utility Pouch","Excel Shop","Adventurers Guild","Jobs & Money","mcMMO Stats","Skills & Perks","Leaderboard","Server Info","Server Commands","Server Website","Auto-open on join"}),"removing Spellbook and Auto-sort leaves the exact 1.5.17 button order");
            java.util.List<String> rc=new java.util.ArrayList<>(java.util.Arrays.asList(c8)); rc.remove("spellinfo"); rc.remove("autosort"); rc.remove("wandstyle"); rc.remove("whatsnew"); rc.remove("lostfound"); rc.remove("trash");
            check(java.util.Arrays.equals(rc.toArray(new String[0]),new String[]{"spawn","tpmenu","excelparty","pouch","shop","ag","jobs browse","mcstats","skillsperks","leaderboard","motd","servercommands","serverwebsite","menujoin"}),"removing spellinfo and autosort leaves the exact 1.5.17 command order");
            check(!ExcelBedrockMenu.requiresConfirm("spellinfo",false)&&!ExcelBedrockMenu.requiresConfirm("spellinfo",true),"Spellbook never opens the teleport confirm");
            check(s8.contains("case \"spellinfo\":")&&s8.contains("Bukkit.getPluginCommand(\"spellinfo\") == null"),"spellinfo is guarded: missing ExcelWandBars gives a message, not an unknown command");
            check("Spellbook".chars().allMatch(c->c<128),"Spellbook label is plain ASCII");
            int as=idxOf(b8,"Auto-sort my chests: on/off");
            check(as>=0&&as==idxOf(c8,"autosort"),"Auto-sort button and autosort command line up");
            check(as==b8.length-2&&b8[b8.length-1].equals("Auto-open on join"),"Auto-sort sits just before Auto-open on join (the switches together)");
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
            check(b9[w+1].startsWith("Wand controls"),"Wand controls follows What's New (switches block unchanged)");
            check(s9.contains("case \"whatsnew\":")&&s9.contains("Bukkit.getPluginCommand(\"whatsnew\") == null"),"whatsnew guarded: missing ExcelWhatsNew gives a message");
            check(!ExcelBedrockMenu.requiresConfirm("whatsnew",false)&&!ExcelBedrockMenu.requiresConfirm("whatsnew",true),"What's New never opens the teleport confirm");
            check(!java.util.Arrays.asList(c9).contains("news"),"menu runs /whatsnew, not the /news alias");
            int g=ExcelBedrockMenu.catalogGroupIndexFor("server info"); int found=-1;
            for(int i=0;i<ExcelBedrockMenu.catalogEntryCount(g);i++) if("whatsnew".equals(ExcelBedrockMenu.catalogRun(g,i))) found=i;
            check(found==ExcelBedrockMenu.catalogEntryCount(g)-1,"/whatsnew is the last runnable row of Server Info");
            check(b9.length==20&&c9.length==20,"20 buttons, 20 commands (1.5.20 adds two)");
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
            java.util.List<String> rb=new java.util.ArrayList<>(java.util.Arrays.asList(b10)); rb.remove("Lost & Found"); rb.remove("Trash Bin");
            check(java.util.Arrays.equals(rb.toArray(new String[0]),new String[]{"Spawn","Teleport to a player","Party","Utility Pouch","Spellbook","Excel Shop","Adventurers Guild","Jobs & Money","mcMMO Stats","Skills & Perks","Leaderboard","Server Info","Server Commands","Server Website","What's New","Wand controls: Classic or Sneak bars","Auto-sort my chests: on/off","Auto-open on join"}),"removing the two new buttons leaves the exact live 1.5.19-whatsnew button order");
            java.util.List<String> rc=new java.util.ArrayList<>(java.util.Arrays.asList(c10)); rc.remove("lostfound"); rc.remove("trash");
            check(java.util.Arrays.equals(rc.toArray(new String[0]),new String[]{"spawn","tpmenu","excelparty","pouch","spellinfo","shop","ag","jobs browse","mcstats","skillsperks","leaderboard","motd","servercommands","serverwebsite","whatsnew","wandstyle","autosort","menujoin"}),"removing the two new commands leaves the exact live 1.5.19-whatsnew command order");
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
            check(ExcelBedrockMenu.catalogEntryCount(g)==10&&ExcelBedrockMenu.catalogTotalEntries()==45,"Server Commands catalogue unchanged (45 entries)");
        }catch(Exception e){check(false,"1.5.20 lost & found / trash checks "+e);}
        System.out.println(fails==0?"ALL PASS":"FAILED "+fails);
        if(fails>0)System.exit(1);
    }
}
