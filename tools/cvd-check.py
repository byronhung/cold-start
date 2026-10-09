"""First Light: check puzzle palettes under colour-vision deficiency (Machado et al. 2009, severity 1.0).

For each palette: simulate protanopia, deuteranopia and tritanopia, convert to CIELAB and report the
smallest CIEDE2000 distance between any two colours. Bigger is better; ~20+ reads as clearly different
at a glance on a small target, under ~10 is a coin flip.
"""
import itertools
import math

MACHADO = {
    "normal": [[1, 0, 0], [0, 1, 0], [0, 0, 1]],
    "protan": [[0.152286, 1.052583, -0.204868], [0.114503, 0.786281, 0.099216], [-0.003882, -0.048116, 1.051998]],
    "deutan": [[0.367322, 0.860646, -0.227968], [0.280085, 0.672501, 0.047413], [-0.011820, 0.042940, 0.968881]],
    "tritan": [[1.255528, -0.076749, -0.178779], [-0.078411, 0.930809, 0.147602], [0.004733, 0.691367, 0.303900]],
}


def to_lin(c):
    c /= 255
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def hexrgb(h):
    h = h.lstrip('#')
    return [int(h[i:i + 2], 16) for i in (0, 2, 4)]


def simulate(rgb, kind):
    lin = [to_lin(v) for v in rgb]
    m = MACHADO[kind]
    return [max(0.0, min(1.0, sum(m[r][k] * lin[k] for k in range(3)))) for r in range(3)]


def lab(lin):
    r, g, b = lin
    x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047
    y = (0.2126 * r + 0.7152 * g + 0.0722 * b)
    z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883
    f = lambda t: t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116
    return 116 * f(y) - 16, 500 * (f(x) - f(y)), 200 * (f(y) - f(z))


def de2000(l1, l2):
    L1, a1, b1 = l1
    L2, a2, b2 = l2
    C1, C2 = math.hypot(a1, b1), math.hypot(a2, b2)
    Cb = (C1 + C2) / 2
    G = 0.5 * (1 - math.sqrt(Cb ** 7 / (Cb ** 7 + 25 ** 7)))
    a1p, a2p = (1 + G) * a1, (1 + G) * a2
    C1p, C2p = math.hypot(a1p, b1), math.hypot(a2p, b2)
    h1p = math.degrees(math.atan2(b1, a1p)) % 360
    h2p = math.degrees(math.atan2(b2, a2p)) % 360
    dLp, dCp = L2 - L1, C2p - C1p
    dhp = h2p - h1p
    if C1p * C2p == 0:
        dhp = 0
    elif dhp > 180:
        dhp -= 360
    elif dhp < -180:
        dhp += 360
    dHp = 2 * math.sqrt(C1p * C2p) * math.sin(math.radians(dhp / 2))
    Lbp, Cbp = (L1 + L2) / 2, (C1p + C2p) / 2
    hbp = (h1p + h2p) / 2 if abs(h1p - h2p) <= 180 else (h1p + h2p + 360) / 2
    if C1p * C2p == 0:
        hbp = h1p + h2p
    T = 1 - 0.17 * math.cos(math.radians(hbp - 30)) + 0.24 * math.cos(math.radians(2 * hbp)) + 0.32 * math.cos(math.radians(3 * hbp + 6)) - 0.20 * math.cos(math.radians(4 * hbp - 63))
    dth = 30 * math.exp(-((hbp - 275) / 25) ** 2)
    Rc = 2 * math.sqrt(Cbp ** 7 / (Cbp ** 7 + 25 ** 7))
    Sl = 1 + 0.015 * (Lbp - 50) ** 2 / math.sqrt(20 + (Lbp - 50) ** 2)
    Sc, Sh = 1 + 0.045 * Cbp, 1 + 0.015 * Cbp * T
    Rt = -math.sin(math.radians(2 * dth)) * Rc
    return math.sqrt((dLp / Sl) ** 2 + (dCp / Sc) ** 2 + (dHp / Sh) ** 2 + Rt * (dCp / Sc) * (dHp / Sh))


def report(name, palette):
    print(f"\n{name}: " + ", ".join(f"{k} {v}" for k, v in palette.items()))
    worst_all = 999
    for kind in MACHADO:
        labs = {k: lab(simulate(hexrgb(v), kind)) for k, v in palette.items()}
        pairs = sorted((de2000(labs[a], labs[b]), a, b) for a, b in itertools.combinations(palette, 2))
        d, a, b = pairs[0]
        worst_all = min(worst_all, d)
        print(f"  {kind:7s} closest pair {a}/{b}: {d:5.1f}")
    print(f"  WORST CASE: {worst_all:.1f}")
    return worst_all


PALETTES = {
    "Stroop inks (InkColour)": {"RED": "#E8433A", "BLUE": "#4A6BFF", "PINK": "#FFA6E0", "YELLOW": "#FFF07A"},
    "Odd one out (ShapeColour)": {"RED": "#E8433A", "BLUE": "#4A6BFF"},
}

if __name__ == "__main__":
    # Run before changing any puzzle colour: every palette should stay at a worst case of ~20 or more.
    for n, p in PALETTES.items():
        report(n, p)
