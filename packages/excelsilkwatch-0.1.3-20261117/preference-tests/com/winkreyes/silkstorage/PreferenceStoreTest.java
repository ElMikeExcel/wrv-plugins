// ExcelSilkWatch — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.silkstorage;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class PreferenceStoreTest {
    private static int passed;
    private static final UUID A = new UUID(0x1111111111111111L, 0x1111111111111111L);
    private static final UUID B = new UUID(0x2222222222222222L, 0x2222222222222222L);
    @FunctionalInterface interface Checked { void run() throws Exception; }
    private static void check(String name, Checked test) throws Exception { test.run(); passed++; System.out.println("PASS " + name); }
    private static void yes(boolean value) { if (!value) throw new AssertionError(); }
    private static void fails(Checked test) throws Exception { try { test.run(); } catch (IOException expected) {return;} throw new AssertionError("expected IOException"); }
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("silk-preferences-test-");
        check("missing file defaults ON without creating data", () -> { Path p=root.resolve("missing/players.yml"); PreferenceStore s=new PreferenceStore(p);yes(s.enabled(A));yes(!Files.exists(p)); });
        check("off persists across new store and reconnect UUID", () -> { Path p=root.resolve("roundtrip/players.yml");PreferenceStore s=new PreferenceStore(p);s.setEnabled(A,false);yes(!s.enabled(A));yes(!new PreferenceStore(p).enabled(UUID.fromString(A.toString())));s.setEnabled(A,true);yes(new PreferenceStore(p).enabled(A)); });
        check("independent players and default", () -> { Path p=root.resolve("multi.yml");PreferenceStore s=new PreferenceStore(p);s.setEnabled(A,false);s.setEnabled(B,true);PreferenceStore r=new PreferenceStore(p);yes(!r.enabled(A));yes(r.enabled(B));yes(r.enabled(UUID.randomUUID())); });
        check("explicit ON persists", () -> { Path p=root.resolve("on.yml");PreferenceStore s=new PreferenceStore(p);s.setEnabled(A,true);yes(Files.readString(p).contains(A+": true"));yes(new PreferenceStore(p).enabled(A)); });
        check("injected save failure preserves memory and disk", () -> { Path p=root.resolve("failure.yml");PreferenceStore original=new PreferenceStore(p);original.setEnabled(A,false);String before=Files.readString(p);PreferenceStore s=new PreferenceStore(p,(file,text)->{throw new IOException("simulated precommit failure");});fails(()->s.setEnabled(A,true));yes(!s.enabled(A));yes(before.equals(Files.readString(p)));yes(!new PreferenceStore(p).enabled(A)); });
        check("atomic move failure preserves target and memory", () -> { Path p=root.resolve("target-dir");PreferenceStore s=new PreferenceStore(p);Files.createDirectory(p);Files.writeString(p.resolve("keep"),"original");fails(()->s.setEnabled(A,false));yes(s.enabled(A));yes(Files.readString(p.resolve("keep")).equals("original")); });
        check("parent write failure preserves default", () -> {Path parent=root.resolve("not-dir");Files.writeString(parent,"keep");PreferenceStore s=new PreferenceStore(root.resolve("later/players.yml"));Path target=root.resolve("later");Files.writeString(target,"keep");fails(()->s.setEnabled(A,false));yes(s.enabled(A));yes(Files.readString(target).equals("keep"));});
        String valid="format-version: 1\nplayers:\n  "+A+": false\n";
        String[] bad={"", "[not: yaml", "format-version: 2\nplayers: {}\n", "format-version: 1\nplayers: []\n", "format-version: 1\nplayers:\n  not-a-uuid: true\n", "format-version: 1\nplayers:\n  "+A+": 'false'\n",valid+"unexpected: true\n", "players: {}\n", "format-version: '1'\nplayers: {}\n", "format-version: 1\nplayers:\n  1-1-1-1-1: true\n", "format-version: 1\nplayers:\n  "+A+": false\n  "+A+": true\n"};
        for(int i=0;i<bad.length;i++){final int n=i;check("invalid schema/YAML "+i+" rejected untouched",()->{Path p=root.resolve("bad-"+n+".yml");Files.writeString(p,bad[n]);fails(()->new PreferenceStore(p));yes(Files.readString(p).equals(bad[n]));});}
        check("empty valid mapping accepted",()->{Path p=root.resolve("empty.yml");Files.writeString(p,"format-version: 1\nplayers: {}\n");yes(new PreferenceStore(p).enabled(A));});
        check("symlink refused untouched",()->{Path real=root.resolve("real.yml");Files.writeString(real,valid);Path link=root.resolve("link.yml");Files.createSymbolicLink(link,real);fails(()->new PreferenceStore(link));yes(Files.readString(real).equals(valid));});
        check("concurrent reads and updates remain coherent",()->{Path p=root.resolve("concurrent.yml");PreferenceStore s=new PreferenceStore(p);Thread reader=new Thread(()->{for(int i=0;i<10000;i++)s.enabled(A);});reader.start();for(int i=0;i<10;i++)s.setEnabled(A,i%2==0);reader.join();yes(!s.enabled(A));yes(!new PreferenceStore(p).enabled(A));});
        check("failed save cleans temp file",()->{try(var paths=Files.list(root)){yes(paths.noneMatch(p->p.getFileName().toString().startsWith(".players-")));}});
        System.out.println("PASS all " + passed + " preference cases");
    }
}
