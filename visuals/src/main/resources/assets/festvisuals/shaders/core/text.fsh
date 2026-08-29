#version 330

#moj_import <festvisuals:common.glsl>

uniform sampler2D Sampler0;

in vec2 TexCoord;
in vec4 FragColor;

out vec4 fragColor;

float median(vec3 color) {
    return max(min(color.r, color.g), min(max(color.r, color.g), color.b));
}

void main() {
    float dist = median(texture(Sampler0, TexCoord).rgb) - 0.5 + uThickness;
    vec2 h = vec2(dFdx(TexCoord.x), dFdy(TexCoord.y)) * textureSize(Sampler0, 0);
    float pixels = uRange * inversesqrt(h.x * h.x + h.y * h.y);
    float alpha = smoothstep(-uSmoothness, uSmoothness, dist * pixels);
    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    if (uOutline > 0.5) {
        color = mix(uOutlineColor, FragColor, alpha);
        color.a *= smoothstep(-uSmoothness, uSmoothness, (dist + uOutlineThickness) * pixels);
    }

    fragColor = color;
}
