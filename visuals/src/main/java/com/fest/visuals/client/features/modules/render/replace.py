import re

with open('AmbienceModule.java', 'r', encoding='utf-8') as f:
    text = f.read()

# Replace time setting
text = re.sub(r'private final ModeSetting time = new ModeSetting\("Time"\)\.value\(WorldTime\.DAY\)\.values\(WorldTime\.values\(\)\);', 
              r'private final SliderSetting time = new SliderSetting("Time").value(-1f).range(-1f, 24000f).step(100f);', text)

# Remove WorldTime enum
text = re.sub(r'@AllArgsConstructor\s+private enum WorldTime[\s\S]*?\}', '', text)

# Replace getTime body
new_get_time = '''public long getTime(long original) {
        if (mc.level == null || !isEnabled()) return original;
        float val = time.getValue();
        if (val < 0) return original;
        return (long) val;
    }'''

text = re.sub(r'public long getTime\(long original\) \{[\s\S]*?\};?\s+\}', new_get_time, text)

with open('AmbienceModule.java', 'w', encoding='utf-8') as f:
    f.write(text)