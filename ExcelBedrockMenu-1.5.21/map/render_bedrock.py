# Renders the Bedrock compass form (ExcelBedrockMenu BUTTONS/COMMANDS arrays) as a PNG list, straight from the source.
import sys, re
sys.path.insert(0,'/workspace/compass-map/src')
from common import font, wrap, text_block, BG, FG, DIM
from PIL import Image, ImageDraw
CATCOL={'travel':(40,110,200),'me':(140,70,200),'shop':(30,150,80),'help':(200,140,20),'settings':(90,100,115)}
# v1.5.21 mockup: Bedrock form look. Light gray button, default dark gray text; colored text = the section code each group sends.
BTN=(198,198,198); BTN_EDGE=(85,85,85); DEFAULT_TXT=(76,76,76); NOTE_TXT=(105,105,105)
TXTCOL={'travel':((0,0,170),'\u00a71 dark blue',7.8),'me':((170,0,170),'\u00a75 dark purple',3.7),'shop':((0,170,0),'\u00a72 dark green',1.8),
 'help':((180,104,77),'\u00a7n copper (Bedrock)',2.4),'settings':((85,85,85),'\u00a78 dark gray',4.4)}
CAT={'spawn':'travel','tpmenu':'travel','excelparty':'travel','ag':'travel',
 'pouch':'me','spellinfo':'me','jobs browse':'me','mcstats':'me','skillsperks':'me','leaderboard':'me','lostfound':'me','trash':'me',
 'shop':'shop','motd':'help','servercommands':'help','serverwebsite':'help','whatsnew':'help',
 'wandstyle':'settings','autosort':'settings','menujoin':'settings'}
NOTE={'spawn':'Yes/Cancel confirm','ag':'Yes/Cancel confirm (label shows lock state)','skillsperks':'opens Skills & Perks form',
 'tpmenu':'player picker form','servercommands':'opens command groups form','serverwebsite':'address form + chat copy',
 'wandstyle':'Classic / Sneak bars form','autosort':'toggle','menujoin':'toggle','lostfound':'shows the price before you pay',
 'trash':'throw away, recover within 5 min'}
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
S='/workspace/batch-next/bedrock-menu-lostfound-trash-20261025'
render(S+'/base/ebm-1.5.19-src/src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java',S+'/map/bedrock-before.png',
 'BEFORE: Bedrock compass (live 1.5.19-whatsnew)',
 'Bedrock shows one scrolling list of full-width light gray buttons (a Geyser form), top to bottom. All labels use the default dark text. Small gray text after a label (/command, notes) is a map note, not on the real button. No icons.',
 ['18 buttons. No Lost & Found and no Trash Bin button. No group colors.','Java (staged cleanup) has both in its Me row: Lost & Found slot 14, Trash Bin slot 16.'])
render(S+'/src/ExcelBedrockMenu/src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java',S+'/map/bedrock-after.png',
 'AFTER: Bedrock compass 1.5.21 (staged, not live)',
 'Same gray buttons. Label TEXT is now colored by group, like the Java map rows. Lost & Found and Trash Bin are added (yellow outline) right after Leaderboard. Small gray text after a label is a map note only.',
 ['New: Lost & Found (12th button, runs /lostfound, ExcelDeathRecord 0.3.0). New: Trash Bin (13th, runs /trash, ExcelTrash 0.2.0).',
  'Label words are unchanged; only the color code in front is new.',
  'Shops green and Help copper are the weakest on light gray (no darker green or orange code exists on Bedrock). Check on a real device.',
  'Colors drawn as RGB of the codes; a phone or console may render them a little differently.'],
 new=('lostfound','trash'),colored=True)
