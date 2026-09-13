"""Generate four short original PCM cues used by the Android MVP."""
from pathlib import Path
import math
import struct
import wave

OUT = Path(__file__).parents[1] / "app/src/main/res/raw"
OUT.mkdir(parents=True, exist_ok=True)

def render(name: str, notes: list[tuple[float, float]], volume: float = .22) -> None:
    rate = 22050
    samples: list[int] = []
    for frequency, seconds in notes:
        count = int(rate * seconds)
        for index in range(count):
            envelope = min(1.0, index / (rate * .015)) * max(0.0, 1 - index / count)
            value = math.sin(2 * math.pi * frequency * index / rate) * envelope * volume
            samples.append(int(value * 32767))
    with wave.open(str(OUT / f"{name}.wav"), "wb") as stream:
        stream.setparams((1, 2, rate, len(samples), "NONE", "not compressed"))
        stream.writeframes(b"".join(struct.pack("<h", value) for value in samples))

render("cue_tap", [(520, .07)])
render("cue_swap", [(390, .06), (540, .07)])
render("cue_discovery", [(520, .07), (660, .07), (780, .10)])
render("cue_celebrate", [(523, .08), (659, .08), (784, .08), (1047, .15)])
