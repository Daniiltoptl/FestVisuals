package com.fest.visuals.api.utils.render.fonts;

import com.fest.visuals.api.system.backend.ClientInfo;
import com.fest.visuals.api.system.files.FileUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public class FontBuilder {
    private String name;
    private Identifier dataIdentifier;
    private Identifier atlasIdentifier;

    public FontBuilder() {}

    public FontBuilder find(String fontName) {
        this.name = fontName;
        this.dataIdentifier = Identifier.fromNamespaceAndPath(ClientInfo.NAME.toLowerCase(), "fonts/" + fontName + ".json");
        this.atlasIdentifier = Identifier.fromNamespaceAndPath(ClientInfo.NAME.toLowerCase(), "fonts/" + fontName + ".png");
        return this;
    }

    public Font load() {
        FontData data = FileUtil.fromJsonToInstance(this.dataIdentifier, FontData.class);
        if (data == null) {
            throw new RuntimeException("Failed to read font data file: " + this.dataIdentifier.toString() + "; Are you sure this is json file? Try to check the correctness of its syntax.");
        }

        float aWidth = data.atlas().width();
        float aHeight = data.atlas().height();
        Map<Integer, MsdfGlyph> glyphs = data.glyphs().stream().collect(Collectors.<FontData.GlyphData, Integer, MsdfGlyph>toMap(FontData.GlyphData::unicode, (glyphData) -> new MsdfGlyph(glyphData, aWidth, aHeight)));

        Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
        data.kernings().forEach((kerning) -> {
            Map<Integer, Float> map = kernings.get(kerning.leftChar());
            if (map == null) {
                map = new HashMap<>();
                kernings.put(kerning.leftChar(), map);
            }

            map.put(kerning.rightChar(), kerning.advance());
        });

        return new Font(name, this.atlasIdentifier, data.atlas(), data.metrics(), glyphs, kernings);
    }
}
