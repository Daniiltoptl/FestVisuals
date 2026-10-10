#version 330

#moj_import <festvisuals:common.glsl>

uniform sampler2D Sampler0;

in vec2 FragCoord;
in vec2 TexCoord;

out vec4 fragColor;

vec4 blend(vec2 uv) {
    vec4 top = mix(uTopLeftColor, uTopRightColor, uv.x);
    vec4 bottom = mix(uBottomLeftColor, uBottomRightColor, uv.x);

    return mix(top, bottom, uv.y);
}

void main() {
    float alpha = ralpha(uSize, FragCoord, uRadius, uSmoothness);

    if (alpha <= 0.0) {
        discard;
    }

    // The blur texture contains a full-screen image. Its lookup must be based
    // on the physical fragment position, as in the 26.2 reference renderer;
    // local rectangle UVs are affected by GUI scale and caused tiled samples.
    vec2 screenUv = gl_FragCoord.xy / vec2(textureSize(Sampler0, 0));
    vec4 color = mix(texture(Sampler0, screenUv), blend(FragCoord), uMix);
    color.a *= alpha * uAlpha;

    fragColor = color;
}
