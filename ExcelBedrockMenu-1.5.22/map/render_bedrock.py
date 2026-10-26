# Renders the Bedrock compass form (ExcelBedrockMenu BUTTONS/COMMANDS arrays) as a PNG list, straight from the source.
import sys, re
sys.path.insert(0,'/workspace/compass-map/src')
from common import font, wrap, text_block, BG, FG, DIM
from PIL import Image, ImageDraw
CATCOL={'travel':(40,110,200),'me':(140,70,200),'shop':(30,150,80),'help':(200,140,20),'settings':(90,100,115)}
# v1.5.22 mockup (copied from the 1.5.21 packet). v1.5.21 mockup: Bedrock form look. Light gray button, default dark gray text; colored text = the section code each group sends.
BTN=(198,198,198); BTN_EDGE=(85,85,85); DEFAULT_TXT=(76,76,76); NOTE_TXT=(105,105,105)
TXTCOL={'travel':((0,0,170),'\u00a71 dark blue',7.8),'me':((170,0,170),'\u00a75 dark purple',3.7),'shop':((0,170,0),'\u00a72 dark green',1.8),
 'help':((180,104,77),'\u00a7n copper (Bedrock)',2.4),'settings':((85,85,85),'\u00a78 dark gray',4.4)}
CAT={'spawn':'travel','tpmenu':'travel','excelparty':'travel','ag':'travel',
 'pouch':'me','spellinfo':'me','jobs browse':'me','mcstats':'me','skillsperks':'me','leaderboard':'me','lostfound':'me','trash':'me',
 'shop':'shop','cosmetics':'shop','ask':'help','mark':'settings','motd':'help','servercommands':'help','serverwebsite':'help','whatsnew':'help',
 'wandstyle':'settings','autosort':'settings','menujoin':'settings'}
NOTE={'spawn':'Yes/Cancel confirm','ag':'Yes/Cancel confirm (label shows lock state)','skillsperks':'opens Skills & Perks form',
 'tpmenu':'player picker form','servercommands':'opens command groups form','serverwebsite':'address form + chat copy',
 'wandstyle':'Classic / Sneak bars form','autosort':'toggle','menujoin':'toggle','lostfound':'shows the price before you pay',
 'trash':'throw away, recover within 5 min',
 'cosmetics':'ExcelCosmetics form','ask':'text box form, Send runs /ask','mark':"ExcelMakersMark two-switch form"}
def arr(src,name):
    s=src.index(name+' = {'); e=src.index('};',s); return re.findall(r'"([^"]*)"',src[s:e])
