# Genera las 4 texturas (Normal, Feliz, Sorprendido, Dormido) del Flowy. Requiere Pillow.
from PIL import Image

YEL_L=(255,224,74,255); YEL=(255,194,31,255); YEL_D=(232,160,20,255)
ORA=(242,140,40,255); BRN=(154,91,50,255); EYE=(43,26,18,255); PINK=(245,154,160,255)
GRN=(63,168,53,255); GRN_D=(46,139,46,255); OLIVE=(150,176,50,255)

def box_regions(u,v,w,h,d):
    return {"top":(u+d,v,w,d),"bottom":(u+d+w,v,w,d),"left":(u,v+d,d,h),
            "front":(u+d,v+d,w,h),"right":(u+d+w,v+d,d,h),"back":(u+2*d+w,v+d,w,h)}

def fill(img,rect,fn):
    x0,y0,w,h=rect
    for y in range(h):
        for x in range(w):
            img.putpixel((x0+x,y0+y),fn(x,y))

def yellow(x,y): return YEL_L if y==0 else (YEL_D if (x+y)%5==0 else YEL)
def green(x,y): return GRN_D if (x+y)%4==0 else GRN
def leaf(x,y): return GRN_D if y==1 else GRN

FACES = {
 "normal":   ["OOOOOO","OOOOOO","OBOOBO","POOOOP","OBOOBO","OOBBOO"],
 "happy":    ["OOOOOO","OBOOBO","BOBBOB","POOOOP","OBBBBO","OOBBOO"],
 "surprised":["OOOOOO","OOOOOO","OBOOBO","POOOOP","OOBBOO","OOBBOO"],
 "sleep":    ["OOOOOO","OOOOOO","OOOOOO","BBOOBB","POOOOP","OOBBOO"],
}
CM={"O":ORA,"B":EYE,"P":PINK}

def build(name,face):
    img=Image.new("RGBA",(64,32),(0,0,0,0))
    # cara 6x6x2 en (0,0)
    r=box_regions(0,0,6,6,2)
    for k,rect in r.items():
        if k=="front":
            fill(img,rect,lambda x,y:CM[face[y][x]])
        elif k=="back":
            fill(img,rect,lambda x,y:OLIVE)
        else:
            fill(img,rect,lambda x,y:BRN if k in("left","right") else ORA)
    for (u,v,w,h,d,fn) in [(16,0,6,2,2,yellow),(32,0,6,2,2,yellow),(0,8,2,6,2,yellow),(8,8,2,6,2,yellow),
                           (16,8,2,2,2,yellow),(16,16,2,5,2,green),(32,16,4,2,2,leaf)]:
        for rect in box_regions(u,v,w,h,d).values(): fill(img,rect,fn)
    img.save(f"src/main/resources/assets/flowys/textures/entity/flowy/flowy_{name}.png")

for n,f in FACES.items(): build(n,f)
print("texturas ok")