def render(srcpath,out,title,subtitle,notes,new=(),colored=False):
    src=open(srcpath).read(); B=arr(src,'BUTTONS'); C=arr(src,'COMMANDS'); assert len(B)==len(C)
    W=1000;M=24;H=50;G=6
    im=Image.new('RGB',(W,700+len(B)*(H+G)+len(notes)*70),BG); d=ImageDraw.Draw(im); y=20
    d.text((M,y),title,font=font(31,True),fill=FG); y+=46
    for l in wrap(d,subtitle,font(20),W-2*M): d.text((M,y),l,font=font(20),fill=DIM); y+=28
    y+=8
    # form frame (what a phone shows: title, content line, then one full-width button per row, scroll top to bottom)
    fx0,fx1=M,W-M
    d.rounded_rectangle((fx0,y,fx1,y+70+len(B)*(H+G)+10),14,fill=(32,35,44),outline=(90,95,110),width=2)
    d.text((W/2-d.textlength('Server Menu',font=font(24,True))/2,y+10),'Server Menu',font=font(24,True),fill=FG)
    t='What would you like to do?'; d.text((W/2-d.textlength(t,font=font(17))/2,y+42),t,font=font(17),fill=DIM)
    y+=72
    for i,(b,c) in enumerate(zip(B,C)):
        x0,x1=fx0+16,fx1-16; col=CATCOL[CAT.get(c,'help')]
        isnew=c in new
        d.rounded_rectangle((x0,y,x1,y+H),4,fill=BTN,outline=(250,205,40) if isnew else BTN_EDGE,width=4 if isnew else 2)
        tc=TXTCOL[CAT.get(c,'help')][0] if colored else DEFAULT_TXT
        d.text((x0+10,y+15),str(i+1),font=font(15),fill=NOTE_TXT)
        d.text((x0+48,y+11),b,font=font(22,True),fill=tc)
        sub='/'+c+('   '+NOTE[c] if c in NOTE else '')
        d.text((x0+48+d.textlength(b,font=font(22,True))+16,y+17),sub,font=font(14),fill=NOTE_TXT)
        if isnew:
            lab='NEW ON BEDROCK'; bw=d.textlength(lab,font=font(14,True))+12
            d.rounded_rectangle((x1-bw-10,y+13,x1-10,y+H-13),5,fill=(240,190,30))
            d.text((x1-bw-4,y+16),lab,font=font(14,True),fill=(20,20,20))
        y+=H+G
    y+=24
    # legend
    x=M
    if colored:
        d.text((M,y),'Text colors (contrast on the gray button, 4.5+ is easy, under 3 is hard):',font=font(19),fill=FG); y+=32
        for k,n in [('travel','Travel'),('me','Me'),('shop','Shops'),('help','Help'),('settings','Settings')]:
            rgb,code,cr=TXTCOL[k]
            d.rounded_rectangle((M,y,M+330,y+34),4,fill=BTN,outline=BTN_EDGE,width=2); d.text((M+12,y+5),n,font=font(20,True),fill=rgb)
            d.text((M+350,y+6),code+'   contrast '+str(cr)+':1',font=font(18),fill=FG); y+=40
        y-=40
    else:
        d.rounded_rectangle((M,y,M+330,y+34),4,fill=BTN,outline=BTN_EDGE,width=2); d.text((M+12,y+5),'Every button',font=font(20,True),fill=DEFAULT_TXT)
        d.text((M+350,y+6),'default dark gray text, no group colors   contrast 5.0:1',font=font(18),fill=FG)
    y+=44
    for n in notes:
        for l in wrap(d,'- '+n,font(19),W-2*M): d.text((M,y),l,font=font(19),fill=FG); y+=28
    im.crop((0,0,W,y+16)).save(out); print(out,len(B),'buttons')
P='/workspace/batch-next/bedrock-menu-parity-1522-20261026'
render('/workspace/batch-next/bedrock-menu-parity-1522-20261026/base/ebm-1.5.21-src/src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java',P+'/map/bedrock-before.png',
 'BEFORE: Bedrock compass (live 1.5.21)',
 'Bedrock shows one scrolling list of full-width light gray buttons (a Geyser form), top to bottom. Label text is colored by group. Small gray text after a label (/command, notes) is a map note, not on the real button. No icons.',
 ['20 buttons. Missing next to Java: My Cosmetics, Ask the Guide, Maker\'s mark and Gear stats.',
  'Has a separate mcMMO Stats button that the Java compass dropped on Oct 25.',
  'Skills & Perks form still says "Magic is sealed for now" although Seal 1 broke Oct 23.'],colored=True)
render('/workspace/batch-next/bedrock-menu-parity-1522-20261026/src/ExcelBedrockMenu/src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java',P+'/map/bedrock-after.png',
 'AFTER: Bedrock compass 1.5.22 (staged, not live)',
 'Same gray buttons and group colors. Three Java buttons added (yellow outline), each next to its Java row-mates. mcMMO Stats moved inside Skills & Perks, like Java. Small gray text after a label is a map note only.',
 ['New: My Cosmetics (7th, Shops green, runs /cosmetics like Java).',
  'New: Ask the Guide (18th, Help copper): a text box; Send runs /ask <your question>, the command Java players type.',
  'New: Maker\'s mark and Gear stats (21st, Settings gray): runs /mark, which opens ExcelMakersMark\'s own two-switch form (the same two Java switches).',
  'Removed as its own button: mcMMO Stats. Skills & Perks now has a "My mcMMO stats" button (same /mcstats as Java\'s Skills & Perks menu).',
  'Skills & Perks Magic text now says Magic is awake, the live seal count, and the Old Quartermaster first-wand line.',
  'Colors drawn as RGB of the codes; a phone or console may render them a little differently.'],
 new=('cosmetics','ask','mark'),colored=True)
